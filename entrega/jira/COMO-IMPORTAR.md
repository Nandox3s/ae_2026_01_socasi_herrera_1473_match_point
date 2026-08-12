# Cómo importar el backlog de MatchPoint en Jira

Este archivo explica cómo cargar [`jira-import.csv`](jira-import.csv) en Jira y qué hacer si algo
no cuadra con la configuración de tu proyecto.

> **¿Solo quieres leer el contenido?** Está completo y en formato legible en
> [`BACKLOG.md`](BACKLOG.md): cada elemento con su descripción, sus criterios de aceptación, sus
> puntos y sus etiquetas. El reparto en iteraciones, con fechas, objetivos y velocidad, está en
> [`SPRINTS.md`](SPRINTS.md). Los tres archivos se generan desde el mismo script, así que no se
> desincronizan.

> **¿Prefieres cargarlo con la IA de Jira?** [`PROMPTS-IA.md`](PROMPTS-IA.md) trae catorce prompts
> listos para pegar en Atlassian Intelligence / Rovo. Lee primero la comparativa que abre ese
> archivo: la IA maneja mal los story points, el sprint y el estado, así que para carga masiva
> el CSV sigue siendo mejor.

---

## 1. Qué contiene el CSV

| | |
|---|---:|
| Filas de trabajo | **80** |
| Épicas | 13 |
| Historias | 45 |
| Tareas | 19 |
| Subtareas | 3 |
| Story points totales | **336** |
| Story points ya terminados | **175** (52 %) |
| Elementos en `Done` | 52 |
| Elementos en `To Do` | 28 |

**Las 13 épicas:**

| # | Épica | Estado | Contenido |
|---|---|---|---|
| 1 | Identidad y perfiles de usuario | Done | HU-01 a HU-03 + resolución por `cognitoId` |
| 2 | Seguridad: autenticación y autorización | Done | HU-04, mapeo de roles, propiedad del recurso |
| 3 | Gestión de canchas | Done | HU-05, HU-06, catálogo público |
| 4 | Motor de reservas | Done | HU-07 (con 2 subtareas), HU-08 |
| 5 | Torneos e inscripción de equipos | Done | HU-09, HU-10, consulta pública |
| 6 | Cuadro de partidos y avance automático | Done | HU-11 (con subtarea), HU-12, programación |
| 7 | Observabilidad, auditoría y calidad | Done | Logging, auditoría, errores, pruebas |
| 8 | Infraestructura, contenedores y gateway | Done | Docker, nginx, BD por servicio, secretos |
| 9 | Documentación y entrega académica | Done | Los 11 documentos de la rúbrica + Postman + ER |
| 10 | **Higiene del repositorio y GitFlow** | To Do | Los 13 commits pendientes, la identidad de git y `main` al día |
| 11 | **App móvil Android** | To Do | 5 historias — **ver el aviso de §2** |
| 12 | Despliegue en la nube (AWS) | To Do | 6 historias: fases 1 y 2, CI/CD, Flyway, CloudWatch |
| 13 | Producto y modelo de negocio | To Do | 7 historias: reportes, pagos, precio dinámico, captación |

Cada historia trae **descripción con criterios de aceptación**, referencia al requerimiento
(`RF-nn`), al ADR que la justifica cuando aplica, y la ruta del archivo que la evidencia.

> La épica 10 es la más urgente: recoge los 103 archivos sin commitear, los cinco commits con la
> identidad de git sin configurar y la rama `main` cinco commits por detrás de `develop`. Es
> exactamente lo que evalúa el criterio 1.2 de la rúbrica.

---

## 2. Antes de importar: dos decisiones

### La épica 11 (App móvil)

La app móvil la desarrolla otro integrante. Si ya la lleva en su propio tablero, **borra esas 6
filas antes de importar** para no duplicar trabajo:

```bash
grep -n "App móvil\|Autenticación con Cognito desde la app\|Catálogo de canchas y búsqueda\|Flujo de reserva con validaciones\|Mis reservas y cancelación\|Vista del cuadro de torneo" jira-import.csv
```

Si no, déjalas: sirven para que el tablero refleje el proyecto completo.

### Los responsables

La columna `Assignee` trae los nombres `Josue Herrera` y `Fernando Socasi`. Jira necesita que
coincidan con cuentas reales del sitio; si no las encuentra, **importa igual y deja el campo
vacío**. Para asignarlas de verdad, reemplaza esos valores por el correo de cada cuenta antes de
importar.

---

## 3. Importación (Jira Cloud, proyecto gestionado por el equipo)

Es el caso más común: un proyecto Scrum o Kanban creado recientemente.

1. **Crea el proyecto** si aún no existe: *Proyectos → Crear proyecto → Scrum → Gestionado por el equipo*.
   Anota la clave (por ejemplo `MP`).
2. **Habilita los campos que usa el CSV**, o sus columnas se ignorarán en silencio:
   - *Story point estimate* — en Scrum viene activo por defecto.
   - *Componentes* — *Configuración del proyecto → Funciones → Componentes*. Si no lo activas,
     omite esa columna en el mapeo.
   - Crea los componentes: `users`, `matchpoint`, `transversal`, `infra`, `docs`, `movil`, `producto`.
3. **Abre el importador:** en el proyecto, *…* (arriba a la derecha) → **Importar incidencias**
   → *Importar desde CSV*. Selecciona `jira-import.csv`.
