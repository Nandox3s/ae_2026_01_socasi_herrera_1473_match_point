# MatchPoint · Arquitectura Empresarial

**Criterio 3 de la rúbrica (/8).** Este documento recorre los seis sub-criterios y, en cada uno,
señala **el archivo y la línea** donde vive la evidencia. Está escrito para leerse con el código
abierto al lado.

| Sub-criterio | Puntos | Sección |
|---|:--:|---|
| 3.1 Modelo de datos y dominio | /1 | [§1](#1-modelo-de-datos-y-dominio-31) |
| 3.2 Organización en capas | /2 | [§2](#2-organización-en-capas-32) |
| 3.3 Lógica de negocio y manejo de errores | /2 | [§3](#3-lógica-de-negocio-y-manejo-de-errores-33) |
| 3.4 Calidad y legibilidad del código | /1 | [§4](#4-calidad-y-legibilidad-del-código-34) |
| 3.5 Pruebas unitarias y funcionales | /1 | [§5](#5-pruebas-unitarias-y-funcionales-35) |
| 3.6 Autenticación y autorización | /1 | [§6](#6-autenticación-y-autorización-36) |

---

## 1. Modelo de datos y dominio (3.1)

### 1.1 Dos modelos, una frontera explícita

*Database per service*: dos bases PostgreSQL 16 independientes, **sin ninguna clave foránea entre
ellas** ([ADR-003](../01-analisis/adr/ADR-003-dos-microservicios-base-por-servicio.md)).

| Base | Microservicio | Tablas |
|---|---|---|
| `users_db` | `users` | `users`, `audit_log` |
| `matchpoint_db` | `matchpoint` | `courts`, `reservations`, `tournaments`, `teams`, `matches`, `audit_log` |

### 1.2 Entidades y responsabilidades

Cada entidad tiene **una** responsabilidad, enunciable en una línea:

| Entidad | Responsabilidad | Identidad del dueño |
|---|---|---|
| `User` | Asocia el `sub` de Cognito con los datos de contacto de la persona. No guarda contraseñas ni roles. | `cognito_id` (UNIQUE) |
| `Court` | Una cancha reservable, con su sector, piso, parqueo, tarifa y disponibilidad. | `manager_user` |
| `Reservation` | La ocupación de una cancha en una franja horaria concreta. | `owner_user` |
| `Tournament` | Un torneo de eliminación directa con su cupo, estado y campeón. | `manager_user` |
| `Team` | Un equipo inscrito, con sus datos de contacto y sus estadísticas. | `registered_by_user` |
| `Match` | Un enfrentamiento entre dos equipos en una posición del cuadro. | *(hereda la del torneo)* |
| `AuditLog` | Quién hizo qué, cuándo, y qué valores cambiaron. | — |

**Decisión de modelado que conviene explicar antes de que la pregunten:** `courts` **fusiona** el
"complejo deportivo" y la "cancha" del diseño original. Cada cancha lleva su `sector` y su
`has_parking`, que eran atributos del complejo. Se hizo porque en el mercado objetivo —canchas de
barrio— el complejo tiene una sola cancha en la abrumadora mayoría de los casos, y una tabla
intermedia con una fila por complejo habría añadido un `JOIN` a cada consulta a cambio de nada.
Son 5 tablas de dominio, no 6, y es una decisión, no un olvido.

### 1.3 Relaciones y cardinalidades

```
courts      ──1:N──►  reservations       una cancha recibe muchas reservas
courts      ──0..1:N► tournaments        un torneo puede tener sede (opcional)
tournaments ──1:N──►  teams              un torneo inscribe muchos equipos
tournaments ──1:N──►  matches            el cuadro cuelga del torneo
teams       ◄─N:M──►  teams              vía 'matches' (entidad asociativa)
tournaments ──0..1:1► teams              champion_team_id
```

| Relación | Cardinalidad | Cómo se garantiza en el motor |
|---|---|---|
| `courts` → `reservations` | 1:N | FK obligatoria, `ON DELETE CASCADE` |
| `courts` → `tournaments` | 0..1 : N | FK **nullable**, `ON DELETE SET NULL` — la sede es opcional |
| `tournaments` → `teams` | 1:N | FK obligatoria + `UNIQUE (tournament_id, name)` |
| `tournaments` → `matches` | 1:N | FK obligatoria + `UNIQUE (tournament_id, round_number, position_in_round)` |
| **`teams` ↔ `teams`** | **N:M** | **`matches` es la entidad asociativa**: `home_team_id` y `away_team_id`, ambas nullable porque el slot del cuadro existe antes de saber quién lo ocupa |
| `matches` → `teams` (ganador) | N : 0..1 | `CHECK (winner_team_id IS NULL OR = home_team_id OR = away_team_id)` |
| `tournaments` → `teams` (campeón) | 0..1 : 0..1 | FK nullable, añadida después de `teams` por la dependencia circular |
| cualquier tabla → `audit_log` | lógica | **Sin FK**, a propósito: el rastro debe sobrevivir al borrado de la fila auditada |

La relación **N:M resuelta con entidad asociativa** es `matches`: un partido enfrenta a dos
equipos, y un equipo juega varios partidos. No es una tabla puente vacía — lleva ronda, posición,
marcador, estado, fecha y ganador.

### 1.4 Coherencia entre diseño e implementación

Es el punto que la rúbrica pide verificar, y se comprueba en vivo sin creerle a ningún documento:

```bash
docker compose exec matchpoint-db psql -U matchpoint_app -d matchpoint_db -c "\d+ matches"
```

En pgAdmin, clic derecho sobre la base → **ERD For Database** dibuja el diagrama **a partir de las
FK reales** creadas por Hibernate. Ese diagrama y el de [`docs/MODELO-ER.md`](../../docs/MODELO-ER.md)
coinciden porque el segundo se derivó del primero, no al revés.

**Value Objects.** `Team` parte sus atributos en dos bloques `@Embeddable` dentro de la misma
tabla, porque son dos cosas con ciclos de vida distintos:

- `TeamExternalInfo` — nombre y contacto: lo que el equipo declara al inscribirse.
- `TeamInternalStats` — eliminado, partidos jugados/ganados/perdidos, puntos, ronda actual: lo que
  el torneo le va calculando.

Un `PATCH` de datos de contacto no puede tocar las estadísticas, porque son objetos distintos.

---

## 2. Organización en capas (3.2)

### 2.1 El recorrido de una petición

```
Cliente (Postman)
   │  HTTP + JWT
   ▼
[ nginx ]            gateway: único puerto publicado
   │
   ▼
[ SecurityConfig ]   valida el JWT contra el JWKS · cognito:groups → ROLE_*
   │                 sin token → 401 · rol equivocado → 403
   ▼
[ ApiLoggingFilter ] pone el sub en el MDC y escribe event=http.request
   │
   ▼
[ Controller ]       SIN lógica. Recibe el body, lee el claim del JWT y delega.
   │
   ▼
[ Service ]          TODA la lógica de negocio. Valida propiedad. Lanza excepciones propias.
   │
   ▼
[ Repository ]       Spring Data JPA. El service nunca escribe SQL.
   │
   ▼
[ Entity ] ⇄ [ Mapper ] ⇄ [ DTO ]

Si algo falla → [ GlobalExceptionHandler ] traduce la excepción al código HTTP correcto.
```

**Las dependencias apuntan hacia adentro.** El *service* no conoce a `HttpServletRequest`; el
*repository* no conoce DTOs; la entidad no conoce a nadie.

### 2.2 Qué hace y qué NO hace cada capa

| Capa | Paquete | Responsabilidad única | Lo que tiene prohibido |
|---|---|---|---|
| **Controller** | `controllers/` | Mapear ruta y método, leer el claim del JWT, delegar, devolver DTO | Reglas de negocio, acceso a repositorios, `try/catch` de traducción |
| **Service** | `services/` | Reglas de dominio, validaciones, propiedad, orquestación, auditoría | Conocer HTTP: no ve `HttpServletRequest`, no construye `ResponseEntity`, no sabe qué código se devolverá |
| **Repository** | `repositories/` | Persistencia (`JpaRepository`) | Cualquier decisión de negocio |
| **Mapper** | `mappers/` | `entity ⇄ dto`, como `@Component` inyectable | Consultar la base o validar |
| **DTO** | `dto/` | Contrato de la frontera. `data class` inmutables | Ser una entidad JPA |
| **Entity** | `entities/` | Modelo persistente | Salir por la API: **los endpoints solo exponen DTOs** |
| **Exception** | `exceptions/` | Excepciones propias + un único traductor a HTTP | Lanzar `ResponseStatusException` desde el service |

### 2.3 La prueba: un controlador completo

`CourtController` — 40 líneas, **cero lógica de negocio**:

```kotlin
@RestController
@RequestMapping("/courts")
class CourtController(private val courtService: CourtService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody request: CreateCourtRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): CourtResponse = courtService.createCourt(request, jwt.username())

    private fun Jwt.username(): String = getClaimAsString("username")
}
```

Tres cosas que decir sobre estas líneas:

1. **Es una sola expresión.** No hay `if`, no hay `try`, no hay validación. Todo lo que el
   controlador decide es a qué método del *service* llamar.
2. **`jwt.username()` es la frontera de identidad.** El propietario sale del token, no del body.
3. **`CreateCourtRequest` no tiene campo `managerUser`.** No es que se ignore si llega: **no hay
   forma de enviarlo** ([ADR-007](../01-analisis/adr/ADR-007-autorizacion-en-dos-capas.md)).

Y el *service* correspondiente no sabe nada de HTTP:

```kotlin
fun createCourt(request: CreateCourtRequest, managerUser: String): CourtResponse {
    if (request.name.isBlank() || request.sector.isBlank() || request.floorType.isBlank()) {
        throw InvalidCourtException("Name, sector and floor type are required")
    }
    if (request.pricePerHour <= BigDecimal.ZERO) {
        throw InvalidCourtException("Price per hour must be greater than 0")
    }
    val saved = courtRepository.save(courtMapper.toEntity(request, managerUser))
    auditService.record(ENTITY_NAME, saved.id, AuditAction.INSERT, newValues = describe(saved))
    logger.info(logLine("court.created", "Court created", "courtId" to saved.id, …))
    return courtMapper.toResponse(saved)
}
```

Lanza `InvalidCourtException`, no un `400`. **Quién decide que eso es un `400` es el
`GlobalExceptionHandler`**, en un único lugar del sistema.

### 2.4 Verificación mecánica de la separación

Comandos que se pueden ejecutar delante del evaluador; los tres deben devolver **cero resultados**:

```bash
grep -rn "Repository" matchpoint/src/main/kotlin/com/pucetec/matchpoint/controllers/
```
→ ningún controlador inyecta un repositorio.

```bash
grep -rn "HttpServletRequest\|ResponseEntity" matchpoint/src/main/kotlin/com/pucetec/matchpoint/services/
```
→ ningún *service* conoce HTTP.

```bash
grep -rn "entities" matchpoint/src/main/kotlin/com/pucetec/matchpoint/controllers/
```
→ ninguna entidad sale por la API.

---

## 3. Lógica de negocio y manejo de errores (3.3)

### 3.1 Las reglas del dominio viven en el *service*

Catorce reglas de negocio, todas en la capa que les corresponde:

| # | Regla | Dónde | Excepción | HTTP |
|---|---|---|---|:--:|
| RN-01 | Sin reservas solapadas en la misma cancha | `ReservationService` | `CourtNotAvailableException` | 409 |
| RN-02 | Una cancha inactiva no se reserva | `ReservationService` | `CourtNotAvailableException` | 409 |
| RN-03 | No se reserva sin perfil registrado en `users` | `ReservationService` | `ProfileNotRegisteredException` | 409 |
| RN-04 | Cupo del torneo potencia de dos entre 2 y 32 | `TournamentService` | `InvalidTournamentException` | 400 |
| RN-05 | El torneo arranca solo con el cupo exacto | `TournamentService` | `TournamentNotReadyException` | 409 |
| RN-06 | Nombre de equipo único dentro del torneo | `TournamentService` | `DuplicateTeamNameException` | 409 |
| RN-07 | No se inscribe ni se retira con el torneo arrancado | `TournamentService` | `RegistrationClosedException` | 409 |
| RN-08 | Sin empates en eliminación directa | `TournamentService` | `TieNotAllowedException` | 400 |
| RN-09 | Un partido no se puntúa dos veces | `TournamentService` | `MatchAlreadyPlayedException` | 409 |
| RN-10 | Un partido sin los dos equipos no se puntúa | `TournamentService` | `MatchNotReadyException` | 409 |
| RN-11 | Nadie modifica un recurso ajeno | los tres *services* | `NotYour*Exception` | 403 |
| RN-12 | Un rol no ejecuta las operaciones del otro | `SecurityConfig` | *(Spring Security)* | 403 |
| RN-13 | Un usuario de Cognito, un solo perfil | `UserService` | `DuplicateCognitoIdException` | 409 |
| RN-14 | Si `users` cae, `matchpoint` degrada | `UsersClient` | `UsersServiceUnavailableException` | 503 |

### 3.2 Excepciones propias, jerarquía por intención

Las excepciones no están agrupadas por entidad sino **por el código HTTP que significan**, que es
lo que permite que el manejador global sea una tabla y no un árbol de `if`:

| Grupo | Excepciones | HTTP | `event` en el log |
|---|---|:--:|---|
| No encontrado | `CourtNotFound`, `ReservationNotFound`, `TournamentNotFound`, `TeamNotFound`, `MatchNotFound`, `UserNotFound` | 404 | `resource.not_found` |
| Propiedad denegada | `NotYourCourt`, `NotYourReservation`, `NotYourTournament`, `NotYourTeam` | 403 | `ownership.denied` |
| Petición inválida | `InvalidCourt`, `InvalidReservation`, `InvalidTournament`, `InvalidTeam`, `TieNotAllowed`, `BlankName` | 400 | `request.rejected` |
| Conflicto de estado | `TournamentFull`, `DuplicateTeamName`, `RegistrationClosed`, `TournamentNotReady`, `MatchNotReady`, `MatchAlreadyPlayed`, `CourtNotAvailable`, `ProfileNotRegistered`, `DuplicateCognitoId` | 409 | `state.conflict` |
| Dependencia caída | `UsersServiceUnavailable` | 503 | `users.unavailable` |

### 3.3 Un único traductor a HTTP

`GlobalExceptionHandler` es un `@RestControllerAdvice` con **cinco métodos**, uno por código. No
hay un solo `try/catch` de traducción en ningún controlador ni *service*:

```kotlin
@ExceptionHandler(
    TournamentFullException::class, DuplicateTeamNameException::class,
    RegistrationClosedException::class, TournamentNotReadyException::class,
    MatchNotReadyException::class, MatchAlreadyPlayedException::class,
    CourtNotAvailableException::class, ProfileNotRegisteredException::class
)
fun handleConflict(exception: RuntimeException) = respond(HttpStatus.CONFLICT, "state.conflict", exception)

private fun respond(status: HttpStatus, event: String, exception: RuntimeException):
    ResponseEntity<Map<String, String>> {
    val message = exception.message ?: status.reasonPhrase
    logger.warn(logLine(event, message, "status" to status.value()))     // ← toda rechazo deja rastro
    return ResponseEntity.status(status).body(mapOf("error" to message))
}
```

Tres propiedades que se derivan de este diseño:

1. **El formato del error es siempre el mismo**: `{"error": "…"}`. El cliente no tiene que adivinar.
2. **Todo rechazo se registra automáticamente** con su `event` y su `status`. No depende de que
   alguien se acuerde de loguearlo.
3. **Añadir una regla nueva es añadir una excepción a una lista**, no escribir un `catch`.

### 3.4 Respuestas HTTP: la tabla completa

| Código | Significa | Quién lo decide |
|:--:|---|---|
| `200` | Consulta o actualización correcta | Controller |
| `201` | Recurso creado | `@ResponseStatus(CREATED)` |
| `204` | Borrado o cancelación correcta | `@ResponseStatus(NO_CONTENT)` |
| `400` | La petición está mal formada o viola una validación | `GlobalExceptionHandler` |
| `401` | No sé quién eres: sin token, expirado o manipulado | Spring Security |
| `403` **(rol)** | Tu rol no puede hacer esto | Spring Security |
| `403` **(propiedad)** | Tienes el rol… pero el recurso no es tuyo | **el service** |
| `404` | No existe | el service |
| `409` | El estado actual no lo permite | el service |
| `503` | El microservicio `users` no responde | el service |

**Los dos `403` se distinguen en el log**: `event=authz.denied` (Spring Security) contra
`event=ownership.denied` (el *service*). Al depurar se sabe de inmediato cuál de las dos reglas se
aplicó.

---

## 4. Calidad y legibilidad del código (3.4)

### 4.1 Un solo idioma

**Todo el código está en inglés** — endpoints, tablas, columnas, clases, métodos, variables,
nombres de eventos de log y mensajes de error de la API. El español queda **solo** para la
documentación y para los comentarios que explican una decisión. No hay `crearCancha` conviviendo
con `findAll`.

### 4.2 Nombres que dicen lo que hacen

| Nombre | Por qué es bueno |
|---|---|
| `findCourtOwnedBy(id, managerUser)` | Dice qué busca **y bajo qué condición**. `getCourt(id, user)` no diría nada. |
| `advanceWinner(tournament, round, position, winner)` | El vocabulario del dominio, no el de la base de datos. |
| `CourtNotAvailableException` | Nombra la **situación del negocio**, no el código HTTP. |
| `TeamExternalInfo` / `TeamInternalStats` | La distinción conceptual está en el nombre del tipo. |
| `isFree(courtId, startsAt, endsAt)` | Predicado booleano nombrado como predicado. |
| `logLine(event, msg, vararg campos)` | Verbo + qué produce. |

### 4.3 Kotlin idiomático

| Recurso | Dónde | Qué evita |
|---|---|---|
| `data class` para DTOs | `dto/` | `equals`, `hashCode`, `toString` y *getters* escritos a mano |
| Parámetros nombrados y por defecto | `UpdateCourtRequest(pricePerHour = null, active = null)` | Sobrecargas de constructor |
| `?.let { }` para opcionales | `request.pricePerHour?.let { … }` | `if (x != null)` anidados |
| Expresiones `when` exhaustivas | `when (match.status) { PENDING → …; PLAYED → …; READY → Unit }` | Un `else` que oculte un estado nuevo: **el compilador obliga a cubrirlos todos** |
| Funciones de extensión privadas | `private fun Jwt.username()` | Un `JwtUtils` estático |
| Funciones de una sola expresión | `fun getMine(id, user) = mapper.toResponse(findReservationOwnedBy(id, user))` | Cuerpos de tres líneas con un `return` |
| Constructor primario para inyectar | Todos los `@Service` y `@RestController` | `@Autowired` sobre campos; además hace las dependencias inmutables y explícitas |
| `companion object` con constantes | `private companion object { const val ENTITY_NAME = "courts" }` | Cadenas mágicas repetidas |

### 4.4 Duplicación: qué se abstrajo y qué no

**Se abstrajo lo que se repetía sin variación:**

- `logLine(...)` — el formato del log estaría copiado en 40 lugares.
- `AuditService.record(...)` — la escritura de auditoría, idéntica en todas las mutaciones.
- `GlobalExceptionHandler.respond(...)` — la construcción de la respuesta de error.
- `CourtMapper.toResponseList(courts) = courts.map { toResponse(it) }` — el mapeo de listas.

**No se abstrajo lo que se parece pero no es lo mismo:** los tres `find*OwnedBy` comparten
estructura (cargar, comparar, lanzar) pero cada uno lanza su propia excepción con su propio
mensaje. Un `findOwnedBy<T>` genérico habría producido *"Resource 4 does not belong to you"* en
lugar de *"Court 4 does not belong to you"*. **Se prefirió la repetición de tres líneas al mensaje
impersonal**, y es una decisión consciente, no un descuido.

### 4.5 Buenas prácticas de Spring Boot

- **Inyección por constructor**, siempre. Ningún `@Autowired` sobre un campo.
- **`@RestControllerAdvice`** para el manejo de errores, en vez de `try/catch` en controladores.
- **`@ConfigurationProperties` / `@Value` con variables de entorno** para toda configuración: no
  hay ni una URL, ni un puerto, ni una credencial escrita en el código.
- **Perfiles separados**: la base H2 de pruebas se declara **solo** en `src/test/resources`, así
  que la configuración de producción no sabe que existe.
- **Actuator** con `/actuator/health` expuesto y usado como `healthcheck` real de Docker.
- **Sin `@Transactional` decorativo**: se usa donde una operación toca varias tablas y debe ser
  atómica, no como anotación por defecto sobre toda clase.

### 4.6 Comentarios

La regla fue: **el código dice qué hace; el comentario dice por qué**. Por eso hay muy pocos, y los
que hay explican una decisión que el código no puede expresar — por ejemplo, por qué `owner_name`
es una copia deliberada y no una consulta en caliente, o por qué `audit_log` no tiene FK. El resto
del "por qué" está en los ADR, que es donde se puede escribir con espacio.

---

## 5. Pruebas unitarias y funcionales (3.5)

Desarrollado en detalle en
[`entrega/01-analisis/PRUEBAS-UNITARIAS.md`](../01-analisis/PRUEBAS-UNITARIAS.md). Resumen:

| | `users` | `matchpoint` |
|---|---:|---:|
| Pruebas | 57 | 157 |
| Cobertura de líneas (código propio) | 100 % | 100 % |
| Umbral atado al build | Sí | Sí |

- **Unitarias de dominio** con `mockito-kotlin`, sin contexto de Spring: reglas de negocio,
  cálculo del cuadro, avance del ganador, enmascaramiento.
- **Funcionales de endpoint** con `@WebMvcTest` + `MockMvc` + `spring-security-test`: rutas,
  códigos HTTP, serialización y **reglas de autorización reales**, incluidos `401` sin token y
  `403` con el rol equivocado en los dos microservicios.
- **Mocks y stubs**: repositorios, `UsersClient`, `AuditService` y `JwtDecoder` sustituidos. Las
  pruebas **no necesitan AWS ni PostgreSQL**.
- **Aserciones significativas**: sobre el valor concreto, sobre el tipo exacto de la excepción y
  sobre el efecto colateral (`verify(repository, never()).save(any())` en los casos de rechazo).
- **Regla del par**: cada regla de negocio tiene su prueba de aceptación y su prueba de rechazo.

```bash
cd matchpoint && ./gradlew check     # 157 pruebas + umbral de cobertura
cd users      && ./gradlew check     #  57 pruebas + umbral de cobertura
```

---

## 6. Autenticación y autorización (3.6)

### 6.1 Autenticación: JWT de AWS Cognito

Cada microservicio es un **OAuth2 Resource Server**. Descarga el JWKS del User Pool y valida
**firma, emisor y expiración** de cada token. No hay JWT propio, ni sesión, ni Basic Auth, ni
usuarios en memoria ([ADR-002](../01-analisis/adr/ADR-002-cognito-como-proveedor-de-identidad.md)).

Los dos servicios arman el `issuer-uri` con **las mismas dos variables de entorno**, así que un
token que uno acepta el otro también — que es lo que permite propagarlo entre servicios.

```kotlin
// cognito:groups → autoridad de Spring Security
groups.map { SimpleGrantedAuthority("ROLE_${it.uppercase()}") }   // MANAGER → ROLE_MANAGER
```

### 6.2 Endpoints públicos y privados, bien delimitados

**Públicos** (`permitAll`, sin token) — el escaparate del producto:

| Método | Ruta |
|---|---|
| `GET` | `/matchpoint/courts`, `/matchpoint/courts/{id}`, `/matchpoint/courts/available` |
| `GET` | `/matchpoint/tournaments`, `/matchpoint/tournaments/{id}` |
| `GET` | `/matchpoint/tournaments/{id}/teams`, `.../teams/{teamId}`, `.../matches` |
| `GET` | `/matchpoint/matches/{matchId}` |
| `GET` | `/actuator/health` |

**Privados** — todo lo demás, con `anyRequest().authenticated()` como red de seguridad final: un
endpoint nuevo que nadie declare queda **protegido por defecto**, no abierto por defecto.

| Rol | Operaciones |
|---|---|
| **MANAGER** | Crear/editar **sus** canchas · crear torneos, arrancar el cuadro, programar y puntuar partidos de **sus** torneos · listar, ver y borrar perfiles |
| **PLAYER** | Reservar canchas, ver/cancelar **sus** reservas · inscribir y retirar equipos · ver y editar **su** perfil |

### 6.3 Restricción de acciones: dos capas de 403

| Capa | Quién decide | Pregunta que responde |
|---|---|---|
| **1 · Rol** | `SecurityFilterChain` | "¿Tu rol puede ejecutar esta operación?" |
| **2 · Propiedad** | El *service* | "Tu rol te deja… pero, ¿el recurso es tuyo?" |

Y la regla que cierra la puerta a la suplantación:

> El propietario sale del claim `username` del token. **Los DTO de request ni siquiera tienen ese
> campo.**

No es una validación que se pueda olvidar en un endpoint nuevo: es que no existe forma de enviarlo
([ADR-007](../01-analisis/adr/ADR-007-autorizacion-en-dos-capas.md)).

### 6.4 Demostración en vivo, en cuatro peticiones

Cuatro llamadas que cubren el sub-criterio completo. Todas están en la colección de Postman con su
aserción `pm.test`:

| # | Petición | Resultado | Qué demuestra |
|---|---|:--:|---|
| 1 | `GET /matchpoint/reservations/me` **sin token** | `401` | Sin identidad no se pasa |
| 2 | Mismo endpoint con **token alterado en un carácter** | `401` | La firma se valida de verdad contra el JWKS |
| 3 | `POST /matchpoint/courts` con token de **PLAYER** | `403` | Restricción por rol, `event=authz.denied` |
| 4 | `PATCH /matchpoint/courts/4` con token de `manager_josue` (la cancha es de `manager_ana`) | `403` | Restricción por propiedad, `event=ownership.denied` |

La cuarta es la que cuenta: **el rol es el correcto y aun así no puede.** Y el `event` distinto en
el log prueba que las dos capas son mecanismos distintos, no la misma regla contada dos veces.

---

## 7. Resumen de evidencia

| Sub-criterio | Evidencia principal | Cómo se verifica en vivo |
|---|---|---|
| 3.1 Modelo | 5 tablas de dominio + auditoría, N:M con entidad asociativa, `docs/MODELO-ER.md` | pgAdmin → *ERD For Database* |
| 3.2 Capas | `controllers/` sin lógica · `services/` sin HTTP · DTOs en la frontera | Los tres `grep` de §2.4, que devuelven cero |
| 3.3 Negocio y errores | 14 reglas en los *services* · 25 excepciones propias · un `@RestControllerAdvice` | Disparar cada código con la colección de Postman |
| 3.4 Calidad | Todo en inglés · Kotlin idiomático · inyección por constructor · sin duplicación evidente | Lectura de `CourtController` y `CourtService` |
| 3.5 Pruebas | 214 pruebas · 100 % de líneas · umbral atado al build | `./gradlew check` en los dos servicios |
| 3.6 Seguridad | Cognito + JWKS · públicos/privados delimitados · dos capas de 403 | Las cuatro peticiones de §6.4 |
