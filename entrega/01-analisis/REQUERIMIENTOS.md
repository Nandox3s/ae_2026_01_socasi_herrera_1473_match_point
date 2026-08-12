# MatchPoint · Levantamiento de requerimientos

**Criterio 1.1 de la rúbrica** — precisión y calidad en la descripción de cada RF y RNF.

Todos los requerimientos de este documento están **derivados del sistema construido**, no de una
lista de deseos: cada fila apunta al endpoint, la clase y la prueba que lo evidencian. La columna
*Evidencia* es la que se abre en vivo durante la sustentación.

- **Notación:** `RF-nn` requerimiento funcional · `RNF-nn` requerimiento no funcional.
- **Prioridad:** `Must` imprescindible para el objetivo del sistema · `Should` importante, el
  sistema funciona sin él pero queda incompleto · `Could` mejora deseable.
- **Estado:** `Implementado` verificado con pruebas automáticas y con la colección de Postman.

---

## 1. Alcance y objetivo del sistema

MatchPoint resuelve dos problemas del básquet amateur en Quito que hoy se manejan por WhatsApp y
cuadernos: **reservar una cancha sin choques de horario** y **llevar un torneo de eliminación
directa sin planillas manuales**.

El sistema es un backend de **dos microservicios** (`users` y `matchpoint`) detrás de un API
Gateway, con identidad delegada a **AWS Cognito** y una base de datos PostgreSQL por servicio.

Fuera de alcance de esta entrega: pagos en línea, deportes distintos de básquet (el enum
`SportType` está preparado pero solo expone `BASKET`), y torneos de formato liga o grupos.

---

## 2. Actores

| Actor | Origen de la identidad | Descripción |
|---|---|---|
| **PLAYER** | Grupo `PLAYER` del User Pool de Cognito | Jugador. Reserva canchas e inscribe equipos en torneos. |
| **MANAGER** | Grupo `MANAGER` del User Pool de Cognito | Administrador de complejo deportivo. Publica canchas, organiza torneos y administra perfiles. |
| **Visitante** | Sin token | Cualquiera que consulte el catálogo público de canchas y torneos. |
| **Microservicio `matchpoint`** | Token del usuario, propagado | Actor de sistema: consume `users` por HTTP para validar el perfil del jugador. |
| **AWS Cognito** | — | Sistema externo. Emite y firma los JWT; el backend solo los valida contra el JWKS. |

---

## 3. Requerimientos funcionales

### 3.1 Identidad y perfiles — microservicio `users`

| ID | Requerimiento | Descripción precisa | Actor | Prioridad | Evidencia | Estado |
|---|---|---|---|---|---|---|
| **RF-01** | Registro automático de perfil | Un usuario autenticado crea su perfil con `POST /users/me`. El `cognito_id` y el `username` **no se envían en el body**: se leen de los claims `sub` y `username` del JWT. Un mismo usuario de Cognito no puede tener dos perfiles (`409`). | PLAYER, MANAGER | Must | `UserController.createMyProfile` · `UserService.createUser` · `UserControllerTest` | Implementado |
| **RF-02** | Consulta del perfil propio | `GET /users/me` devuelve el perfil asociado al `sub` del token. Si el usuario aún no se registró, responde `404` con mensaje explícito. | PLAYER, MANAGER | Must | `UserService.getUserByCognitoId` | Implementado |
| **RF-03** | Actualización de datos de contacto | `PUT /users/me` actualiza nombre, correo y teléfono del **propio** perfil. El nombre no puede quedar en blanco (`400`). No existe endpoint para editar el perfil de otro. | PLAYER, MANAGER | Must | `UserService.updateUser` · `BlankNameException` | Implementado |
| **RF-04** | Listado de perfiles | `GET /users` devuelve todos los perfiles. Exclusivo de MANAGER; un PLAYER recibe `403`. | MANAGER | Should | `SecurityConfig` (`hasRole(MANAGER)`) · `UserControllerTest` | Implementado |
| **RF-05** | Consulta de perfil por identificador | `GET /users/{id}` devuelve un perfil concreto. Exclusivo de MANAGER. | MANAGER | Should | `UserController.getUserById` | Implementado |
| **RF-06** | Baja de perfil | `DELETE /users/{id}` elimina un perfil y deja el registro en `audit_log` con los valores previos. Exclusivo de MANAGER. | MANAGER | Should | `UserService.deleteUser` | Implementado |
| **RF-07** | Resolución de perfil por `cognitoId` | `GET /users/cognito/{cognitoId}` permite a un usuario autenticado resolver un perfil a partir del identificador de Cognito. Es el endpoint que consume la integración entre microservicios. | Sistema | Should | `UserController` · `UsersClient` | Implementado |

