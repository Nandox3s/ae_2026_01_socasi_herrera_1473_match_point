# MatchPoint · Plan de sprints

Siete iteraciones de **una semana** más el backlog sin planificar. Se genera junto con [`BACKLOG.md`](BACKLOG.md) y [`jira-import.csv`](jira-import.csv) desde [`generar-backlog.py`](generar-backlog.py).

> Las fechas de inicio y fin **no viajan en el CSV**: Jira no las importa. Hay que crearlas a mano en el tablero con la tabla de la sección 1. El resto —qué elemento va en qué sprint— sí lo trae la columna `Sprint`.

---

## 1. Los sprints de un vistazo

| Sprint | Fechas | Estado | Objetivo | Elementos | Puntos | Hechos |
|---|---|---|---|---:|---:|---:|
| **Sprint 1** | 2026-07-07 → 2026-07-13 | ✅ Cerrado | Fundación: identidad antes que negocio | 4 | 19 | 19 |
| **Sprint 2** | 2026-07-14 → 2026-07-20 | ✅ Cerrado | Perfiles completos y catálogo de canchas | 6 | 28 | 28 |
| **Sprint 3** | 2026-07-21 → 2026-07-27 | ✅ Cerrado | Motor de reservas de punta a punta | 4 | 20 | 20 |
| **Sprint 4** | 2026-07-28 → 2026-08-03 | ✅ Cerrado | Torneos y cuadro de eliminación directa | 7 | 34 | 34 |
| **Sprint 5** | 2026-08-04 → 2026-08-10 | ✅ Cerrado | Observabilidad, calidad e infraestructura | 9 | 38 | 38 |
| **Sprint 6** | 2026-08-11 → 2026-08-17 | 🟡 En curso | Documentación de la entrega e higiene del repositorio | 19 | 48 | 36 |
| **Sprint 7** | 2026-08-18 → 2026-08-24 | ⬜ Planificado | App móvil Android | 5 | 37 | 0 |
| **Backlog** | — | 📥 Sin planificar | Despliegue en la nube y producto | 13 | 112 | 0 |
| | | | **Total** | **67** | **336** | **175** |

**Velocidad media de los sprints cerrados: 28 puntos por semana** (19 · 28 · 20 · 34 · 38).

---

## 2. Velocidad

```
Sprint 1    19 pts  ███████
Sprint 2    28 pts  ██████████
Sprint 3    20 pts  ███████
Sprint 4    34 pts  ████████████
Sprint 5    38 pts  ██████████████
Sprint 6    48 pts  █████████████████  (en curso)
Sprint 7    37 pts  █████████████  (planificado)
Backlog    112 pts  ████████████████████████████████████████  (sin planificar)
media 1-5   28 pts  ··········
```

El Sprint 6 está **por encima de la velocidad histórica** (48 puntos frente a 28). Es deliberado y está bajo control: 36 de esos puntos ya están terminados —la documentación— y lo que queda son las seis tareas de integración de la épica 10, que son mecánicas.

El Backlog no se compara contra la velocidad: no tiene compromiso de fecha.

---

## 3. Sprint por sprint

### ✅ Sprint 1 · Fundación: identidad antes que negocio

**2026-07-07 → 2026-07-13** · Cerrado · 4 elementos · **19 puntos** (19 hechos)

Ninguna otra historia se puede probar sin la autenticación, así que va primera (ADR-001). Al cerrar el sprint, un token real de Cognito atraviesa el sistema y produce ROLE_MANAGER o ROLE_PLAYER.

> **Criterio de cierre:** GET /matchpoint/me con token real devuelve username, sub y groups; sin token devuelve 401.

| | Elemento | Tipo | Épica | Pts | Responsable |
|---|---|---|---|---:|---|
| ✅ | HU-01 Registro automático de perfil de usuario | Story | Identidad y perfiles de usuario | 5 | Fernando Socasi |
| ✅ | HU-02 Actualización de datos de contacto | Story | Identidad y perfiles de usuario | 3 | Fernando Socasi |
| ✅ | HU-04 Validación de JWT y JWKS con AWS Cognito | Story | Seguridad: autenticación y autorización | 8 | Fernando Socasi |
| ✅ | Mapeo de cognito:groups a roles de Spring Security | Story | Seguridad: autenticación y autorización | 3 | Fernando Socasi |

