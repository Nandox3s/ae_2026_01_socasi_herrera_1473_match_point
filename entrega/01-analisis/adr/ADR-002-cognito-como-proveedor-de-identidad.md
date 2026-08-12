# ADR-002 · Delegar autenticación y roles en AWS Cognito

- **Estado:** Aceptado
- **Fecha:** 2026-08-01
- **Decide:** Fernando Socasi
- **Requerimientos afectados:** RF-08, RF-09 · RNF-01, RNF-13
- **Historia:** HU-04

## Contexto

El sistema necesita saber quién hace cada petición y con qué rol. Las opciones iban desde
implementar el registro y el login en casa hasta delegarlo por completo en un proveedor externo.

Implementar identidad propia significa asumir: almacenamiento de contraseñas con hash y *salt*,
política de contraseñas, confirmación por correo, recuperación de cuenta, rotación de claves de
firma, revocación de tokens y bloqueo por intentos fallidos. Es un producto en sí mismo, y cada
una de esas piezas es una oportunidad de introducir una vulnerabilidad real en un proyecto
académico donde nadie hará una auditoría de seguridad.

Además, el sistema tiene **dos** microservicios. Cualquier solución propia obliga a resolver cómo
los dos confían en la misma identidad sin compartir base de datos.

## Decisión

**La identidad se delega por completo a un User Pool de AWS Cognito.** Los dos microservicios son
*OAuth2 Resource Servers*: no emiten tokens, solo los validan contra el JWKS del emisor.

- Los usuarios y sus grupos (`MANAGER`, `PLAYER`) viven en el User Pool, nunca en la base de datos.
- El backend no almacena contraseñas ni tiene endpoint de login.
- El rol llega en el claim `cognito:groups` y se traduce a autoridad de Spring Security.
- Los dos servicios arman el `issuer-uri` con **las mismas dos variables de entorno**
  (`COGNITO_REGION`, `COGNITO_USER_POOL_ID`), así que un token que uno acepta el otro también.

## Alternativas consideradas

| Alternativa | Por qué se descartó |
|---|---|
| **JWT propio** con tabla de usuarios y endpoint `/login` | Obliga a gestionar hashing, expiración, rotación de la clave de firma y revocación. Cada pieza es una superficie de ataque que el equipo no puede auditar. Y con dos microservicios, obliga a compartir el secreto de firma entre ambos. |
| **Spring Security con usuarios en memoria** | Suficiente para una demo, inservible como argumento arquitectónico. La rúbrica pide autenticación y autorización reales, y no resuelve nada del problema de los dos servicios. |
| **Keycloak autoalojado** | Técnicamente equivalente y con la ventaja de ser portable entre nubes, pero añade un contenedor más, su propia base de datos y su propia configuración de realms. Para dos roles y dos usuarios de demo, el costo operativo no se justificaba. Cognito además es gestionado: sin parches, sin respaldos, sin disponibilidad que mantener. |
| **Auth0 / Firebase Auth** | Funcionalmente comparables. Se prefirió Cognito porque el resto de la arquitectura objetivo es AWS (ver [ADR-011](ADR-011-despliegue-local-y-arquitectura-objetivo.md)) y mantener un solo proveedor simplifica IAM, facturación y el discurso de la sustentación. |

## Consecuencias

**A favor**

- Cero contraseñas en el sistema. La pregunta "¿cómo almacenan las claves?" tiene una respuesta de
  una línea: no las almacenamos.
- Los dos microservicios confían en la misma identidad **sin compartir nada**: cada uno descarga el
  JWKS por su cuenta y valida por su cuenta. No hay secreto compartido entre servicios.
- El token es autocontenido: `matchpoint` puede propagarlo a `users` y este aplica sus propias
  reglas sin ninguna coordinación previa (ver [ADR-004](ADR-004-comunicacion-http-con-token-propagado.md)).
- Los roles se administran desde la consola de AWS sin desplegar código.

**En contra**

- **Dependencia de un servicio externo.** Sin conexión a internet no se obtiene un token. Se
  mitigó en las pruebas sustituyendo el `JwtDecoder` por un mock, de modo que las 214 pruebas
  corren sin red y sin AWS.
- **La demo requiere preparación previa**: los usuarios deben existir y estar `CONFIRMED` en el
  User Pool, y no en estado `FORCE_CHANGE_PASSWORD`. Está documentado como *checklist* en
  `docs/DEMO.md`.
- **El App Client tiene *client secret***, así que el login son dos llamadas (`InitiateAuth` +
  `RespondToAuthChallenge`) firmadas con `SECRET_HASH = Base64(HMAC-SHA256(username + clientId, clientSecret))`.
  Es más laborioso que un login simple, pero lo resuelve la carpeta `0` de la colección de Postman.
- **Cambiar de proveedor no es gratis**, aunque el costo es acotado: al ser OIDC estándar, el
  cambio se limita al `issuer-uri` y al conversor de claims a roles.

## Puesta en práctica

| Dónde | Qué |
|---|---|
| `application.yaml` (ambos) | `spring.security.oauth2.resourceserver.jwt.issuer-uri` construido con las dos variables de entorno |
| `config/SecurityConfig.kt` (ambos) | `oauth2ResourceServer { jwt { … } }`; no existe ningún `formLogin` ni `httpBasic` |
| `config/CognitoGroupsConverter.kt` (ambos) | `groups.map { SimpleGrantedAuthority("ROLE_${it.uppercase()}") }` |
| `scripts/cognito-token.sh` | Obtiene un token desde la terminal pidiendo el secreto por teclado, sin eco y sin guardarlo |
| `.gitignore` | El *client secret* **no está en el repositorio**: ni en `.env` ni en el código. Se copia a mano al environment de Postman |

Verificación en vivo: alterar un carácter del token y repetir la petición. El sistema responde
`401` con `event=auth.rejected` — la firma no valida contra el JWKS.