### 3.2 Seguridad transversal

| ID | Requerimiento | Descripción precisa | Actor | Prioridad | Evidencia | Estado |
|---|---|---|---|---|---|---|
| **RF-08** | Autenticación por JWT de Cognito | Cada microservicio actúa como *OAuth2 Resource Server*: descarga el JWKS del User Pool y valida **firma, emisor y expiración** de cada token. Sin token, token expirado o token manipulado → `401`. No existe JWT propio, ni sesión, ni Basic Auth, ni usuarios en memoria. | Todos | Must | `SecurityConfig` (ambos servicios) · `LoggingAuthenticationEntryPoint` | Implementado |
| **RF-09** | Autorización por rol | El claim `cognito:groups` se traduce a autoridades de Spring (`MANAGER` → `ROLE_MANAGER`). Cada endpoint declara el rol que admite; el rol equivocado recibe `403`. | Todos | Must | `CognitoGroupsConverter` · `CognitoGroupsConverterTest` | Implementado |
| **RF-10** | Autorización por propiedad del recurso | Segunda capa de `403`, decidida en el *service*: un MANAGER no puede editar la cancha de otro MANAGER, ni un PLAYER cancelar la reserva de otro. Se resuelve con `findCourtOwnedBy`, `findReservationOwnedBy` y `findTournamentOwnedBy`. **Los DTO de request ni siquiera tienen el campo de propietario**, así que no se puede suplantar por body. | Todos | Must | `CourtService.findCourtOwnedBy` · `ReservationService.findReservationOwnedBy` · `TournamentService.findTournamentOwnedBy` | Implementado |

### 3.3 Gestión de canchas — microservicio `matchpoint`

| ID | Requerimiento | Descripción precisa | Actor | Prioridad | Evidencia | Estado |
|---|---|---|---|---|---|---|
| **RF-11** | Publicación de cancha | `POST /matchpoint/courts` registra una cancha con nombre, sector, tipo de piso, parqueo y precio por hora. Nombre, sector y piso son obligatorios y el precio debe ser `> 0`, si no `400`. El `managerUser` sale del token. | MANAGER | Must | `CourtService.createCourt` · `InvalidCourtException` | Implementado |
| **RF-12** | Catálogo público de canchas | `GET /matchpoint/courts` lista las canchas **sin necesidad de token**, con filtros opcionales por `sector` y `sport`. Es el escaparate del producto. | Visitante | Must | `CourtService.listCourts` · `SecurityConfig` (`permitAll`) | Implementado |
| **RF-13** | Detalle de cancha | `GET /matchpoint/courts/{id}` devuelve una cancha; si no existe, `404`. Público. | Visitante | Should | `CourtService.getCourt` | Implementado |
| **RF-14** | Motor de búsqueda de disponibilidad | `GET /matchpoint/courts/available?sector=&sport=&startsAt=&durationMinutes=` devuelve **solo canchas activas** y, si se envía franja horaria, solo aquellas **sin ninguna reserva `CONFIRMED` que se solape** con `[startsAt, startsAt + durationMinutes)`. Sin franja, devuelve las activas que cumplen los filtros. | Visitante | Must | `CourtService.availableCourts` · `CourtService.isFree` · `CourtServiceTest` | Implementado |
| **RF-15** | Actualización de cancha | `PATCH /matchpoint/courts/{id}` permite al **dueño** cambiar precio y estado activo/inactivo. Precio `<= 0` → `400`; cancha ajena → `403`. Es una actualización parcial: los campos nulos no se tocan. | MANAGER | Should | `CourtService.updateCourt` | Implementado |

### 3.4 Motor de reservas