---

### ✅ Sprint 2 · Perfiles completos y catálogo de canchas

**2026-07-14 → 2026-07-20** · Cerrado · 6 elementos · **28 puntos** (28 hechos)

Se cierra el microservicio users y se abre el dominio deportivo por donde entra el usuario: el catálogo público y el motor de disponibilidad.

> **Criterio de cierre:** Un visitante sin cuenta consulta qué canchas están libres en una franja horaria.

| | Elemento | Tipo | Épica | Pts | Responsable |
|---|---|---|---|---:|---|
| ✅ | HU-03 Administración de perfiles exclusiva de MANAGER | Story | Identidad y perfiles de usuario | 5 | Fernando Socasi |
| ✅ | Resolución de perfil por identificador de Cognito | Story | Identidad y perfiles de usuario | 2 | Fernando Socasi |
| ✅ | Autorización por propiedad del recurso | Story | Seguridad: autenticación y autorización | 5 | Josué Herrera |
| ✅ | HU-05 Publicación y actualización de canchas | Story | Gestión de canchas | 5 | Josué Herrera |
| ✅ | HU-06 Motor de búsqueda de disponibilidad | Story | Gestión de canchas | 8 | Josué Herrera |
| ✅ | Catálogo público de canchas con filtros | Story | Gestión de canchas | 3 | Josué Herrera |

---

### ✅ Sprint 3 · Motor de reservas de punta a punta

**2026-07-21 → 2026-07-27** · Cerrado · 4 elementos · **20 puntos** (20 hechos)

El sprint de mayor riesgo: es el único flujo que cruza la frontera entre microservicios. Se cierra reservas completo antes de tocar torneos (ADR-005).

> **Criterio de cierre:** Reservar, chocar contra el 409 de solapamiento, cancelar y volver a reservar la misma franja.

| | Elemento | Tipo | Épica | Pts | Responsable |
|---|---|---|---|---:|---|
| ✅ | HU-07 Creación de reserva con validación de solapamiento | Story | Motor de reservas | 8 | Josué Herrera |
| ✅ | Validar perfil llamando al microservicio users | Sub-task | Motor de reservas | 5 | Josué Herrera |
| ✅ | Sembrar datos que disparan cada validación de reserva | Sub-task | Motor de reservas | 2 | Josué Herrera |
| ✅ | HU-08 Consulta y cancelación de reservas propias | Story | Motor de reservas | 5 | Josué Herrera |

---

### ✅ Sprint 4 · Torneos y cuadro de eliminación directa

**2026-07-28 → 2026-08-03** · Cerrado · 7 elementos · **34 puntos** (34 hechos)

El dominio más complejo, abordado cuando la infraestructura transversal ya estaba resuelta y solo había que aplicarla.

> **Criterio de cierre:** Torneo de 4 equipos de punta a punta: crear, inscribir, arrancar, puntuar y coronar campeón.

| | Elemento | Tipo | Épica | Pts | Responsable |
|---|---|---|---|---:|---|
| ✅ | HU-09 Creación de torneos | Story | Torneos e inscripción de equipos | 5 | Josué Herrera |
| ✅ | HU-10 Inscripción y retiro de equipos | Story | Torneos e inscripción de equipos | 5 | Josué Herrera |
| ✅ | Consulta pública de torneos y del cuadro | Story | Torneos e inscripción de equipos | 3 | Josué Herrera |
| ✅ | HU-11 Generación automática del cuadro | Story | Cuadro de partidos y avance automático | 8 | Josué Herrera |
| ✅ | Calcular número de rondas y nombres legibles | Sub-task | Cuadro de partidos y avance automático | 2 | Josué Herrera |
| ✅ | HU-12 Registro de marcador y avance del ganador | Story | Cuadro de partidos y avance automático | 8 | Josué Herrera |
| ✅ | Programación de fecha y hora de partidos | Story | Cuadro de partidos y avance automático | 3 | Josué Herrera |

---

### ✅ Sprint 5 · Observabilidad, calidad e infraestructura

**2026-08-04 → 2026-08-10** · Cerrado · 9 elementos · **38 puntos** (38 hechos)

Lo que convierte el código en un sistema entregable: logging estándar, auditoría, umbral de cobertura atado al build y los seis contenedores con un solo puerto publicado.

