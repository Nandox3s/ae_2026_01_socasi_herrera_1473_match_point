# MatchPoint · Business Model Canvas

**Criterio 5.1 de la rúbrica (/4)** — capacidad para plantear el proyecto como una idea de negocio
utilizando el Business Model Canvas.

> **Nota sobre las cifras:** los datos de mercado y las proyecciones de este documento son
> **estimaciones del equipo** construidas a partir de observación directa de canchas del norte de
> Quito y de precios públicos de alquiler. No provienen de un estudio de mercado formal. Se
> declaran como supuestos, con su origen, para que puedan discutirse.

---

## 1. El problema, antes del canvas

Un canvas sin problema es un ejercicio de relleno. El de MatchPoint es concreto y se puede
observar en cualquier cancha de barrio de Quito un sábado:

**Del lado del dueño de la cancha:**
- Reserva por WhatsApp y cuaderno. **Doble reserva del mismo horario** con discusión en la puerta.
- No sabe cuáles son sus horas muertas, así que no puede fijar precios distintos por franja.
- Cobra en efectivo y sin registro: no tiene historial que mostrar si necesita un crédito.

**Del lado del jugador:**
- Para saber si hay cancha libre a las 8 de la noche del sábado hay que llamar a cinco números.
- Los torneos de barrio se llevan en una hoja impresa que se pierde, y nadie sabe cuándo juega su
  equipo hasta que alguien lo escribe en el grupo.

**El costo del problema:** una cancha con **30 % de horas muertas evitables** a 12 USD la hora, con
14 horas útiles al día, deja de facturar alrededor de **1 500 USD al mes**. Ese número es la razón
por la que un dueño de cancha pagaría por esto.

---

## 2. El canvas

```
┌──────────────────┬──────────────────┬──────────────────────┬──────────────────┬──────────────────┐
│ 8. SOCIOS CLAVE  │ 7. ACTIVIDADES   │ 2. PROPUESTA DE      │ 4. RELACIÓN CON  │ 1. SEGMENTOS DE  │
│                  │    CLAVE         │    VALOR             │    EL CLIENTE    │    CLIENTE       │
│ • Dueños de      │                  │                      │                  │                  │
│   complejos      │ • Desarrollo y   │ PARA EL DUEÑO:       │ • Onboarding     │ • Dueños de      │
│   (aliados       │   operación de   │ "Tu cancha se        │   asistido       │   canchas de     │
│   fundadores)    │   la plataforma  │  llena sola y no     │   (1ª visita     │   barrio         │
│ • Ligas y        │ • Captación de   │  vuelves a tener     │   presencial)    │   (1–3 canchas)  │
│   organizadores  │   canchas        │  dos reservas en     │ • Autoservicio   │ • Complejos      │
│   barriales      │   (venta en      │  la misma hora"      │   en la app      │   medianos       │
│ • AWS            │   terreno)       │                      │ • Soporte por    │   (4–8 canchas)  │
│ • Pasarela de    │ • Soporte y      │ PARA EL JUGADOR:     │   WhatsApp       │ • Jugadores      │
│   pagos local    │   éxito del      │ "Ves qué cancha      │ • Reportes       │   amateur        │
│   (fase 2)       │   cliente        │  está libre HOY y    │   mensuales      │   18–40 años     │
│ • Marcas         │ • Producto y     │  la reservas en 30   │   automáticos    │ • Organizadores  │
│   deportivas     │   analítica      │  segundos"           │                  │   de torneos     │
│   (fase 3)       │                  │                      │                  │   barriales      │
│                  ├──────────────────┤ PARA EL              ├──────────────────┤                  │
│                  │ 6. RECURSOS      │ ORGANIZADOR:         │ 3. CANALES       │                  │
│                  │    CLAVE         │ "Tu torneo se        │                  │                  │
│                  │                  │  arma solo: cuadro,  │ • Venta directa  │                  │
│                  │ • Plataforma     │  avance de ganadores │   en cancha      │                  │
│                  │   (2 microser-   │  y campeón, sin      │ • App móvil      │                  │
│                  │   vicios, API)   │  planillas"          │   Android        │                  │
│                  │ • Red de canchas │                      │ • Instagram y    │                  │
│                  │   afiliadas      │ DIFERENCIAL:         │   TikTok local   │                  │
│                  │   (efecto red)   │ Reservas + torneos   │ • Referidos      │                  │
│                  │ • Datos de       │ en un solo producto. │   entre dueños   │                  │
│                  │   ocupación      │ Nadie más lo une.    │ • Ligas          │                  │
│                  │ • Equipo técnico │                      │   barriales      │                  │
├──────────────────┴──────────────────┴──────────┬───────────┴──────────────────┴──────────────────┤
│ 9. ESTRUCTURA DE COSTOS                        │ 5. FUENTES DE INGRESO                           │
│                                                │                                                 │
│ FIJOS (mensual):                               │ 1. SUSCRIPCIÓN SaaS al dueño de cancha          │
│  • Infraestructura AWS ...........  127        │     Básico  19 USD/mes · 1 cancha               │
│  • Equipo (2 socios, fase 1) .....    0        │     Pro     39 USD/mes · hasta 3 canchas        │
│  • Dominio, herramientas .........   25        │     Multi   79 USD/mes · hasta 8 canchas        │
│  • Contabilidad y legal ..........   80        │  → INGRESO PRINCIPAL Y RECURRENTE               │
│                                                │                                                 │
│ VARIABLES:                                     │ 2. COMISIÓN por reserva pagada en línea         │
│  • Comisión de pasarela (4,5 %)                │     5 % del valor · desde fase 2                │
│  • Adquisición por cancha (~35 USD)            │                                                 │
│  • Soporte (crece con la base)                 │ 3. TORNEOS: 1 USD por equipo inscrito           │
│                                                │     (organizador o equipo)                      │
│ ESCALABLE: el costo marginal de la cancha      │                                                 │
│ número 101 es prácticamente cero.              │ 4. DESTACADOS en el catálogo · 10 USD/mes       │
└────────────────────────────────────────────────┴─────────────────────────────────────────────────┘
```

