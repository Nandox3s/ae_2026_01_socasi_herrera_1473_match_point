# MatchPoint · Estrategia y aplicación de pruebas unitarias

**Criterio 1.3 de la rúbrica** — aplicación adecuada de pruebas unitarias.
**Criterio 3.5** — cobertura de la lógica relevante con casos válidos e inválidos, uso correcto de
mocks y aserciones significativas.

---

## 1. Resumen ejecutable

```bash
cd users      && ./gradlew check     # 57 pruebas + umbral de cobertura
cd matchpoint && ./gradlew check     # 157 pruebas + umbral de cobertura
```

| | `users` | `matchpoint` | Total |
|---|---:|---:|---:|
| Archivos de prueba | 8 | 13 | **21** |
| Métodos `@Test` | 57 | 157 | **214** |
| Cobertura de líneas (JaCoCo, código propio) | 100 % | 100 % | **100 %** |
| Cobertura del IDE (todo, incluidas las clases excluidas) | 99.3 % | 99.8 % | — |

`./gradlew check` **falla el build** si la cobertura de líneas baja del 100 %. No es un número
declarado en un documento: es una condición que el proyecto se impone a sí mismo.

---

## 2. Qué se prueba y con qué nivel

La pirámide está deliberadamente cargada abajo: la mayor parte son pruebas unitarias de *service*
con dependencias simuladas, y encima una capa de pruebas de endpoint con `@WebMvcTest`.

| Nivel | Técnica | Qué verifica | Archivos |
|---|---|---|---|
| **Unitario de dominio** | JUnit 5 + `mockito-kotlin`, sin contexto de Spring | Reglas de negocio, validaciones, cálculo del cuadro, avance del ganador, enmascaramiento | `CourtServiceTest`, `ReservationServiceTest`, `TournamentServiceTest`, `UserServiceTest`, `AuditServiceTest`, `LogEventsTest` |
| **Unitario de infraestructura** | JUnit 5 + dobles de prueba | Traducción de claims a roles, filtro de logging, manejadores de 401/403, cliente HTTP saliente | `CognitoGroupsConverterTest`, `ApiLoggingFilterTest`, `LoggingSecurityHandlersTest`, `UsersClientTest` |
| **Integración de la capa web** | `@WebMvcTest` + `MockMvc` + `spring-security-test` | Rutas, códigos HTTP, serialización de DTO y **reglas de autorización reales** | `BookingControllerTest`, `TournamentControllerTest`, `MeControllerTest`, `UserControllerTest` |
| **Arranque del contexto** | `@SpringBootTest` | Que el contexto levante con toda la configuración conectada | `MatchpointApplicationTests`, `UsersApplicationTests` |

### Distribución real

**`matchpoint` — 157 pruebas**

| Archivo | Pruebas | Foco |
|---|---:|---|
| `TournamentServiceTest` | 46 | Cuadro, avance, estadísticas, campeón, todas las validaciones |
| `TournamentControllerTest` | 22 | Endpoints de torneos, equipos y partidos con roles reales |
| `BookingControllerTest` | 20 | Endpoints de canchas y reservas, incluidos `401`/`403` |
| `CourtServiceTest` | 19 | Filtros, disponibilidad, solapamiento, propiedad |
| `ReservationServiceTest` | 12 | Reserva, integración con `users`, cancelación |
| `LogEventsTest` | 9 | Formato de log, comillas, `|` prohibido, enmascaramiento |
| `UsersClientTest` | 7 | Propagación del token, *timeout*, `503`, perfil ausente |
| `AuditServiceTest` | 5 | Quién/qué/cuándo, valores antes y después |
| `LoggingSecurityHandlersTest` | 5 | `auth.rejected` y `authz.denied` |
| `ApiLoggingFilterTest` | 4 | Línea de entrada, de salida y `sub` en el MDC |
| `CognitoGroupsConverterTest` | 4 | `cognito:groups` → `ROLE_*`, claim ausente |
| `MeControllerTest` | 3 | Identidad efectiva y `503` del vecino |
| `MatchpointApplicationTests` | 1 | Carga del contexto |

**`users` — 57 pruebas**