4. **Codificación:** UTF-8. **Delimitador:** coma.
5. **Mapeo de columnas:**

| Columna del CSV | Campo de Jira |
|---|---|
| `Issue Id` | **Id de incidencia** *(no es el campo Resumen — verifícalo)* |
| `Parent Id` | **Id principal** / *Parent Id* |
| `Issue Type` | Tipo de incidencia |
| `Summary` | Resumen |
| `Description` | Descripción |
| `Priority` | Prioridad |
| `Status` | Estado |
| `Story Points` | Story point estimate |
| `Component` | Componentes |
| `Epic Name` | Nombre de épica *(si no aparece, no lo mapees: es opcional aquí)* |
| `Assignee` | Persona asignada |
| `Sprint` | Sprint *(Jira crea los que falten; las fechas se ponen después a mano)* |
| `Labels` ×5 | Etiquetas *(las cinco a la misma)* |

6. **Mapeo de valores:** cuando pregunte por `Status`, asegúrate de que `Done` → *Listo* / *Done* y
   `To Do` → *Tareas por hacer* / *To Do*. Los tipos deben mapear a `Epic`, `Story`, `Task` y
   `Subtask`.
7. **Importa.** El importador informa cuántas incidencias creó. Deben ser **80**.

---

## 4. Si tu proyecto es "gestionado por la empresa" (company-managed)

Ahí la relación épica → historia **no** usa `Parent Id`, sino los campos clásicos:

1. Añade al CSV una columna `Epic Link` con el **nombre de la épica** en cada historia
   (el mismo texto de `Epic Name` de su épica padre).
2. Mapea `Epic Name` → *Epic Name* solo en las filas de tipo `Epic`.
3. Mapea `Parent Id` → *Parent Id* **únicamente** para las 3 subtareas.

Alternativa más simple si el mapeo se complica: **importa sin jerarquía** (no mapees `Parent Id`
ni `Epic Link`) y arrastra las historias bajo su épica en el *backlog*. Con 80 elementos son unos
diez minutos.

---

## 5. Después de importar

1. **Revisa la jerarquía** en el *backlog*: las 13 épicas deben aparecer en el panel lateral, cada
   una con sus historias.
2. **Crea los sprints.** El CSV usa `Sprint 1` … `Sprint 7` y `Backlog`, y al mapear la columna
   `Sprint` Jira crea los que no existan y reparte los elementos. Lo que **no** viaja en el CSV son
   las fechas ni el objetivo de cada sprint: eso se rellena a mano con la tabla de
   [`SPRINTS.md`](SPRINTS.md). Después, cierra los sprints 1 a 5 para que el informe de velocidad
   tenga histórico e inicia el **Sprint 6** como sprint activo.
3. **Revisa el tablero:** deberías ver 52 elementos en *Done* y 28 en *To Do*.
4. **Filtro útil para la sustentación** — todo lo que la rúbrica evalúa y ya está hecho:
   ```
   project = MP AND status = Done ORDER BY created ASC
   ```
5. **Filtro de lo que queda:**
   ```
   project = MP AND status != Done ORDER BY priority DESC
   ```

---

## 6. Problemas frecuentes

| Síntoma | Causa | Solución |
|---|---|---|
| Las historias quedan sueltas, sin épica | `Parent Id` no se mapeó, o el proyecto es *company-managed* | Ver §4 |
| Los story points quedan vacíos | El campo no está habilitado en el proyecto | Actívalo y reimporta, o cárgalos a mano |
| Todo entra como `Task` | No se mapeó `Issue Type`, o los nombres de tipo difieren | Mapea explícitamente `Epic`, `Story`, `Task`, `Subtask` |
| Los acentos salen mal | Codificación distinta de UTF-8 | Vuelve a seleccionar UTF-8 en el importador |
| Todo queda en el primer estado del flujo | El flujo de trabajo del proyecto no tiene un estado llamado `Done` | Mapea `Done` al estado final que sí exista |
| Las subtareas fallan | El tipo *Subtask* no está habilitado | Actívalo, o cambia esas 3 filas a `Story` |
| Los sprints no aparecen | El proyecto es Kanban, no Scrum | En Kanban no hay sprints: usa la columna como etiqueta o crea versiones con esos nombres |
| Los sprints se crean sin fechas | El CSV no puede llevarlas | Rellénalas a mano con la tabla de `SPRINTS.md` |
| Se importaron menos de 80 | Alguna fila fue rechazada | El importador muestra el registro de errores: revísalo antes de reintentar |

**Si algo sale mal:** el importador de Jira permite **deshacer la última importación** desde el
registro de importaciones. Úsalo antes de reimportar, o tendrás todo duplicado.

---

## 7. Regenerar el CSV

Los cuatro archivos —el CSV, [`BACKLOG.md`](BACKLOG.md), [`SPRINTS.md`](SPRINTS.md) y
[`PROMPTS-IA.md`](PROMPTS-IA.md)— los produce [`generar-backlog.py`](generar-backlog.py), que no
necesita dependencias externas:

```bash
python3 generar-backlog.py
```

Edita ese script para añadir historias, cambiar responsables o ajustar estimaciones, y vuelve a
ejecutarlo: se regeneran los cuatro a la vez y nunca se desincronizan. Es preferible a editar el CSV a
mano, porque las descripciones son multilínea y el escapado se rompe con facilidad.
