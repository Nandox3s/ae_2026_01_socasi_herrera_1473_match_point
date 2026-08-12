# MatchPoint · Guion de la demo

Guion para presentar el proyecto en vivo. Está pensado para **15 minutos** y para que cada
paso demuestre un criterio de la entrega, no para lucir pantallas.

Todo lo que aparece aquí se puede ejecutar tal cual: los datos sembrados
(`data.sql` de cada microservicio) ya incluyen los casos que disparan cada error.

---

## 0. Diez minutos antes (sin público)

```bash
cd ~/josue/match
docker compose down -v          # arranque limpio: sin datos de pruebas anteriores
docker compose up -d --build
docker compose ps               # esperar a que todo diga (healthy)
```

Checklist previo:

- [ ] `docker compose ps`: `nginx`, `users`, `matchpoint`, `users-db`, `matchpoint-db`, `pgadmin` en `healthy`.
- [ ] `curl -s http://localhost:9090/health` responde.
- [ ] Postman abierto, environment **MatchPoint - local (nginx 9090)** seleccionado y con
      `cognitoClientSecret`, `managerPassword` y `playerPassword` rellenos.
- [ ] Los dos usuarios existen y están **CONFIRMED** en el User Pool: `manager_josue` (grupo
      `MANAGER`) y `player_fernando` (grupo `PLAYER`). Si alguno está en
      `FORCE_CHANGE_PASSWORD`, el token no sale: cambiar la clave antes.
- [ ] El login funciona. Comprobación de 10 segundos, sin Postman de por medio:

      ```bash
      ./scripts/cognito-token.sh manager_josue
      ```

      Pide el client secret y la clave por teclado (sin eco) e imprime el `access_token`.
      Si esto responde, la carpeta `0` de Postman también.
- [ ] Tres ventanas listas: **Postman**, **terminal con los logs**, **explorador de base de datos**.

Terminal de logs, que se queda visible toda la demo:

```bash
docker compose logs -f --tail=0 users matchpoint
```

---

## 1. Arquitectura en una pantalla · 2 min

