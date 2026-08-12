# ADR-008 · Borrado lógico de reservas y auditoría en tabla propia

- **Estado:** Aceptado
- **Fecha:** 2026-08-03
- **Decide:** Josué Herrera
- **Requerimientos afectados:** RF-20, RF-32 · RNF-02
- **Historias:** HU-08, configuración transversal

## Contexto

Cancelar una reserva parece un `DELETE` y no lo es. Hay tres necesidades en conflicto:

1. La franja **debe** volver a estar disponible de inmediato para otros jugadores.
2. El historial de la cancha —cuántas reservas tuvo, cuántas se cancelaron— es exactamente el dato
   con el que un dueño de complejo decide si el sistema le sirve. Es el argumento comercial del
   producto.
3. Una fila borrada no deja rastro de quién la borró ni de qué contenía.

En paralelo, la rúbrica y el sentido común piden saber **quién hizo qué y cuándo** sobre todas las
entidades, no solo sobre reservas.

## Decisión

**Dos mecanismos complementarios, que resuelven cosas distintas.**

### 1. Borrado lógico en reservas

`DELETE /matchpoint/reservations/{id}` **no borra la fila**: cambia `status` de `CONFIRMED` a
`CANCELLED`. La disponibilidad se resuelve consultando únicamente reservas `CONFIRMED`, así que la
franja se libera en el mismo instante.

```kotlin
reservationRepository.findByCourtIdAndStatus(courtId, ReservationStatus.CONFIRMED)
    .none { startsAt < it.endsAt() && it.startsAt < endsAt }
```

### 2. Tabla `audit_log`, una por microservicio

Cada `INSERT`, `UPDATE` y `DELETE` de las entidades de dominio escribe una fila con:

| Dimensión | Columnas |
|---|---|
| **Quién** | `user_sub` (claim `sub` del JWT) + `user_name` (claim `username`) |
| **Qué** | `entity_name`, `entity_id`, `action` |
| **Cuándo** | `created_at` |
| **Qué cambió** | `old_values`, `new_values` — **con correos y teléfonos enmascarados** |

`audit_log` **no tiene clave foránea** hacia las tablas que audita: apunta a cualquier entidad con
el par (`entity_name`, `entity_id`). Es deliberado — las filas de auditoría deben sobrevivir al
borrado de la fila auditada, que es justamente cuando más valen.

## Alternativas consideradas

| Alternativa | Por qué se descartó |
|---|---|
| **Borrado físico de reservas** | Pierde el histórico, que es el dato de valor comercial, y deja la cancelación sin rastro. |
| **Tabla `cancelled_reservations` aparte** | Duplica el esquema y obliga a consultar dos tablas para reconstruir la vida de una reserva. |
| **Triggers de PostgreSQL para auditar** | Funcionan y son difíciles de eludir, pero el trigger **no ve el token**: no puede saber quién es el usuario, solo el rol de base de datos, que es el mismo para toda la aplicación. La dimensión "quién" —la más importante— se perdería. |
| **Hibernate Envers** | Resuelve el versionado de entidades de forma automática, pero genera una tabla espejo por entidad y su modelo de revisiones no encaja con "quién, qué, cuándo y valores enmascarados" en una sola tabla legible. Para el volumen de este sistema, un `AuditService` de 40 líneas es más simple y mucho más explicable en una sustentación. |
| **Confiar solo en los logs** | Los logs son un flujo, no una tabla: no se consultan con SQL, no se conservan al reiniciar el contenedor y no admiten un `WHERE user_name = …`. |

## Consecuencias

**A favor**

- La franja se libera al instante **y** el histórico se conserva. Las dos necesidades quedan
  satisfechas sin transigir en ninguna.
- La auditoría es **consultable con SQL** durante la demo:
  ```sql
  SELECT * FROM audit_log ORDER BY created_at DESC LIMIT 10;
  ```
- El enmascaramiento se aplica también a los valores auditados, no solo a los logs: `audit_log`
  nunca guarda un correo o un teléfono completo (RNF-02).
- La ausencia de FK permite auditar entidades de cualquier tabla con el mismo mecanismo, y que el
  rastro sobreviva al `DELETE` de un perfil de usuario.

**En contra**

- **Toda consulta de disponibilidad debe filtrar por `status`.** Si un desarrollador futuro olvida
  el filtro, contará reservas canceladas como ocupadas. Está centralizado en `CourtService.isFree`
  y en `ReservationService.createReservation`, y cubierto por pruebas — pero es una obligación
  permanente del modelo.
- **La tabla crece indefinidamente.** A esta escala es irrelevante; en producción necesitaría una
  política de retención y particionado por fecha.
- **El `AuditService` se invoca a mano** desde cada *service*, así que se puede olvidar en una
  mutación nueva. Es el costo de no usar triggers ni Envers; se compensa con las pruebas, que
  verifican con `verify(auditService)` que cada mutación audita —y que **cada rechazo no audita**.

## Puesta en práctica

| Dónde | Qué |
|---|---|
| `enums/ReservationStatus.kt` | `CONFIRMED` / `CANCELLED` |
| `ReservationService.cancel` | Cambia el estado; **nunca** llama a `repository.delete` |
| `CourtService.isFree` | Filtra por `CONFIRMED`: la franja cancelada vuelve a estar libre |
| `audit/AuditService.kt` | Un método `record(entity, id, action, oldValues, newValues)` |
| `audit/AuditLog.kt` | La entidad, sin FK hacia las tablas auditadas |
| `logging/LogEvents.kt` | `maskEmail` y `maskPhone`, aplicados también a los valores auditados |
| `AuditServiceTest` | 5 pruebas en cada microservicio |

Verificación en vivo: crear una reserva, cancelarla y volver a reservar la **misma franja**.
La segunda reserva se crea sin problema (la franja se liberó) y `audit_log` contiene las tres
operaciones con el usuario que las hizo.