| Archivo | Pruebas | Foco |
|---|---:|---|
| `UserControllerTest` | 16 | Endpoints de perfil, `401`, `403` de PLAYER contra rutas de MANAGER |
| `UserServiceTest` | 13 | Alta, duplicado, actualización, baja, nombre en blanco |
| `LogEventsTest` | 9 | Mismo estándar de logging que el otro servicio |
| `AuditServiceTest` | 5 | Auditoría de perfiles |
| `LoggingSecurityHandlersTest` | 5 | 401 y 403 registrados |
| `ApiLoggingFilterTest` | 4 | Trazabilidad de toda petición |
| `CognitoGroupsConverterTest` | 4 | Traducción de grupos |
| `UsersApplicationTests` | 1 | Carga del contexto |

---

## 3. Casos válidos e inválidos: la regla del par

Ninguna regla de negocio se prueba solo por su camino feliz. **Cada validación tiene su prueba de
aceptación y su prueba de rechazo**, y la de rechazo asegura además que la excepción es la
correcta, no una cualquiera.

| Regla | Caso válido | Caso inválido |
|---|---|---|
| RN-01 solapamiento | Franja libre → reserva creada | Franja ocupada → `CourtNotAvailableException` |
| RN-02 cancha inactiva | Cancha activa → reserva creada | `active = false` → `CourtNotAvailableException` |
| RN-03 perfil obligatorio | Perfil existente → reserva creada | Sin perfil → `ProfileNotRegisteredException` |
| RN-04 cupo potencia de 2 | `maxTeams = 4` → torneo creado | `maxTeams = 6` → `InvalidTournamentException` |
| RN-05 cupo exacto | 4 de 4 equipos → cuadro generado | 3 de 4 → `TournamentNotReadyException` |
| RN-06 nombre de equipo | Nombre nuevo → equipo inscrito | Nombre repetido → `DuplicateTeamNameException` |
| RN-08 sin empates | `50-48` → ganador y avance | `50-50` → `TieNotAllowedException` |
| RN-09 doble marcador | Partido `READY` → puntuado | Partido `PLAYED` → `MatchAlreadyPlayedException` |
| RN-11 propiedad | Recurso propio → operación aplicada | Recurso ajeno → `NotYour*Exception` |
| RN-13 perfil único | `sub` nuevo → perfil creado | `sub` repetido → `DuplicateCognitoIdException` |
| RN-14 vecino caído | `users` responde → reserva creada | `users` no responde → `UsersServiceUnavailableException` |

---

## 4. Uso de mocks y stubs

**Los tests no necesitan AWS ni PostgreSQL.** Es requisito, no casualidad: una prueba que depende
de un servicio externo deja de ser una prueba unitaria y se vuelve un indicador del clima.

| Dependencia real | Sustituto en pruebas | Por qué |
|---|---|---|
| Repositorios JPA | `mock<CourtRepository>()` etc. (`mockito-kotlin`) | El *service* se prueba aislado; se verifica **qué** guarda, no cómo persiste |
| Microservicio `users` (HTTP) | `mock<UsersClient>()` en los tests de *service*; respuestas simuladas en `UsersClientTest` | Permite probar el `409` de perfil ausente y el `503` de vecino caído sin apagar contenedores |
| AWS Cognito (JWKS) | `JwtDecoder` sustituido por un mock; los JWT se fabrican con `jwt()` de `spring-security-test` | Se prueban roles y claims sin red y sin secretos |
| PostgreSQL | H2 en memoria, declarada **solo** en `src/test/resources` | Únicamente para las pruebas que levantan contexto; la configuración de producción no cambia |
| `AuditService` | Mock verificado con `verify(...)` | Se comprueba que **toda mutación audita**, sin escribir en base |

Ejemplo del patrón que se repite: se prepara el doble, se ejecuta, y se **verifica tanto el
resultado como la interacción**.

```kotlin
@Test
fun `rejects a reservation when the slot overlaps an existing one`() {
    whenever(usersClient.fetchCurrentProfile()).thenReturn(profile)
    whenever(courtRepository.findById(1L)).thenReturn(Optional.of(activeCourt))
    whenever(reservationRepository.findByCourtIdAndStatus(1L, CONFIRMED))
        .thenReturn(listOf(existingReservation))

    assertThrows<CourtNotAvailableException> {
        service.createReservation(overlappingRequest, "player_fernando")
    }

    verify(reservationRepository, never()).save(any())   // no se guardó nada
    verify(auditService, never()).record(any(), any(), any(), any(), any())
}
```

Las dos líneas de `verify(..., never())` son la parte que de verdad importa: comprueban que el
rechazo **no dejó efectos secundarios**. Una prueba que solo verifica que "lanzó la excepción"
pasaría igual aunque el sistema hubiera guardado la reserva antes de fallar.