> **Criterio de cierre:** docker compose up levanta todo en healthy y ./gradlew check pasa en los dos servicios.

| | Elemento | Tipo | Épica | Pts | Responsable |
|---|---|---|---|---:|---|
| ✅ | Estándar de logging de una sola línea | Story | Observabilidad, auditoría y calidad | 5 | Josué Herrera |
| ✅ | Auditoría de todas las escrituras | Story | Observabilidad, auditoría y calidad | 5 | Josué Herrera |
| ✅ | Manejo global de excepciones y códigos HTTP semánticos | Story | Observabilidad, auditoría y calidad | 3 | Josué Herrera |
| ✅ | Suite de pruebas y umbral de cobertura atado al build | Story | Observabilidad, auditoría y calidad | 8 | Josué Herrera |
| ✅ | Contenedorización con build multi-stage | Story | Infraestructura, contenedores y gateway | 5 | Josué Herrera |
| ✅ | Gateway nginx como único punto de entrada | Story | Infraestructura, contenedores y gateway | 5 | Josué Herrera |
| ✅ | Base de datos por servicio con healthchecks | Story | Infraestructura, contenedores y gateway | 3 | Josué Herrera |
| ✅ | Explorador de base de datos detrás del gateway | Story | Infraestructura, contenedores y gateway | 2 | Josué Herrera |
| ✅ | Gestión de secretos fuera del repositorio | Story | Infraestructura, contenedores y gateway | 2 | Josué Herrera |

---

### 🟡 Sprint 6 · Documentación de la entrega e higiene del repositorio

**2026-08-11 → 2026-08-17** · En curso · 19 elementos · **48 puntos** (36 hechos)

Los once documentos de la rúbrica más el trabajo pendiente de integración: 103 archivos sin commitear, la identidad de git y main al día.

> **Criterio de cierre:** Los entregables de la rúbrica versionados en develop, y main sin commits de diferencia.

| | Elemento | Tipo | Épica | Pts | Responsable |
|---|---|---|---|---:|---|
| ✅ | Levantamiento de requerimientos RF y RNF | Task | Documentación y entrega académica | 3 | Josué Herrera |
| ✅ | Tabla y detalle de casos de uso | Task | Documentación y entrega académica | 3 | Josué Herrera |
| ✅ | Documentación del manejo de GitFlow | Task | Documentación y entrega académica | 2 | Josué Herrera |
| ✅ | Estrategia y evidencia de pruebas unitarias | Task | Documentación y entrega académica | 2 | Josué Herrera |
| ✅ | Registros de decisiones de arquitectura (ADR) | Task | Documentación y entrega académica | 5 | Josué Herrera |
| ✅ | Documento de Arquitectura Empresarial | Task | Documentación y entrega académica | 3 | Josué Herrera |
| ✅ | Documento de Computación en la Nube | Task | Documentación y entrega académica | 3 | Josué Herrera |
| ✅ | Business Model Canvas | Task | Documentación y entrega académica | 3 | Josué Herrera |
| ✅ | Propuesta tecnológica con innovación | Task | Documentación y entrega académica | 2 | Josué Herrera |
| ✅ | Planificación financiera a tres años | Task | Documentación y entrega académica | 3 | Josué Herrera |
| ✅ | Guion de sustentación y banco de preguntas | Task | Documentación y entrega académica | 2 | Josué Herrera |
| ✅ | Colección de Postman de punta a punta | Task | Documentación y entrega académica | 3 | Josué Herrera |
| ✅ | Diagramas del modelo entidad-relación | Task | Documentación y entrega académica | 2 | Josué Herrera |
| ⬜ | Configurar la identidad de git en las máquinas del equipo | Task | Higiene del repositorio y GitFlow | 1 | Josué Herrera |
| ⬜ | Integrar limpieza de código y configuración | Task | Higiene del repositorio y GitFlow | 3 | Josué Herrera |
| ⬜ | Integrar herramientas de demostración | Task | Higiene del repositorio y GitFlow | 2 | Josué Herrera |
| ⬜ | Integrar documentación técnica del proyecto | Task | Higiene del repositorio y GitFlow | 2 | Josué Herrera |
| ⬜ | Integrar la documentación de la entrega P02 | Task | Higiene del repositorio y GitFlow | 3 | Josué Herrera |
| ⬜ | Actualizar main con el estado de develop | Task | Higiene del repositorio y GitFlow | 1 | Josué Herrera |

