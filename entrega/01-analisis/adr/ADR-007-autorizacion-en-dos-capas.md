# ADR-007 · Autorización en dos capas: rol y propiedad del recurso

- **Estado:** Aceptado
- **Fecha:** 2026-08-02
- **Decide:** Fernando Socasi, Josué Herrera
- **Requerimientos afectados:** RF-09, RF-10 · RN-11, RN-12
- **Historias:** HU-03, HU-05, HU-07, HU-11

## Contexto

Tener el rol correcto no basta. Un MANAGER puede editar canchas — pero **no las de otro MANAGER**.
Un PLAYER puede cancelar reservas — pero **no las de otro PLAYER**.

Esta segunda condición no se puede expresar en la configuración de rutas de Spring Security,
porque depende del **dato**: hay que cargar el recurso para saber de quién es. Y si se resuelve mal,
produce el fallo de seguridad más común en aplicaciones web: *Broken Object Level Authorization*
— el endpoint valida el rol, ignora la pertenencia, y cualquiera con el rol correcto edita los
recursos de todos.

Además había que decidir **de dónde sale la identidad del propietario**. Si el cliente la envía en
el body, cualquiera puede escribir el nombre de otro.

## Decisión

**Dos capas de `403`, en dos lugares distintos y por dos motivos distintos.**

| Capa | Quién decide | Qué comprueba | Ejemplo |
|---|---|---|---|
| **1 · Por rol** | `SecurityFilterChain` | "¿Tu rol puede ejecutar esta operación?" | Un PLAYER hace `POST /courts` → `403` |
| **2 · Por propiedad** | El *service* | "Tu rol te deja… pero, ¿el recurso es tuyo?" | Un MANAGER hace `PATCH /courts/4`, que es de otro → `403` |

Y una regla que cierra el problema de raíz:

> **El propietario sale siempre del claim `username` del token. Los DTO de request ni siquiera
> tienen ese campo.**

No es una validación que se pueda olvidar en un endpoint nuevo: es que **no existe forma de
enviarlo**. `CreateCourtRequest` no tiene `managerUser`, `CreateReservationRequest` no tiene
`ownerUser`, `RegisterTeamRequest` no tiene `registeredByUser`. El controlador lee el claim y lo
pasa al *service* como parámetro aparte.

La capa 2 se concentra en tres métodos privados con el mismo patrón —cargar, comparar, lanzar—
para que no haya diez implementaciones distintas de la misma idea:

```kotlin
private fun findCourtOwnedBy(id: Long, managerUser: String): Court {
    val court = courtRepository.findById(id)
        .orElseThrow { CourtNotFoundException("Court $id was not found") }
    if (court.managerUser != managerUser) {
        throw NotYourCourtException("Court $id does not belong to you")
    }
    return court
}
```

## Alternativas consideradas

| Alternativa | Por qué se descartó |
|---|---|
| **Solo autorización por rol** | Es el agujero de seguridad descrito arriba. Un MANAGER podría desactivar las canchas de la competencia. |
| **`@PreAuthorize` con SpEL** consultando el repositorio | Spring lo permite, pero mete lógica de negocio dentro de una cadena de texto: no se compila, no se refactoriza, no se puede probar unitariamente y falla en tiempo de ejecución. La regla de propiedad **es** lógica de dominio y le corresponde al *service*. |
| **Filtrar por propietario en la consulta** (`findByIdAndManagerUser`) | Es elegante para las lecturas, pero confunde dos casos que deben distinguirse: "no existe" (`404`) y "existe pero no es tuyo" (`403`). Cargar primero y comparar después conserva la diferencia, que es información útil y honesta. |
| **Incluir el propietario en el DTO y validarlo** contra el token | Añade una validación que hay que recordar escribir en cada endpoint nuevo. Quitar el campo del DTO elimina la clase entera de error. |

## Consecuencias

**A favor**

- **La suplantación por body es imposible por construcción**, no por disciplina.
- **Cada `403` tiene una causa identificable en el log:** `event=authz.denied` lo emite Spring
  Security (capa 1); `event=ownership.denied` lo emite el `GlobalExceptionHandler` a partir de las
  excepciones `NotYour*` (capa 2). Al depurar se sabe de inmediato cuál de las dos reglas se
  aplicó.
- **La regla es probable unitariamente.** Las pruebas de *service* verifican la capa 2 sin levantar
  contexto de Spring; las de `@WebMvcTest` verifican la capa 1 con roles reales.

**En contra**

- **Una consulta adicional** antes de cada mutación, para cargar el recurso y comparar. A esta
  escala es irrelevante, y a cambio se conserva la distinción `404` / `403`.
- **El patrón hay que repetirlo** en cada agregado con dueño: `findCourtOwnedBy`,
  `findReservationOwnedBy`, `findTournamentOwnedBy`, más la comprobación de `registeredByUser` en
  equipos. Se aceptó la repetición porque cada uno lanza su propia excepción, con su propio
  mensaje; abstraerlos en un genérico habría producido mensajes de error impersonales.
- **Un `403` no revela si el recurso existe.** Es deliberado: decir "existe pero no es tuyo" ya es
  filtrar información. El mensaje es idéntico exista o no el dueño.

## Puesta en práctica

| Dónde | Qué |
|---|---|
| `config/SecurityConfig.kt` (ambos servicios) | Capa 1: `hasRole(MANAGER)` / `hasRole(PLAYER)` por método y ruta |
| `CourtService.findCourtOwnedBy` | Capa 2 en canchas |
| `ReservationService.findReservationOwnedBy` | Capa 2 en reservas |
| `TournamentService.findTournamentOwnedBy` | Capa 2 en torneos y partidos |
| `TournamentService.withdrawTeam` | Capa 2 en equipos (`registeredByUser`) |
| `exceptions/GlobalExceptionHandler.kt` | Traduce todas las `NotYour*Exception` a `403` con `event=ownership.denied` |
| `dto/CourtDto.kt`, `dto/ReservationDto.kt`, `dto/TeamDto.kt` | **Ausencia deliberada** del campo de propietario |

Verificación con los datos sembrados: la cancha 4 pertenece a `manager_ana` y la reserva 4 a
`player_luis`. Con el token de `manager_josue`, `PATCH /matchpoint/courts/4` responde `403`
aunque el rol sea el correcto — y el log muestra `event=ownership.denied`, no `authz.denied`.