| ID | Requerimiento | Descripción precisa | Actor | Prioridad | Evidencia | Estado |
|---|---|---|---|---|---|---|
| **RF-16** | Reserva sujeta a perfil registrado | `POST /matchpoint/reservations` **primero** pide el perfil del jugador al microservicio `users` (`GET /users/me`, propagando el token). Si el jugador no tiene perfil → `409`; si `users` no responde en 3 s → `503`. El nombre devuelto se copia en `reservations.owner_name`. | PLAYER | Must | `ReservationService.createReservation` · `UsersClient` · `UsersClientTest` | Implementado |
| **RF-17** | Validación de solapamiento y de estado de la cancha | Antes de guardar: la cancha debe existir (`404`), estar **activa** (`409`), la duración debe ser `> 0` (`400`) y **no puede existir ninguna reserva `CONFIRMED` que se solape** en esa cancha (`409`). El solapamiento se evalúa con `inicioA < finB && inicioB < finA`, que también cubre los casos de contención parcial. | PLAYER | Must | `ReservationService.createReservation` · `CourtNotAvailableException` · `ReservationServiceTest` | Implementado |
| **RF-18** | Listado de reservas propias | `GET /matchpoint/reservations/me` devuelve **solo** las reservas del jugador del token, ordenadas por fecha de inicio descendente. | PLAYER | Must | `ReservationService.listMine` | Implementado |
| **RF-19** | Detalle de reserva propia | `GET /matchpoint/reservations/{id}` devuelve la reserva si es del solicitante; ajena → `403`; inexistente → `404`. | PLAYER | Should | `ReservationService.getMine` | Implementado |
| **RF-20** | Cancelación de reserva | `DELETE /matchpoint/reservations/{id}` marca la reserva como `CANCELLED` (**borrado lógico**, la fila se conserva) y libera la franja para nuevas reservas. Solo el dueño. | PLAYER | Must | `ReservationService.cancel` · `ReservationStatus` | Implementado |

### 3.5 Torneos e inscripciones

| ID | Requerimiento | Descripción precisa | Actor | Prioridad | Evidencia | Estado |
|---|---|---|---|---|---|---|
| **RF-21** | Creación de torneo | `POST /matchpoint/tournaments` crea el torneo en estado `REGISTRATION`. `maxTeams` debe ser **potencia de dos entre 2 y 32** (2, 4, 8, 16, 32) para armar un cuadro perfecto sin *byes*; cualquier otro valor → `400`. Si se indica sede, la cancha debe existir y **ser del propio MANAGER** (`403`). | MANAGER | Must | `TournamentService.createTournament` · `isPowerOfTwo` · `resolveCourt` | Implementado |
| **RF-22** | Consulta pública de torneos y del cuadro | `GET /matchpoint/tournaments` lista torneos con su conteo de equipos inscritos. `GET /matchpoint/tournaments/{id}` devuelve el **progreso completo**: estado, equipos inscritos, rondas con nombre legible (*Final*, *Semifinals*, *Quarterfinals*, *Round of 16*, *Round of 32*) y campeón si lo hay. Sin token. | Visitante | Must | `TournamentService.getProgress` · `buildRounds` · `roundName` | Implementado |
| **RF-23** | Inscripción de equipo | `POST /matchpoint/tournaments/{id}/teams` inscribe un equipo con sus datos de contacto. Rechaza: torneo que ya arrancó (`409`), cupo lleno (`409`), nombre de equipo repetido dentro del mismo torneo (`409`) y datos de contacto en blanco (`400`). El `registeredByUser` sale del token. | PLAYER | Must | `TournamentService.registerTeam` · `TournamentFullException` · `DuplicateTeamNameException` | Implementado |
| **RF-24** | Retiro de equipo | `DELETE /matchpoint/tournaments/{id}/teams/{teamId}` retira un equipo **solo mientras el torneo siga en `REGISTRATION`** (`409` si ya arrancó) y **solo si lo inscribió el mismo jugador** (`403`). | PLAYER | Should | `TournamentService.withdrawTeam` | Implementado |
| **RF-25** | Consulta de equipos | `GET /matchpoint/tournaments/{id}/teams` y `.../teams/{teamId}` exponen los equipos con sus datos externos (contacto) y sus estadísticas internas. Público. | Visitante | Could | `TournamentService.listTeams` · `getTeam` | Implementado |

### 3.6 Cuadro de partidos