---

### ⬜ Sprint 7 · App móvil Android

**2026-08-18 → 2026-08-24** · Planificado · 5 elementos · **37 puntos** (0 hechos)

Cliente nativo que consume la misma API por el gateway. Lo desarrolla el otro integrante.

> **Criterio de cierre:** Login con Cognito, catálogo, reserva con validaciones y vista del cuadro desde el teléfono.

| | Elemento | Tipo | Épica | Pts | Responsable |
|---|---|---|---|---:|---|
| ⬜ | Autenticación con Cognito desde la app | Story | App móvil Android | 8 | — |
| ⬜ | Catálogo de canchas y búsqueda de disponibilidad | Story | App móvil Android | 8 | — |
| ⬜ | Flujo de reserva con validaciones de formulario | Story | App móvil Android | 8 | — |
| ⬜ | Mis reservas y cancelación | Story | App móvil Android | 5 | — |
| ⬜ | Vista del cuadro de torneo | Story | App móvil Android | 8 | — |

---

### 📥 Backlog · Despliegue en la nube y producto

Sin planificar · 13 elementos · **112 puntos**

Lo que sigue después de la entrega académica: llevar el sistema a AWS y convertir el backend en un negocio.

> **Criterio de cierre:** Sin compromiso de fecha: se planifica cuando termine la entrega.

| | Elemento | Tipo | Épica | Pts | Responsable |
|---|---|---|---|---:|---|
| ⬜ | Fase 1: EC2 con Docker Compose y RDS Single-AZ | Story | Despliegue en la nube (AWS) | 8 | — |
| ⬜ | Fase 2: ALB, Auto Scaling Group y RDS Multi-AZ | Story | Despliegue en la nube (AWS) | 13 | — |
| ⬜ | Pipeline CI/CD con despliegue azul-verde | Story | Despliegue en la nube (AWS) | 8 | — |
| ⬜ | Integración continua con GitHub Actions | Story | Despliegue en la nube (AWS) | 5 | — |
| ⬜ | Migraciones versionadas con Flyway | Story | Despliegue en la nube (AWS) | 5 | — |
| ⬜ | Centralización de logs y alarmas en CloudWatch | Story | Despliegue en la nube (AWS) | 5 | — |
| ⬜ | Reporte mensual de ocupación para el dueño de cancha | Story | Producto y modelo de negocio | 8 | — |
| ⬜ | Integración de pasarela de pagos local | Story | Producto y modelo de negocio | 13 | — |
| ⬜ | Precio sugerido por franja horaria | Story | Producto y modelo de negocio | 13 | — |
| ⬜ | Emparejamiento de jugadores sueltos | Story | Producto y modelo de negocio | 13 | — |
| ⬜ | Modo sin conexión en la app móvil | Story | Producto y modelo de negocio | 8 | — |
| ⬜ | Captación de las 10 canchas fundadoras | Story | Producto y modelo de negocio | 8 | — |
| ⬜ | Soporte de deportes adicionales | Story | Producto y modelo de negocio | 5 | — |

---

## 4. Cómo crear los sprints en Jira

1. Importa el CSV siguiendo [`COMO-IMPORTAR.md`](COMO-IMPORTAR.md) y **mapea la columna `Sprint`**. Jira crea los sprints que no existan y reparte los elementos.

2. En el *backlog* del tablero, abre cada sprint → *Editar sprint* y rellena las fechas de la tabla de la sección 1. **Este paso es manual**: el importador no admite fechas de sprint.

3. Pega el objetivo del sprint en el campo *Objetivo del sprint*. Son las frases de la columna «Objetivo».

4. Cierra los sprints 1 a 5 (*Completar sprint*) para que quede el histórico y el informe de velocidad tenga datos. Los elementos ya vienen en `Done`, así que se cierran sin arrastrar nada.

5. Inicia el **Sprint 6** como sprint activo.

6. Deja el Sprint 7 y el Backlog sin iniciar.


> Si tu proyecto es Kanban en lugar de Scrum, no hay sprints: usa la columna `Sprint` como una etiqueta más, o crea *versiones* (`Fix Version`) con los mismos nombres.
