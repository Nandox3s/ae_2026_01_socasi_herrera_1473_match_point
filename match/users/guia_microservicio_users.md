# Guía — Microservicio `users`

> Nuestro primer microservicio. **Una sola responsabilidad:** asociar el
> `cognitoId` de un usuario (el `sub` de su token de Cognito) con los **datos
> propios** que guardamos en nuestra base de datos (nombre, email, teléfono).

---

## 1. ¿Por qué un microservicio de usuarios?

Cognito ya sabe **autenticar** (email, contraseña, MFA…) y nos entrega un
**token** que identifica al usuario con un `sub` (un id único como
`a1b2c3d4-...`). Pero Cognito **no** guarda la información de negocio de tu app
(carrera, teléfono, dirección, rol interno, etc.).

Ahí entra este micro:

```
Cognito (autenticación)          Microservicio users (datos propios)
┌─────────────────────┐          ┌──────────────────────────────────┐
│ sub: a1b2c3d4-...    │  ─────►  │ cognitoId: a1b2c3d4-...           │
│ email, password, MFA │          │ name, email, phone (NUESTROS)    │
└─────────────────────┘          └──────────────────────────────────┘
```

La relación es **1 a 1**: un usuario de Cognito ↔ un perfil en nuestro micro.
Por eso `cognitoId` es **único** en la tabla.

---

## 2. Arquitectura por capas (lo de siempre)

```
Controller  →  Service  →  Repository  →  Base de datos (H2)
   ▲              ▲            ▲
  DTOs          lógica     JpaRepository
  Mapper        + reglas
```

| Capa | Archivo | Responsabilidad |
|---|---|---|
| Entity | `entities/User.kt` | Cómo se guarda en la BD (`cognitoId` único + datos) |
| DTO | `dto/UserDto.kt` | Qué entra (`UserRequest`) y qué sale (`UserResponse`) |
| Mapper | `mappers/UserMapper.kt` | Convierte request ↔ entity ↔ response |
| Repository | `repositories/UserRepository.kt` | Acceso a datos (`findByCognitoId`) |
| Service | `services/UserService.kt` | Lógica de negocio y validaciones |
| Controller | `controllers/UserController.kt` | Endpoints HTTP |
| Config | `config/SecurityConfig.kt` | Validación del token (resource server) |
| Exceptions | `exceptions/*` | Errores + `GlobalExceptionHandler` |

**Novedad respecto a `students`:** aquí el micro es un **Resource Server**: cada
petición a `/api/**` debe traer un token válido de Cognito, o responde `401`.

---

## 3. El concepto clave: el `cognitoId` sale del **token**, no del cliente

Mira los endpoints `/me` en el controller:

```kotlin
@GetMapping("/api/users/me")
fun getMyProfile(
    @AuthenticationPrincipal jwt: Jwt   // <-- Spring ya validó el token
): UserResponse {
    val cognitoId = jwt.subject          // <-- el "sub" = el cognitoId
    return userService.getUserByCognitoId(cognitoId)
}
```

- El cliente **no** manda su `cognitoId`. Sería inseguro (podría mentir).
- Spring valida la firma del token contra Cognito y nos da los *claims*.
- `jwt.subject` es el `sub`. Así el usuario **solo** puede tocar su propio perfil.

Esto es la diferencia entre **autenticación** (¿quién eres? → el token lo prueba)
y **autorización** (¿qué puedes hacer? → solo tu propio perfil).

---

## 4. Endpoints

| Método | Ruta | ¿De dónde sale el cognitoId? | Uso |
|---|---|---|---|
| `POST` | `/api/users/me` | del token (`sub`) | Registrar **mi** perfil |
| `GET` | `/api/users/me` | del token (`sub`) | Ver **mi** perfil |
| `PUT` | `/api/users/me` | del token (`sub`) | Actualizar **mi** perfil |
| `GET` | `/api/users/cognito/{cognitoId}` | del path | Que **otro micro** resuelva un sub |
| `GET` | `/api/users` | — | Listar todos |
| `GET` | `/api/users/{id}` | — | Buscar por id |
| `DELETE` | `/api/users/{id}` | — | Eliminar |

### Ejemplo `POST /api/users/me`

Request (body):
```json
{
  "name": "Ana Lopez",
  "email": "ana@puce.edu",
  "phone": "0999999999"
}
```

Response `200`:
```json
{
  "id": 1,
  "cognitoId": "a1b2c3d4-...",
  "name": "Ana Lopez",
  "email": "ana@puce.edu",
  "phone": "0999999999"
}
```

---

## 5. Manejo de errores

| Situación | Excepción | HTTP |
|---|---|---|
| Nombre vacío | `BlankNameException` | `400 Bad Request` |
| No existe perfil para ese usuario/id | `UserNotFoundException` | `404 Not Found` |
| El usuario ya tiene perfil | `DuplicateCognitoIdException` | `409 Conflict` |
| Sin token / token inválido | (Spring Security) | `401 Unauthorized` |

El `GlobalExceptionHandler` traduce cada excepción a su código y a un JSON:
```json
{ "message": "...", "source": "UserService" }
```

---

## 6. Cómo levantar y probar

### Levantar
```bash
cd users
./gradlew bootRun
```
- API en `http://localhost:8686`
- Consola H2 en `http://localhost:8686/h2-console`
  (JDBC URL: `jdbc:h2:mem:usersdb`, user `admin`, pass `admin`)

### Conseguir un token de Cognito
Usa el flujo de la Hosted UI + `curl` (ver `demo_auth/auth.md`, sección 🔑).
**Usa el `access_token`, no el `id_token`.**

### Probar con Postman
1. Importa `users.postman_collection.json`.
2. En la colección → *Variables*, pega tu token en `{{token}}`.
3. Corre las peticiones en orden (01 crear → 02 ver → …).
4. La petición **08** va sin token: verás el `401` (demuestra la autenticación).

### Probar con curl
```bash
TOKEN="pega_tu_access_token"

# crear mi perfil
curl -X POST localhost:8686/api/users/me \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Ana Lopez","email":"ana@puce.edu","phone":"0999999999"}'

# ver mi perfil
curl localhost:8686/api/users/me -H "Authorization: Bearer $TOKEN"

# sin token -> 401
curl -i localhost:8686/api/users/me
```

---

## 7. Pruebas

```bash
./gradlew test
```
- `UserServiceTest`: 8 pruebas unitarias (Mockito, sin BD ni red) que cubren
  crear, duplicado, nombre vacío, buscar por cognitoId, actualizar y eliminar.
- `UsersApplicationTests`: verifica que el contexto de Spring levanta. Mockea el
  `JwtDecoder` para no salir a la red a Cognito durante el build.

---

## 8. Para pensar / tarea

1. ¿Qué pasa si dos usuarios distintos intentan registrar el mismo `cognitoId`?
   ¿Qué código HTTP devuelve y por qué?
2. ¿Por qué es más seguro leer el `cognitoId` del token que recibirlo en el body?
3. Ahora mismo cualquier usuario autenticado puede llamar `GET /api/users`
   (listar todos). ¿Cómo restringirías eso solo a un rol `admin`? (Pista:
   *claims* de grupos de Cognito + `hasAuthority`.)
