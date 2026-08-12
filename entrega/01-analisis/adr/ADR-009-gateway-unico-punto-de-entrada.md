# ADR-009 · Un único punto de entrada; ningún microservicio publica puerto

- **Estado:** Aceptado
- **Fecha:** 2026-08-03
- **Decide:** Josué Herrera, Fernando Socasi
- **Requerimientos afectados:** RNF-03, RNF-06, RNF-10
- **Historia:** configuración y documentación

## Contexto

Con dos microservicios, dos bases de datos y un explorador de base de datos, la forma perezosa de
levantar el sistema es publicar el puerto de cada contenedor en el host: `8686` para `users`,
`8787` para `matchpoint`, `5432` y `5433` para las bases, `5050` para pgAdmin.

Funciona, y tiene tres problemas:

1. **Superficie de ataque.** Cinco puertos abiertos donde basta uno. Las bases de datos accesibles
   desde el host son el caso más grave.
2. **El cliente tiene que saber la topología.** Postman tendría que apuntar a un puerto distinto
   por servicio, y cualquier cambio en el reparto interno rompe al cliente.
3. **No se parece a producción.** En cualquier despliegue real hay un balanceador o un ingress
   delante; desarrollar sin él esconde justamente los problemas que aparecen al desplegar
   (cabeceras, rutas, prefijos, redirecciones).

## Decisión

**nginx es el único servicio con puerto publicado al host** (`9090` por defecto, configurable con
`GATEWAY_PORT`). Todos los demás usan `expose:` y solo son alcanzables dentro de la red interna de
Docker Compose.

| Ruta pública | Destino interno |
|---|---|
| `http://localhost:9090/users` | `users:8686` |
| `http://localhost:9090/matchpoint` | `matchpoint:8787` |
| `http://localhost:9090/pgadmin/` | `pgadmin:80` |
| `http://localhost:9090/health` | respuesta del propio nginx |

**Incluido pgAdmin.** Es la parte que suele quedarse fuera: el explorador de base de datos también
entra por el gateway, así que ni siquiera la herramienta de inspección abre un puerto propio.

## Alternativas consideradas

| Alternativa | Por qué se descartó |
|---|---|
| **Publicar el puerto de cada servicio** | Los tres problemas descritos arriba. En particular, dejar PostgreSQL accesible desde el host en un proyecto que se comparte por git es una mala costumbre que conviene no adquirir. |
| **Spring Cloud Gateway** | Es un gateway excelente y con integración natural en el ecosistema Spring. Se descartó porque añade una tercera aplicación JVM (medio gigabyte de memoria, 30 segundos de arranque) para hacer lo que nginx resuelve con 40 líneas de configuración y 10 MB de imagen. La lógica de enrutamiento aquí es puro *reverse proxy*: no hay filtros, ni *rate limiting*, ni composición de respuestas. |
| **Traefik** con descubrimiento automático por etiquetas | Elegante y menos configuración explícita. Se prefirió nginx porque la configuración explícita **se puede leer y explicar en la sustentación**, y porque es el mismo componente que se usaría delante de un despliegue en EC2. |
| **Sin gateway, cliente contra cada servicio** | Traslada el problema al cliente y hace imposible el argumento de "un solo punto de entrada". |

## Consecuencias

**A favor**

- **Superficie mínima:** un puerto abierto en lugar de cinco. Las bases de datos son inalcanzables
  desde fuera de la red de Compose.
- **El cliente no conoce la topología.** La colección de Postman tiene una sola variable
  `{{baseUrl}}`. Si mañana `matchpoint` se parte en dos servicios, el cliente no se entera.
- **Se parece a producción.** El mismo `nginx.conf` sirve, casi sin cambios, delante de un
  despliegue en EC2 o como configuración de un ALB.
- **Un solo lugar para el log de acceso.** `access_log /dev/stdout` con un formato alineado al
  estándar del proyecto: toda petición entrante deja rastro antes incluso de llegar a la aplicación.
- **Punto natural para TLS.** Terminar HTTPS en un solo componente es trivial; hacerlo en cinco, no.

**En contra**

- **El gateway es un punto único de fallo.** Si nginx cae, todo cae. Mitigado con `healthcheck` y
  `restart: unless-stopped`; en la arquitectura objetivo el rol lo asume un ALB gestionado con
  redundancia entre zonas de disponibilidad.
- **Un salto más en la depuración.** Un `404` puede venir de nginx o de la aplicación. Se resuelve
  mirando el `access_log`, que distingue los dos casos.
- **Los prefijos de ruta hay que mantenerlos coherentes** entre `nginx.conf` y el
  `SERVER_CONTEXT_PATH` de cada aplicación. Es la fuente de error más probable en este montaje, y
  está documentada.
- **pgAdmin detrás de un prefijo requiere configuración adicional** (`SCRIPT_NAME: /pgadmin`), que
  fue el punto que más tiempo costó ajustar.

## Puesta en práctica

| Dónde | Qué |
|---|---|
| `docker-compose.yml` | **`ports:` aparece una sola vez**, en `nginx`. Todos los demás usan `expose:` |
| `nginx/nginx.conf` | Enrutamiento por prefijo, `/health` propio y `access_log /dev/stdout` |
| `nginx/proxy_headers.conf` | `X-Forwarded-*` para que las aplicaciones sepan el host y el esquema originales |
| `postman/…environment.json` | `baseUrl = http://localhost:9090`, una sola variable |
| `pgadmin/servers.json` | Las dos conexiones ya registradas; se accede vía `/pgadmin/` |

Verificación en un comando, pensada para ejecutarla delante del evaluador:

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:9090/matchpoint/courts && curl -s --max-time 3 http://localhost:8787/matchpoint/courts; echo "exit=$?"
```

La primera llamada responde `200`; la segunda falla, porque el puerto interno no está publicado.
Y `docker compose ps` muestra `PORTS` mapeado en un único servicio.