---

## 3. Los nueve bloques, en detalle

### 1 · Segmentos de cliente

Dos lados de un mercado, y hay que ser claro sobre **quién paga**:

| Segmento | Tamaño estimado (Quito) | ¿Paga? | Qué quiere |
|---|---|---|---|
| **Dueño de cancha de barrio** (1–3 canchas) | ~400 | **Sí — cliente principal** | Llenar horas muertas y dejar de perder reservas |
| **Complejo mediano** (4–8 canchas) | ~60 | **Sí — mayor ticket** | Administración centralizada y reportes |
| **Jugador amateur** 18–40 años | ~25 000 | No paga suscripción; paga la reserva | Encontrar cancha libre hoy, sin llamar a nadie |
| **Organizador de torneo barrial** | ~150 al año | **Sí — por evento** | Cuadro que se arma y avanza solo |

> **La estrategia de entrada es el lado del dueño**, no el del jugador. Es el que tiene un dolor
> cuantificable en dinero y el que trae el inventario. Sin canchas en la plataforma, la app no le
> sirve a ningún jugador; con canchas, los jugadores llegan solos.

### 2 · Propuesta de valor

| Segmento | Promesa en una frase | Cómo la cumple el producto **hoy** |
|---|---|---|
| Dueño de cancha | *"Tu cancha se llena sola y no vuelves a tener dos reservas en la misma hora"* | Validación de solapamiento en el motor (RN-01), catálogo público sin login, control de propiedad por dueño |
| Jugador | *"Ves qué cancha está libre HOY y la reservas en 30 segundos"* | `GET /courts/available` con filtro por sector, deporte y franja horaria |
| Organizador | *"Tu torneo se arma solo: cuadro, avance de ganadores y campeón"* | Generación automática del cuadro, avance del ganador y cierre con campeón (RF-26 a RF-30) |

**El diferencial no es reservar canchas.** Eso ya existe. Es **unir reservas y torneos en un solo
producto**: la misma cancha que se alquila entre semana es la sede del torneo del fin de semana, y
el organizador no tiene que hablar con nadie para bloquear los horarios. Ningún competidor local
cubre las dos cosas.

### 3 · Canales