| ID | Requerimiento | Descripción precisa | Actor | Prioridad | Evidencia | Estado |
|---|---|---|---|---|---|---|
| **RF-26** | Generación automática del cuadro | `POST /matchpoint/tournaments/{id}/start` exige **exactamente `maxTeams` equipos inscritos** (`409` si faltan o sobran) y que el torneo esté en `REGISTRATION` (`409`). Genera **todas** las rondas de una vez: la primera con los emparejamientos por orden de inscripción y estado `READY`; las siguientes con slots vacíos en estado `PENDING`. El torneo pasa a `IN_PROGRESS`. | MANAGER | Must | `TournamentService.startTournament` · `TournamentNotReadyException` | Implementado |
| **RF-27** | Programación de partido | `PATCH /matchpoint/matches/{matchId}/schedule` fija fecha y hora del partido. Solo el MANAGER dueño del torneo (`403`); un partido ya jugado no se reprograma (`409`). | MANAGER | Should | `TournamentService.scheduleMatch` | Implementado |
| **RF-28** | Registro de marcador | `PATCH /matchpoint/matches/{matchId}/score` registra el marcador. Rechaza: partido sin los dos equipos definidos (`409`), partido ya jugado (`409`), marcadores negativos (`400`) y **empates** (`400`), porque en eliminación directa siempre debe haber un ganador. | MANAGER | Must | `TournamentService.registerScore` · `TieNotAllowedException` | Implementado |
| **RF-29** | Avance automático del ganador y estadísticas | Al registrar el marcador, el sistema actualiza las estadísticas de los dos equipos (partidos jugados, ganados, perdidos, puntos a favor y en contra), marca al perdedor como `eliminated` y **mueve al ganador al slot correcto de la siguiente ronda** (`ronda+1`, `posición/2`; local si la posición era par, visitante si impar). Cuando el slot padre queda con sus dos equipos, pasa a `READY`. | Sistema | Must | `TournamentService.advanceWinner` · `applyStats` | Implementado |
| **RF-30** | Cierre del torneo | Si el partido puntuado es el de la **última ronda**, el ganador queda como `championTeam` y el torneo pasa a `FINISHED`. A partir de ahí no admite inscripciones ni nuevos marcadores. | Sistema | Must | `TournamentService.registerScore` · evento `tournament.finished` | Implementado |

### 3.7 Requerimientos transversales

| ID | Requerimiento | Descripción precisa | Actor | Prioridad | Evidencia | Estado |
|---|---|---|---|---|---|---|
| **RF-31** | Identidad efectiva del usuario | `GET /matchpoint/me` devuelve lo que dice el token (`username`, `sub`, `groups`) **y** el perfil traído en caliente del microservicio `users`. Es la prueba más corta de que el token cruza el gateway y salta de un microservicio al otro. | PLAYER, MANAGER | Should | `MeController` · `MeControllerTest` | Implementado |
| **RF-32** | Auditoría de toda escritura | Cada `INSERT`, `UPDATE` y `DELETE` de las entidades de dominio deja una fila en `audit_log` con **quién** (`user_sub` + `user_name` del token), **qué** (`entity_name`, `entity_id`, `action`), **cuándo** (`created_at`) y los **valores anteriores y nuevos**. | Sistema | Should | `AuditService` · `AuditServiceTest` | Implementado |
| **RF-33** | Trazabilidad de toda petición | Toda petición HTTP —incluidas las que terminan en `401` y `403`— deja línea de entrada y de salida en el log, con el `sub` del token en el MDC. Se implementa **una sola vez por servicio** en un filtro, no con `log.info` repartidos por los controladores. | Sistema | Should | `ApiLoggingFilter` · `LoggingAccessDeniedHandler` · `ApiLoggingFilterTest` | Implementado |

**Total: 33 requerimientos funcionales, todos implementados y cubiertos por pruebas.**

---

## 4. Requerimientos no funcionales

