# MatchPoint · Entrega P02

Documentación producida para la **Evaluación de Proyecto Integrador – P02**. Vive aparte de
[`docs/`](../docs), que conserva la documentación técnica original del proyecto sin modificar.

**Proyecto:** MatchPoint — reserva de canchas de básquet y torneos de eliminación directa
**Equipo:** Josué Herrera (`jxherrera`) · Fernando Socasi (`Nandox3s`)

---

## Mapa rúbrica → entregable

| Criterio | Pts | Entregable | Estado |
|---|:--:|---|---|
| **1. Análisis y Diseño de Sistemas de Información** | **/8** | | |
| 1.1 Precisión y calidad de RF, RNF y casos de uso | /2 | [`01-analisis/REQUERIMIENTOS.md`](01-analisis/REQUERIMIENTOS.md) · [`01-analisis/CASOS-DE-USO.md`](01-analisis/CASOS-DE-USO.md) | ✅ |
| 1.2 Manejo apropiado de GitFlow | /2 | [`01-analisis/GITFLOW.md`](01-analisis/GITFLOW.md) | ✅ |
| 1.3 Aplicación adecuada de pruebas unitarias | /2 | [`01-analisis/PRUEBAS-UNITARIAS.md`](01-analisis/PRUEBAS-UNITARIAS.md) | ✅ |
| 1.4 Justificación de la priorización mediante ADR | /2 | [`01-analisis/adr/`](01-analisis/adr) — 11 registros | ✅ |
| **2. Desarrollo Móvil** | **/8** | *fuera de esta entrega — lo desarrolla el otro integrante* | — |
| **3. Arquitectura Empresarial** | **/8** | [`03-arquitectura/ARQUITECTURA-EMPRESARIAL.md`](03-arquitectura/ARQUITECTURA-EMPRESARIAL.md) | ✅ |
| **4. Computación en la Nube** | **/8** | [`04-nube/COMPUTACION-EN-LA-NUBE.md`](04-nube/COMPUTACION-EN-LA-NUBE.md) | ✅ |
| **5. Emprendimiento** | **/8** | | |
| 5.1 Business Model Canvas | /4 | [`05-emprendimiento/BUSINESS-MODEL-CANVAS.md`](05-emprendimiento/BUSINESS-MODEL-CANVAS.md) | ✅ |
| 5.2 Propuesta tecnológica con innovación | /2 | [`05-emprendimiento/PROPUESTA-TECNOLOGICA.md`](05-emprendimiento/PROPUESTA-TECNOLOGICA.md) | ✅ |
| 5.3 Planificación financiera | /2 | [`05-emprendimiento/PLAN-FINANCIERO.md`](05-emprendimiento/PLAN-FINANCIERO.md) | ✅ |
| **6. Sustentación** | **/10** | [`06-sustentacion/GUION-SUSTENTACION.md`](06-sustentacion/GUION-SUSTENTACION.md) | ✅ |
| **Backlog y sprints en Jira** | — | [`jira/BACKLOG.md`](jira/BACKLOG.md) · [`jira/SPRINTS.md`](jira/SPRINTS.md) · [`jira/jira-import.csv`](jira/jira-import.csv) · [`jira/PROMPTS-IA.md`](jira/PROMPTS-IA.md) · [`jira/COMO-IMPORTAR.md`](jira/COMO-IMPORTAR.md) | ✅ |

---

## Estructura

