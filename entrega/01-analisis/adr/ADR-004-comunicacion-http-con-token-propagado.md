# ADR-004 · Comunicación síncrona por HTTP propagando el token del usuario

- **Estado:** Aceptado
- **Fecha:** 2026-08-02
- **Decide:** Josué Herrera
- **Requerimientos afectados:** RF-16, RF-31 · RNF-07
- **Historias:** HU-07, HU-08

## Contexto

La regla de negocio RN-03 dice que **no se reserva sin perfil registrado**. El perfil vive en
`users`; la reserva se crea en `matchpoint`. Alguien tiene que cruzar la frontera.

Dos preguntas que había que responder juntas:

1. **¿Cómo viaja la petición?** Síncrona (HTTP) o asíncrona (mensajería).
2. **¿Con qué identidad viaja?** Con un token de servicio propio, o con el token del usuario final.

La segunda es la más peligrosa: un token de servicio con permisos amplios convierte a `matchpoint`
en un usuario privilegiado capaz de leer cualquier perfil, y anula las reglas de seguridad de
`users` justo cuando más falta hacen.

## Decisión

**Llamada síncrona HTTP, por el nombre de servicio de Compose, reenviando la cabecera
`Authorization` original del usuario.**

```
matchpoint  ──HTTP GET http://users:8686/users/me──►  users
            ◄──────── perfil del usuario ───────────
```

- La URL base llega por variable de entorno (`USERS_SERVICE_URL`), nunca `localhost` ni una IP.
- **`matchpoint` nunca fabrica un token.** Reenvía el del usuario que originó la petición, así que
  `users` aplica exactamente las mismas reglas que aplicaría a una llamada directa.
- *Timeout* explícito de 3 s. Al agotarse, `matchpoint` responde `503` y se recupera solo cuando el
  vecino vuelve.

## Alternativas consideradas

| Alternativa | Por qué se descartó |
|---|---|
| **Token de servicio** (credenciales propias de `matchpoint` contra Cognito) | `matchpoint` pasaría a ser un cliente privilegiado con acceso a todos los perfiles. La regla "solo ves tu perfil" dejaría de aplicarse en el camino que más importa. Propagar el token del usuario mantiene el principio de mínimo privilegio de extremo a extremo. |
| **Mensajería asíncrona** (SQS, RabbitMQ, Kafka) | La validación del perfil es una **precondición** de la reserva: hay que saber la respuesta *antes* de decidir si se guarda. Un mensaje asíncrono obligaría a crear la reserva en estado provisional y compensarla después con una saga. Complejidad enorme para un problema que la llamada síncrona resuelve en 50 ms. |
| **Duplicar la tabla de perfiles en `matchpoint`** | Rompe el aislamiento de datos que motivó [ADR-003](ADR-003-dos-microservicios-base-por-servicio.md) y obliga a mantener sincronizadas dos copias de la verdad. |
| **Consultar directamente `users_db` desde `matchpoint`** | El antipatrón que [ADR-003](ADR-003-dos-microservicios-base-por-servicio.md) prohíbe explícitamente. |

## Consecuencias

**A favor**

- **Seguridad coherente de extremo a extremo.** No existe un camino privilegiado que salte las
  reglas de `users`. Si el usuario no puede ver algo, `matchpoint` tampoco puede verlo por él.
- **Depuración trivial.** El mismo `sub` aparece en las líneas de log de los dos servicios, así
  que una petición se sigue de punta a punta con `docker compose logs -f`.
- **Sin infraestructura adicional.** No hay que operar un broker de mensajes.

**En contra**

- **Acoplamiento temporal.** Si `users` está caído, no se pueden crear reservas nuevas. Se aceptó
  porque es la semántica correcta: sin poder verificar el perfil, crear la reserva sería violar
  RN-03. **Lo que sí se garantizó es que la caída no se propague:** `matchpoint` responde `503`,
  sigue sirviendo el catálogo y los torneos, y se recupera solo.
- **Latencia sumada.** El salto añade unas decenas de milisegundos a `POST /reservations`.
- **El token debe estar vigente durante todo el trayecto.** Un token que expira entre el gateway y
  el segundo servicio produce un `401` del vecino. Con tokens de una hora no ocurre en la práctica.

**Decisión derivada:** la dependencia en `docker-compose.yml` entre `matchpoint` y `users` es
`service_started`, **no** `service_healthy`. Exigir `healthy` convertiría una dependencia de datos
en una dependencia de arranque: si `users` tarda, `matchpoint` ni siquiera levantaría. Con
`service_started`, arranca igual y devuelve `503` hasta que el vecino responda.

## Puesta en práctica

| Dónde | Qué |
|---|---|
| `matchpoint/.../clients/UsersClient.kt` | Reenvía la cabecera `Authorization` de la petición en curso |
| `matchpoint/.../config/HttpClientConfig.kt` | *Timeout* de 3 s (`services.users.timeout-millis`) |
| `matchpoint/.../exceptions/IntegrationExceptions.kt` | `UsersServiceUnavailableException` → `503`; `ProfileNotRegisteredException` → `409` |
| `matchpoint/src/test/.../UsersClientTest.kt` | 7 pruebas: propagación del token, *timeout*, perfil ausente, vecino caído |
| `docker-compose.yml` | `USERS_SERVICE_URL: http://users:8686` y `depends_on: users: condition: service_started` |

Verificación en vivo, en dos comandos:

```bash
docker compose stop users
# POST /matchpoint/reservations  →  503
docker compose start users
# POST /matchpoint/reservations  →  201, sin reiniciar matchpoint
```