| ID | Categoría | Requerimiento | Criterio de aceptación medible | Evidencia | Estado |
|---|---|---|---|---|---|
| **RNF-01** | Seguridad | La identidad se delega por completo a AWS Cognito (OIDC). El backend nunca almacena contraseñas ni emite tokens. | No existe ninguna tabla de credenciales ni ninguna clase que firme JWT. Cada servicio solo configura `issuer-uri`. | `application.yaml` · `SecurityConfig` | Cumplido |
| **RNF-02** | Privacidad | Ningún dato sensible aparece completo en logs ni en auditoría. | `juan.perez@puce.edu.ec` → `j***@puce.edu.ec`; `0999555666` → `****5666`. Nunca se loguea una contraseña ni un token completo. | `LogEvents.maskEmail` / `maskPhone` · `LogEventsTest` | Cumplido |
| **RNF-03** | Seguridad de red | Un único punto de entrada. Solo el gateway publica puerto al host. | `docker compose ps` muestra `PORTS` mapeado **solo** en `nginx`. `curl http://localhost:8787/...` falla; vía `:9090` responde `200`. | `docker-compose.yml` (`expose` vs `ports`) | Cumplido |
| **RNF-04** | Arquitectura de datos | *Database per service*: ningún servicio accede a la base del otro. | Dos contenedores Postgres con credenciales, base y volumen distintos. Cero claves foráneas entre bases, cero `JOIN` entre dominios. | `docker-compose.yml` · `docs/MODELO-ER.md` | Cumplido |
| **RNF-05** | Reproducibilidad | Todas las imágenes con versión fija; nunca `latest`. | `postgres:16-alpine`, `nginx:1.27-alpine`, `dpage/pgadmin4:8.14`. | `docker-compose.yml` | Cumplido |
| **RNF-06** | Disponibilidad | Cada contenedor declara `healthcheck` y política de reinicio. | `restart: unless-stopped` en los seis servicios; las bases con `pg_isready`; las apps con `/actuator/health`. | `docker-compose.yml` | Cumplido |
| **RNF-07** | Tolerancia a fallos | La caída de un microservicio no tumba al otro. | La llamada a `users` tiene *timeout* explícito de 3 s; al agotarse, `matchpoint` responde `503` y se recupera solo cuando el vecino vuelve. La dependencia entre microservicios es `service_started`, no `service_healthy`. | `HttpClientConfig` · `UsersServiceUnavailableException` | Cumplido |
| **RNF-08** | Calidad | Cobertura de líneas del **100 %** sobre el código propio, verificada por el build. | `./gradlew check` falla si la cobertura de líneas baja de 1.00. 157 pruebas en `matchpoint`, 57 en `users`. | `build.gradle.kts` (`jacocoTestCoverageVerification`) | Cumplido |
| **RNF-09** | Observabilidad | Log de una sola línea, campos y orden fijos, con el `sub` del usuario en todas. | Patrón `%d \| %-5level \| servicio \| sub=%X{sub:-anonimo} \| logger \| msg`. SQL registrado en dos capas: Hibernate (`DEBUG`) y el motor Postgres (`log_statement=all`). | `application.yaml` · `docker-compose.yml` | Cumplido |
| **RNF-10** | Portabilidad | El sistema completo se levanta en una máquina limpia con un solo comando. | `cp .env.example .env && docker compose up -d --build`. No requiere JDK, Gradle ni Postgres instalados en el host. | `docker-compose.yml` · `Dockerfile` multi-stage | Cumplido |
| **RNF-11** | Trazabilidad | Toda línea de log permite reconstruir quién hizo qué. | El `sub` del JWT se pone en el MDC en el filtro; sin usuario autenticado se escribe `sub=anonimo`, nunca se omite el campo. | `ApiLoggingFilter` | Cumplido |
| **RNF-12** | Mantenibilidad | Un único idioma técnico en todo el código. | Endpoints, tablas, columnas, clases, métodos, variables, eventos de log y mensajes de error de la API **en inglés**. El español queda solo para la documentación. | Todo `src/` | Cumplido |
| **RNF-13** | Seguridad de la configuración | Ningún secreto se versiona. | `.env`, `pgadmin/pgpass` y el *client secret* de Cognito están en `.gitignore`; se versionan solo las plantillas `.example`. `git log -p` no contiene ninguna clave. | `.gitignore` · `.env.example` | Cumplido |
| **RNF-14** | Consistencia de la API | Los errores se traducen a códigos HTTP semánticos en un único lugar. | `401` sin identidad · `403` rol o propiedad · `404` inexistente · `400` petición inválida · `409` conflicto de estado · `503` dependencia caída. Ningún `try/catch` de traducción en los controladores. | `GlobalExceptionHandler` | Cumplido |
| **RNF-15** | Plataforma | Stack fijado y actual. | Kotlin 2.2.21 · Spring Boot 4.0.6 · Java 21 (toolchain) · PostgreSQL 16. | `build.gradle.kts` | Cumplido |
| **RNF-16** | Rendimiento | El catálogo público responde sin autenticación ni consulta remota. | `GET /courts` y `GET /tournaments` no llaman a Cognito ni a `users`: resuelven contra la base local. | `SecurityConfig` (`permitAll`) | Cumplido |