```
entrega/
├── 01-analisis/
│   ├── REQUERIMIENTOS.md          33 RF + 16 RNF + 14 reglas de negocio + trazabilidad
│   ├── CASOS-DE-USO.md            14 casos de uso con flujos alternativos + matriz CU × HTTP
│   ├── GITFLOW.md                 ramas, convenciones, ciclo de vida y verificación
│   ├── PRUEBAS-UNITARIAS.md       214 pruebas, mocks, aserciones, los dos números de cobertura
│   └── adr/                       11 registros de decisiones + criterio de priorización
├── 03-arquitectura/
│   └── ARQUITECTURA-EMPRESARIAL.md   los 6 sub-criterios con evidencia por archivo
├── 04-nube/
│   └── COMPUTACION-EN-LA-NUBE.md     conceptos, escalamiento, contenedores, arquitectura AWS
├── 05-emprendimiento/
│   ├── BUSINESS-MODEL-CANVAS.md      los 9 bloques + punto de equilibrio + riesgos
│   ├── PROPUESTA-TECNOLOGICA.md      stack justificado + 8 puntos de innovación + deuda técnica
│   └── PLAN-FINANCIERO.md            3 años, flujo mes a mes, sensibilidad
├── 06-sustentacion/
│   └── GUION-SUSTENTACION.md         guion por bloques + banco de preguntas + glosario
└── jira/
    ├── BACKLOG.md                    el backlog completo, legible: cada issue con su contenido
    ├── SPRINTS.md                    7 sprints con fechas, objetivo, alcance y velocidad
    ├── jira-import.csv               80 elementos, 13 épicas, 336 story points
    ├── PROMPTS-IA.md                 14 prompts para cargarlo con Atlassian Intelligence / Rovo
    ├── COMO-IMPORTAR.md              instructivo paso a paso
    └── generar-backlog.py            regenera los cuatro desde una sola fuente
```

---

## Los números del proyecto

| | |
|---|---|
| Requerimientos funcionales | **33**, todos implementados |
| Requerimientos no funcionales | **16**, todos cumplidos |
| Casos de uso documentados | 14 |
| Reglas de negocio con validación en código | 14 |
| Decisiones de arquitectura (ADR) | 11 |
| Pruebas automáticas | **214** (157 `matchpoint` + 57 `users`) |
| Cobertura de líneas del código propio | **100 %**, con el umbral atado al build |
| Microservicios | 2, con base de datos independiente cada uno |
| Contenedores | 6, en 3 capas, con **un solo puerto publicado** |
| Ramas de GitFlow publicadas | 9 `feature/*` + `main` + `develop` |
| Elementos en el backlog de Jira | **80** en 13 épicas, 336 story points |
| Sprints planificados | 7 de una semana + backlog · velocidad media **28 pts/semana** |
| Costo mensual de la arquitectura AWS diseñada | **127 USD** |
| Punto de equilibrio del negocio | **9 canchas** suscritas |

---

## Alcance: lo que está y lo que no

**Está, y se demuestra en vivo:**
backend completo de dos microservicios · seguridad real con AWS Cognito (JWKS, roles y propiedad) ·
214 pruebas con cobertura al 100 % · sistema contenedorizado que levanta con un comando · colección
de Postman de punta a punta · 11 ADR · modelo de negocio con números.

**No está, y está dicho de frente:**

| Qué falta | Por qué | Dónde se explica |
|---|---|---|
| Despliegue real en AWS | Decisión consciente: una demo que depende de la red del aula puede no ocurrir. La arquitectura está diseñada y costeada | [ADR-011](01-analisis/adr/ADR-011-despliegue-local-y-arquitectura-objetivo.md) |
| Integración continua | Deuda técnica reconocida, con fecha en la hoja de ruta | [GITFLOW.md §8](01-analisis/GITFLOW.md) · [PROPUESTA-TECNOLOGICA.md §5](05-emprendimiento/PROPUESTA-TECNOLOGICA.md) |
| Migraciones versionadas (Flyway) | Idem: hoy se usa `ddl-auto=update` | [PROPUESTA-TECNOLOGICA.md §5](05-emprendimiento/PROPUESTA-TECNOLOGICA.md) |
| Pagos en línea | Fase 2 del modelo de negocio | [PLAN-FINANCIERO.md](05-emprendimiento/PLAN-FINANCIERO.md) |
| Torneos con cupo distinto de potencia de dos | Simplificación deliberada, con ruta de evolución trazada | [ADR-006](01-analisis/adr/ADR-006-cuadro-potencia-de-dos.md) |

---

## Documentación técnica previa (sin modificar)

- [`README.md`](../README.md) — arquitectura, endpoints, logging, Cognito, cobertura
- [`docs/DOCUMENTACION.md`](../docs/DOCUMENTACION.md) — referencia técnica del código
- [`docs/MODELO-ER.md`](../docs/MODELO-ER.md) — modelo entidad-relación de las dos bases
- [`docs/DEMO.md`](../docs/DEMO.md) — guion operativo de la demo
- [`docs/coverage/`](../docs/coverage) — capturas de cobertura del IDE