| Canal | Para quién | Fase | Costo estimado |
|---|---|---|---|
| **Venta directa en cancha** | Dueños | 1 | Tiempo del equipo. **Es el canal que funciona**: se cierra en la cancha, con el dueño mirando la app |
| App móvil Android | Jugadores | 1 | Desarrollo interno |
| Instagram y TikTok locales | Jugadores | 1 | 100–200 USD/mes en fase 2 |
| Referidos entre dueños | Dueños | 2 | 1 mes gratis por referido efectivo |
| Alianza con ligas barriales | Organizadores | 2 | Comisión compartida |

### 4 · Relación con el cliente

| Momento | Qué se hace | Por qué |
|---|---|---|
| **Alta del dueño** | Visita presencial: se cargan sus canchas y horarios con él delante | El dueño de cancha de barrio no completa formularios. Esta hora de trabajo **es** el producto en la fase 1 |
| **Uso diario** | Autoservicio total en la app | No escala de otra forma |
| **Soporte** | WhatsApp, respuesta el mismo día | Es el canal que ya usa y en el que confía |
| **Retención** | Reporte mensual automático: horas ocupadas, horas muertas, ingreso estimado | Le muestra en números lo que gana con la plataforma; es el argumento de renovación |

### 5 · Fuentes de ingreso

| # | Fuente | Modelo | Fase | Peso proyectado al año 2 |
|---|---|---|---|---:|
| 1 | **Suscripción SaaS** | 19 / 39 / 79 USD por mes según número de canchas | 1 | **72 %** |
| 2 | **Comisión por reserva pagada en línea** | 5 % del valor | 2 | 18 % |
| 3 | **Torneos** | 1 USD por equipo inscrito | 1 | 6 % |
| 4 | **Destacados en el catálogo** | 10 USD/mes | 3 | 4 % |

**Por qué suscripción y no solo comisión:** la comisión suena más justa —se cobra cuando el cliente
gana— pero depende de que el pago pase por la plataforma, y en el mercado real el 90 % del pago es
en efectivo en la cancha. La suscripción da **ingreso recurrente y predecible desde el primer mes**
sin depender de un cambio de hábito que no controlamos. La comisión entra en fase 2 como
complemento, cuando el pago en línea ya sea una opción real.

**Prueba de sensatez del precio:** 19 USD/mes son **1,6 horas de alquiler**. Si la plataforma
recupera dos horas muertas al mes, ya se pagó sola. Es la frase con la que se cierra la venta.

### 6 · Recursos clave

| Recurso | Tipo | Por qué es clave |
|---|---|---|
| **La plataforma** | Intelectual | Dos microservicios, API documentada, 214 pruebas, 100 % de cobertura. Ya construida |
| **La red de canchas afiliadas** | Relacional | **El activo más difícil de copiar.** Un competidor puede clonar el software en tres meses; conseguir 100 canchas afiliadas toma un año |
| **Los datos de ocupación** | Intelectual | Con dos años de historia se puede sugerir precio por franja y predecir demanda. Es la base del producto de fase 3 |
| **Equipo técnico** | Humano | Dos desarrolladores capaces de operar y evolucionar el sistema |
| **Infraestructura AWS** | Físico | Costo conocido y escalable: 127 USD/mes en la arquitectura completa |

### 7 · Actividades clave

| Actividad | Fase 1 | Fase 2–3 |
|---|---|---|
| **Captación de canchas en terreno** | **Es la actividad principal.** Sin inventario no hay producto | Se vuelve parcialmente autoservicio |
| Desarrollo y operación | Alta: completar la app móvil y los pagos | Media: mantenimiento y evolución |
| Soporte y éxito del cliente | Directo, de los socios | Con personal dedicado |
| Producto y analítica | Baja | **Alta**: precios sugeridos, predicción de demanda |

### 8 · Socios clave

| Socio | Qué aporta | Qué recibe |
|---|---|---|
| **Dueños fundadores** (las primeras 10 canchas) | Inventario inicial, validación y referidos | **6 meses gratis** a cambio de retroalimentación semanal |
| **Ligas y organizadores barriales** | Acceso a cientos de jugadores en bloque | Herramienta de torneo gratuita para su liga |
| **AWS** | Infraestructura | Pago por uso; posible crédito de programa para *startups* |
| **Pasarela de pagos local** (Datafast, Kushki, PayPhone) | Cobro en línea en fase 2 | Comisión por transacción |
| **Marcas deportivas** | Patrocinio de torneos en fase 3 | Visibilidad ante un público segmentado |

