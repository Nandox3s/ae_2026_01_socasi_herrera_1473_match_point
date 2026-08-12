# Registros de Decisiones de Arquitectura (ADR)

**Criterio 1.4 de la rúbrica** — justificación clara de las decisiones de priorización de
requerimientos y su puesta en práctica en el desarrollo del sistema mediante el ADR.

Un ADR (*Architecture Decision Record*) es un documento corto e inmutable que deja constancia de
**una** decisión: el contexto en el que se tomó, la decisión misma, las alternativas que se
descartaron y las consecuencias que se aceptaron. No se edita cuando cambia de opinión el equipo:
se marca como *Reemplazado* y se escribe uno nuevo. Esa inmutabilidad es lo que lo hace útil —
permite reconstruir *por qué* el sistema es como es, meses después de que nadie recuerde la
discusión.

---

## 1. Cómo se priorizaron los requerimientos

La priorización no fue por intuición. Cada historia se puntuó con tres factores y se ordenó por el
resultado:

| Factor | Pregunta | Peso |
|---|---|---|
| **Valor** | ¿Sin esto el producto sirve de algo para un usuario real? | ×3 |
| **Riesgo técnico** | ¿Es lo que más probablemente nos bloquee si lo dejamos para el final? | ×2 |
| **Dependencia** | ¿Cuántas otras historias no pueden empezar hasta que esta esté? | ×1 |

De ahí salieron **tres reglas de orden** que gobiernan todo el backlog:

1. **Primero lo que habilita, después lo que luce.** La autenticación (HU-04) no le sirve a nadie
   por sí sola, pero **ninguna** otra historia puede probarse sin ella. Riesgo alto + dependencia
   máxima ⇒ va primera. → [ADR-001](ADR-001-identidad-primero.md)
2. **Un flujo completo antes que dos flujos a medias.** Se cerró reservas de punta a punta
   (publicar → buscar → reservar → cancelar) antes de tocar torneos, para tener siempre algo
   demostrable. → [ADR-005](ADR-005-reservas-antes-que-torneos.md)
3. **Lo que se puede simplificar sin perder la demostración, se simplifica.** El cupo potencia de
   dos elimina los *byes* y con ellos la mitad de la complejidad del cuadro, sin quitarle nada al
   torneo que se enseña. → [ADR-006](ADR-006-cuadro-potencia-de-dos.md)

**Orden de ejecución resultante**, que es exactamente el del historial de git:

| # | Historia | Valor | Riesgo | Dep. | Puntaje | ADR que la justifica |
|---|---|:--:|:--:|:--:|:--:|---|
| 1 | HU-04 Autenticación con Cognito | 3 | 5 | 8 | **27** | ADR-001, ADR-002 |
| 2 | HU-01 Registro de perfil | 4 | 2 | 4 | **20** | ADR-003 |
| 3 | HU-02 Actualización de perfil | 3 | 1 | 0 | **11** | — |
| 4 | HU-03 Administración de perfiles | 2 | 1 | 0 | **8** | ADR-007 |
| 5 | HU-05/06 Canchas y disponibilidad | 5 | 3 | 2 | **23** | ADR-005 |
| 6 | HU-07/08 Motor de reservas | 5 | 4 | 1 | **24** | ADR-004, ADR-005 |
| 7 | HU-09/10 Torneos e inscripción | 4 | 2 | 2 | **18** | ADR-006 |
| 8 | HU-11/12 Cuadro de partidos | 4 | 4 | 0 | **20** | ADR-006 |
| 9 | Configuración y observabilidad | 3 | 3 | 0 | **15** | ADR-009, ADR-010 |

> El orden de la tabla no es el del puntaje puro: HU-01 (20) va antes que HU-05/06 (23) porque
> HU-07 depende de HU-01 —no se reserva sin perfil— y adelantarla habría dejado la dependencia
> abierta. **La dependencia manda sobre el puntaje cuando hay conflicto**; es la única excepción y
> queda documentada aquí para que no parezca un descuido.

---

## 2. Índice de decisiones

| ADR | Decisión | Estado | Impacto principal |
|---|---|---|---|
| [ADR-001](ADR-001-identidad-primero.md) | Construir la identidad antes que cualquier funcionalidad de negocio | Aceptado | Priorización |
| [ADR-002](ADR-002-cognito-como-proveedor-de-identidad.md) | Delegar autenticación y roles en AWS Cognito | Aceptado | Seguridad |
| [ADR-003](ADR-003-dos-microservicios-base-por-servicio.md) | Dos microservicios con una base de datos cada uno | Aceptado | Arquitectura |
| [ADR-004](ADR-004-comunicacion-http-con-token-propagado.md) | Comunicación síncrona HTTP propagando el token del usuario | Aceptado | Integración |
| [ADR-005](ADR-005-reservas-antes-que-torneos.md) | Cerrar el flujo de reservas antes de empezar torneos | Aceptado | Priorización |
| [ADR-006](ADR-006-cuadro-potencia-de-dos.md) | Cupo potencia de dos y cuadro completo generado al arrancar | Aceptado | Dominio |
| [ADR-007](ADR-007-autorizacion-en-dos-capas.md) | Autorización en dos capas: rol y propiedad del recurso | Aceptado | Seguridad |
| [ADR-008](ADR-008-borrado-logico-y-auditoria.md) | Borrado lógico de reservas y auditoría en tabla propia | Aceptado | Datos |
| [ADR-009](ADR-009-gateway-unico-punto-de-entrada.md) | Un único punto de entrada; ningún microservicio publica puerto | Aceptado | Infraestructura |
| [ADR-010](ADR-010-cobertura-atada-al-build.md) | Umbral de cobertura del 100 % atado al build, con exclusiones declaradas | Aceptado | Calidad |
| [ADR-011](ADR-011-despliegue-local-y-arquitectura-objetivo.md) | Entregar sobre Docker Compose local y documentar la arquitectura AWS objetivo | Aceptado | Despliegue |

---

## 3. Plantilla

Los once registros siguen la misma estructura, derivada de la plantilla de Michael Nygard con una
sección añadida —**Puesta en práctica**— que es la que la rúbrica exige: no basta con haber
decidido, hay que poder señalar dónde vive la decisión en el código.

```markdown
# ADR-nnn · Título en una línea

- **Estado:** Propuesto | Aceptado | Reemplazado por ADR-mmm
- **Fecha:** AAAA-MM-DD
- **Decide:** quién
- **Requerimientos afectados:** RF-nn, RNF-nn

## Contexto
La fuerza que obligó a decidir. Hechos, no opiniones.

## Decisión
Una frase en presente y en voz activa.

## Alternativas consideradas
Qué más se evaluó y por qué se descartó. Sin alternativas, no hubo decisión.

## Consecuencias
Lo bueno y lo malo que aceptamos. Un ADR sin costos es propaganda.

## Puesta en práctica
Dónde vive la decisión en el código y cómo se verifica.
```