---

## 5. Aserciones significativas

Tres criterios que se aplicaron en todo el conjunto:

1. **Se afirma sobre el valor, no sobre el tipo.** No basta `assertNotNull(response)`: se comprueba
   el campo concreto que la regla debía producir (`assertEquals(TournamentStatus.FINISHED, …)`,
   `assertEquals("Tigres", progress.champion?.name)`).
2. **Se afirma sobre la excepción exacta.** `assertThrows<DuplicateTeamNameException>`, nunca
   `assertThrows<RuntimeException>`, porque el tipo de excepción es lo que determina el código HTTP
   que verá el usuario.
3. **Se afirma sobre el efecto colateral.** `verify(repository).save(captor.capture())` seguido de
   aserciones sobre el objeto capturado: así se comprueba, por ejemplo, que el `ownerUser` guardado
   viene **del token** y no del body.

En la capa web, las pruebas afirman sobre el contrato completo:

```kotlin
mockMvc.perform(post("/reservations").with(jwt().authorities(ROLE_MANAGER)) …)
    .andExpect(status().isForbidden)        // un MANAGER no reserva
```

---

## 6. Pruebas de autorización: el conjunto que suele faltar

En los dos microservicios hay pruebas que verifican explícitamente que un endpoint protegido:

- **rechaza la petición sin token** → `401`;
- **rechaza la petición con el rol equivocado** → `403`;
- **rechaza la petición sobre un recurso ajeno** → `403`, esta vez lanzado por el *service*.

Es lo que evita el error clásico de un backend con 100 % de cobertura y todos los endpoints
abiertos: la cobertura mide líneas ejecutadas, no reglas de acceso respetadas.

---

## 7. Cobertura: dos números, los dos correctos

| Medición | `users` | `matchpoint` | Qué cuenta |
|---|---:|---:|---|
| JaCoCo (`./gradlew check`) | **100 %** | **100 %** | Solo el código propio, con las exclusiones declaradas |
| Coverage del IDE (IntelliJ) | 99.3 % | 99.8 % | **Todo**, incluidas las clases excluidas |

Lo que se excluye del cómputo, declarado en `build.gradle.kts` y permitido por la rúbrica:

```kotlin
val coverageExclusions = listOf(
    "com/pucetec/matchpoint/MatchpointApplication*",   // el main()
    "com/pucetec/matchpoint/config/**",                // configuración, sin lógica
    "com/pucetec/matchpoint/dto/**",                   // data classes de frontera
    "com/pucetec/matchpoint/entities/**",              // entidades sin comportamiento
    "com/pucetec/matchpoint/enums/**",
    "com/pucetec/matchpoint/audit/AuditLog*",
    "com/pucetec/matchpoint/clients/UserProfile*",
)
```

Todo lo demás —controladores, *services*, *mappers*, filtros de logging, cliente HTTP, auditoría y
manejo de excepciones— está al **100 % de líneas**. Lo único que el IDE marca sin cubrir es el
`main()` de las clases `Application` y los constructores sintéticos de las entidades: exactamente
las dos categorías que la rúbrica permite dejar fuera.

El umbral está atado al build, no a la buena voluntad:

```kotlin
tasks.jacocoTestCoverageVerification {
    violationRules { rule { limit { counter = "LINE"; minimum = "1.00".toBigDecimal() } } }
}
tasks.check { dependsOn(tasks.jacocoTestCoverageVerification) }
```

Reporte HTML tras ejecutar las pruebas:
`build/reports/jacoco/test/html/index.html` en cada servicio.
Capturas del coverage del IDE: [`docs/coverage/`](../../docs/coverage).

---

## 8. Cómo demostrarlo en la sustentación

```bash
cd matchpoint && ./gradlew check
```

1. Se ven las 157 pruebas en verde y el build pasa el umbral de cobertura.
2. Abrir `build/reports/jacoco/test/html/index.html` y entrar a `services` — todo en verde.
3. Romper una regla a propósito para demostrar que la red funciona: comentar la validación de
   empates en `TournamentService.registerScore` y volver a ejecutar. Falla la prueba
   `rejects a tie in a single elimination bracket`. Deshacer con `git checkout`.

El paso 3 es el que convence: demuestra que las pruebas **detectan** la regresión, no solo que
existen.