### 9 · Estructura de costos

**Costos fijos mensuales, fase 1** (los dos socios sin sueldo, reinvirtiendo):

| Concepto | USD/mes |
|---|---:|
| Infraestructura AWS (arquitectura completa) | 127 |
| Dominio y herramientas | 25 |
| Contabilidad y obligaciones legales | 80 |
| **Total** | **232** |

**Costos variables:** comisión de pasarela (≈ 4,5 %), adquisición por cancha (≈ 35 USD entre
transporte, tiempo y material) y soporte, que crece con la base instalada.

**La economía es de software, y es lo que hace atractivo el negocio:** el costo marginal de sumar
la cancha número 101 es prácticamente cero, mientras que su ingreso es el mismo que el de la
primera. Todo cliente por encima del punto de equilibrio es margen casi puro.

---

## 4. Validación del modelo

### Punto de equilibrio

Con un ticket promedio de **26 USD/mes** (mezcla proyectada de los tres planes) y **232 USD** de
costo fijo:

**Punto de equilibrio ≈ 9 canchas suscritas.**

Nueve dueños de cancha en una ciudad con unos 460 establecimientos es un **2 %** del mercado
direccionable. Es la cifra que hace creíble el resto del plan.

### Métricas que se van a seguir

| Métrica | Meta año 1 | Por qué importa |
|---|---|---|
| Canchas activas | 40 | Es el inventario: el numerador de todo |
| Ingreso recurrente mensual (MRR) | 1 040 USD | La salud real de un SaaS |
| Abandono mensual (*churn*) | < 5 % | Por encima de eso, el crecimiento no compensa la fuga |
| Reservas por cancha al mes | > 25 | Prueba de que el dueño **usa** el producto y no solo lo paga |
| Costo de adquisición (CAC) | < 35 USD | Debe recuperarse en menos de 2 meses de suscripción |
| Relación LTV/CAC | > 3 | Umbral estándar de un negocio SaaS sano |

### Riesgos y cómo se mitigan

| Riesgo | Impacto | Mitigación |
|---|---|---|
| **El dueño no adopta tecnología** | Alto | Alta asistida presencial; la app resuelve **su** dolor, no el nuestro; WhatsApp como canal de soporte |
| Pocas canchas ⇒ pocos jugadores ⇒ pocas canchas (arranque en frío) | Alto | Concentrarse en **un solo sector de la ciudad** hasta tener densidad suficiente, en lugar de dispersarse |
| Un competidor con más capital | Medio | La red de canchas afiliadas y los datos históricos son la barrera, no el código |
| Estacionalidad (lluvias, vacaciones) | Medio | La suscripción amortigua la caída de reservas; se contempla plan anual con descuento |
| Dependencia de una sola nube | Bajo | Todo está contenedorizado: migrar de proveedor es cambiar el destino del despliegue |

---

## 5. De la rúbrica al producto: qué existe hoy

Un canvas cuyas promesas no tienen respaldo técnico es una presentación. Este las tiene:

| Promesa del canvas | Estado | Evidencia |
|---|---|---|
| "No vuelves a tener dos reservas en la misma hora" | **Funcionando** | RN-01, validación de solapamiento, 12 pruebas en `ReservationServiceTest` |
| "Ves qué cancha está libre hoy" | **Funcionando** | `GET /matchpoint/courts/available`, público y con filtros |
| "Tu torneo se arma solo" | **Funcionando** | RF-26 a RF-30, cuadro completo y avance automático |
| "Cada dueño administra solo lo suyo" | **Funcionando** | Autorización por propiedad ([ADR-007](../01-analisis/adr/ADR-007-autorizacion-en-dos-capas.md)) |
| "Reporte mensual de horas muertas" | Pendiente | El dato ya se registra en `reservations` y `audit_log`; falta la consulta y la vista |
| "Pago en línea" | Pendiente, fase 2 | Requiere integrar la pasarela |
| App móvil Android | **En desarrollo** | Consume esta misma API a través del gateway |