**Total: 16 requerimientos no funcionales, todos cumplidos y verificables en vivo.**

---

## 5. Reglas de negocio explícitas

Las reglas que el sistema **hace cumplir en código** y que un evaluador puede intentar romper:

| # | Regla | Se rompe con… | Respuesta |
|---|---|---|---|
| RN-01 | Una cancha no admite dos reservas confirmadas solapadas. | Reservar la misma franja dos veces | `409` |
| RN-02 | Una cancha inactiva no se puede reservar. | Reservar la cancha 3 (sembrada inactiva) | `409` |
| RN-03 | No se reserva sin perfil registrado en `users`. | Reservar con un usuario sin `POST /users/me` | `409` |
| RN-04 | El cupo de un torneo es potencia de dos entre 2 y 32. | Crear torneo con `maxTeams: 6` | `400` |
| RN-05 | Un torneo arranca solo con el cupo exacto. | `start` con 3 de 4 equipos | `409` |
| RN-06 | No hay nombres de equipo repetidos dentro de un torneo. | Inscribir dos veces el mismo nombre | `409` |
| RN-07 | No se inscribe ni se retira un equipo con el torneo ya arrancado. | Inscribir en torneo `IN_PROGRESS` | `409` |
| RN-08 | En eliminación directa no hay empates. | Marcador `50 - 50` | `400` |
| RN-09 | Un partido no se puntúa dos veces. | Repetir el `PATCH .../score` | `409` |
| RN-10 | Un partido sin los dos equipos definidos no se puntúa. | Puntuar un slot `PENDING` | `409` |
| RN-11 | Nadie modifica un recurso que no es suyo, aunque tenga el rol. | `PATCH /courts/4` con otro MANAGER | `403` |
| RN-12 | Un rol no ejecuta las operaciones del otro. | `POST /courts` con un PLAYER | `403` |
| RN-13 | Un usuario de Cognito tiene un solo perfil. | `POST /users/me` dos veces | `409` |
| RN-14 | Si `users` no responde, `matchpoint` degrada, no falla en cascada. | Apagar el contenedor `users` y reservar | `503` |

Los datos sembrados (`data.sql`) incluyen a propósito el caso que dispara **cada una** de estas
reglas, para poder demostrarlas sin preparar nada.

---

## 6. Trazabilidad requerimiento → historia → rama

| Historia | Requerimientos | Rama de GitFlow |
|---|---|---|
| HU-01 Registro y visualización de perfil | RF-01, RF-02 | `feature/HU-01-registro-perfil` |
| HU-02 Actualización de datos de contacto | RF-03 | `feature/HU-02-actualizacion-perfil` |
| HU-03 Administración de perfiles | RF-04, RF-05, RF-06, RF-07 | `feature/HU-03-admin-usuarios` |
| HU-04 Autenticación con Cognito | RF-08, RF-09, RF-10 | `feature/HU-04-autenticacion-cognito` |
| HU-05 / HU-06 Gestión de canchas y disponibilidad | RF-11 … RF-15 | `feature/HU-05-06-gestion-canchas` |
| HU-07 / HU-08 Motor de reservas | RF-16 … RF-20 | `feature/HU-07-08-motor-reservas` |
| HU-09 / HU-10 Torneos e inscripción | RF-21 … RF-25 | `feature/HU-09-10-inscripcion-torneos` |
| HU-11 / HU-12 Cuadro de partidos | RF-26 … RF-30 | `feature/HU-11-12-cuadro-partidos` |
| Configuración, observabilidad y documentación | RF-31, RF-32, RF-33 · RNF-01 … RNF-16 | `feature/configuracion-y-documentacion` |

La trazabilidad es verificable en el historial: `git log --oneline` muestra un commit por historia,
con el identificador `HU-nn` en el mensaje.

---

## 7. Documentos relacionados

- Casos de uso detallados: [`CASOS-DE-USO.md`](CASOS-DE-USO.md)
- Justificación de la priorización: [`adr/`](adr/)
- Manejo de ramas: [`GITFLOW.md`](GITFLOW.md)
- Estrategia de pruebas: [`PRUEBAS-UNITARIAS.md`](PRUEBAS-UNITARIAS.md)