Mostrar el diagrama del [README](../README.md#1-arquitectura) y decir tres frases:

1. **Dos microservicios**, cada uno con **su propia base**. No comparten tablas ni hacen
   `JOIN` entre dominios: lo ajeno se pide por HTTP.
2. **Un solo punto de entrada**: nginx en el puerto `9090`. Los microservicios no publican
   puerto; solo las bases publican el suyo, y únicamente para poder inspeccionarlas.
3. **La identidad no es nuestra**: la pone AWS Cognito. Cada servicio es un *resource server*
   que valida el JWT contra el JWKS del User Pool.

Prueba de que el gateway es el único camino a la API:

```bash
curl -s -o /dev/null -w "por nginx: %{http_code}\n" http://localhost:9090/matchpoint/courts
curl -s --max-time 3 -o /dev/null -w "directo al micro: %{http_code}\n" http://localhost:8787/matchpoint/courts || echo "directo al micro: sin respuesta (puerto no publicado)"
```

---

## 2. Cognito: de dónde sale el token · 2 min

Postman → carpeta **`0. Cognito - obtener los tokens`** → *Run folder*.

Lo que hay que explicar mientras corre (es la parte que preguntó el profesor):

- Se usa el App Client **`matchpoint-client`**, el que ya existía en el User Pool. No se creó
  ningún cliente aparte para la demo.
- Ese cliente **tiene client secret**, así que cada llamada va firmada con
  `SECRET_HASH = Base64(HMAC-SHA256(username + clientId, clientSecret))`. Lo calcula el
  pre-request script de la carpeta; el secreto vive solo en el environment de Postman, nunca
  en el repositorio ni en el código de los microservicios.
- Su flujo habilitado es **inicio de sesión basado en opciones (`USER_AUTH`)**, no
  `USER_PASSWORD_AUTH`. Por eso el login son dos llamadas: `InitiateAuth` pide el reto
  (`PREFERRED_CHALLENGE: PASSWORD`) y `RespondToAuthChallenge` lo responde con la clave.
- **El backend no conoce el client id ni el secret.** Solo conoce el *issuer*: descarga el
  JWKS y valida firma, emisor y expiración. Cambiar de App Client no toca ni una línea de
  código.

Abrir un token en [jwt.io](https://jwt.io) y señalar los tres claims que usa el sistema:
`sub` (identidad), `username` (dueño de canchas, torneos y reservas) y `cognito:groups`
(`MANAGER` / `PLAYER`, que Spring convierte en `ROLE_MANAGER` / `ROLE_PLAYER`).

---

## 3. Recorrido de negocio · 4 min

Carpetas `1` a `4` de la colección, en orden. Puntos donde vale la pena parar:

| Paso | Request | Qué demuestra |
|---|---|---|
| Perfil | `POST /users/me` | El `cognitoId` y el `username` **no van en el body**: salen del token |
| Cancha | `POST /matchpoint/courts` (MANAGER) | El `managerUser` también sale del token |
| Reserva | `POST /matchpoint/reservations` (PLAYER) | **Comunicación entre microservicios**: `matchpoint` llama a `users` por HTTP y copia el nombre en `owner_name` |
| Torneo | carpeta `4`, de punta a punta | Crear → inscribir equipos → arrancar el cuadro → programar → puntuar → campeón |

En el paso de la reserva, señalar en la terminal de logs la línea
`users.profile.requested` del microservicio `matchpoint` seguida de la petición entrante en
`users`: es la llamada entre servicios, por nombre de servicio de Compose (`http://users:8686`),
nunca por IP ni por `localhost`.

---

## 4. Seguridad: los cuatro rechazos · 2 min

Están sembrados a propósito, así que no hay que preparar nada:

| Caso | Request | Respuesta |
|---|---|---|
| Sin token | `GET /matchpoint/courts` sin `Authorization` | `401` |
| Token manipulado | carpeta `5`, request con la firma alterada | `401` (falla contra el JWKS) |
| Rol equivocado | `POST /matchpoint/courts` con el token del PLAYER | `403` por rol |
| Recurso ajeno | `PATCH /matchpoint/courts/4` con el token de `manager_josue` (la cancha 4 es de `manager_ana`) | `403` por propiedad |

Y las reglas de negocio, que son `409`, no `500`:

| Caso | Request | Respuesta |
|---|---|---|
| Franja ocupada | reservar la misma hora de la reserva 1 | `409` |
| Cancha inactiva | reservar la cancha 3 | `409` |
| Cupo incompleto | `POST /matchpoint/tournaments/1/start` (3 de 4 equipos) | `409` |
| Inscripción cerrada | inscribir equipo en el torneo 3 (`FINISHED`) | `409` |

Frase que cierra el punto: *el rol lo decide Spring Security; el dueño lo decide el servicio.
Son dos capas distintas de 403.*

---

## 5. Base de datos · 3 min

El explorador entra **por el mismo gateway que la API**, con las dos conexiones ya registradas:

```
http://localhost:9090/pgadmin/
```

| Conexión en pgAdmin | Base de datos | Usuario |
|---|---|---|
| `users-db` | `users_db` | `users_app` |
| `matchpoint-db` | `matchpoint_db` | `matchpoint_app` |

Las bases no publican puerto al host: se llega a ellas desde dentro de la red de Compose, que
es lo que hace pgAdmin. Para verlo en terminal, `docker compose exec` entra al contenedor.

Qué mostrar, en este orden:

1. **Database-per-service.** Dos servidores distintos, dos bases, dos usuarios, dos volúmenes.
   En `users_db` solo hay `users` y `audit_log`; el dominio deportivo no está por ningún lado.

   ```bash
   docker compose exec users-db psql -U users_app -d users_db -c "\dt"
   docker compose exec matchpoint-db psql -U matchpoint_app -d matchpoint_db -c "\dt"
   ```

2. **El modelo entidad-relación.** Abrir [`docs/MODELO-ER.md`](MODELO-ER.md) —o, para mostrarlo
   generado por la base real, clic derecho sobre `matchpoint_db` en pgAdmin → **ERD For
   Database**, que dibuja el diagrama a partir de las claves foráneas que creó Hibernate.
   Explicar la entidad asociativa: `matches` resuelve el N:M de `teams` contra `teams`.

3. **La fila recién creada.** La reserva del paso 3, con su `owner_name` copiado del otro
   microservicio:

   ```bash
   docker compose exec matchpoint-db psql -U matchpoint_app -d matchpoint_db -c "SELECT id, court_id, owner_user, owner_name, starts_at, status FROM reservations ORDER BY id DESC LIMIT 3;"
   ```

4. **La auditoría.** Quién, qué, cuándo y los valores anteriores/nuevos:

   ```bash
   docker compose exec matchpoint-db psql -U matchpoint_app -d matchpoint_db -c "SELECT created_at, user_name, action, entity_name, entity_id FROM audit_log ORDER BY id DESC LIMIT 5;"
   ```

> Lo mismo se hace con clics en pgAdmin (*Query Tool*), que es más vistoso en la defensa. Los
> comandos están aquí por si el navegador falla.

---

## 6. Logs en vivo · 1 min

Volver a la terminal de logs y disparar una petición cualquiera desde Postman. Señalar que de
una sola petición salen cuatro cosas, en una línea por evento y con el mismo formato en los
dos servicios:

```
<timestamp> | <LEVEL> | <servicio> | sub=<sub del token> | <logger> | event=<evento> | msg=<...>
```

1. La línea de entrada (método, ruta).
2. El evento de negocio (`reservation.created`, `users.profile.requested`...).
3. El SQL que ejecutó Hibernate, con sus parámetros.
4. La línea de salida con el código HTTP y la duración.

Repetir con un `401` y con un `403` para mostrar que los rechazos también quedan registrados,
con `sub=anonimo` en el caso del `401`.

---

## 7. Pruebas · 1 min

```bash
(cd users && ./gradlew test) && (cd matchpoint && ./gradlew test)
```

El reporte de cobertura (JaCoCo) queda en `build/reports/jacoco/test/html/index.html` de cada
microservicio.

---

## Preguntas que suelen caer

**¿Cómo entras a la base si no publica puerto?**
Por pgAdmin, que corre dentro del stack y se sirve a través del mismo gateway
(`http://localhost:9090/pgadmin/`). pgAdmin está en la red interna de Compose, así que llega a
`users-db:5432` y `matchpoint-db:5432` por nombre de servicio, igual que los microservicios.
Desde el host no hay forma de tocar las bases directamente, y esa es la idea.

**¿Por qué `matchpoint` guarda `owner_name` en vez de consultarlo cada vez?**
Porque es un dato de otro dominio. Se pide una vez, en el momento de crear la reserva, y se
guarda una copia. Si `users` se cae después, las reservas ya creadas se siguen leyendo.

**¿Y si el token es de otro User Pool?**
Los dos servicios arman el issuer con las mismas dos variables de entorno
(`COGNITO_REGION` y `COGNITO_USER_POOL_ID`), así que resuelven exactamente el mismo valor.
Un token de otro pool no valida contra el JWKS: `401`.

**¿Dónde está el client secret?**
Solo en el environment de Postman, como variable de tipo *secret*. No está en el repositorio,
ni en el `.env`, ni en el código: el backend no lo necesita, porque no pide tokens, los valida.

**¿Por qué el login son dos llamadas y no una?**
Porque el App Client que ya existía (`matchpoint-client`) tiene habilitado el inicio de sesión
basado en opciones (`USER_AUTH`), donde el cliente primero pregunta qué reto debe resolver y
después lo responde. Es el flujo recomendado por AWS y no exige habilitar
`USER_PASSWORD_AUTH`, que manda la contraseña en la primera llamada.
