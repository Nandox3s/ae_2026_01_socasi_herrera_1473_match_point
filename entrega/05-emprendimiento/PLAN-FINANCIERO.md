# MatchPoint · Planificación financiera

**Criterio 5.3 de la rúbrica (/2)** — capacidad para hacer la planificación financiera del proyecto.

> **Cómo leer este documento.** Todas las cifras son **proyecciones construidas por el equipo** a
> partir de precios de lista de AWS, precios observados de alquiler de canchas en el norte de Quito
> y supuestos de captación declarados en §2. No son resultados históricos ni provienen de un
> estudio de mercado formal. Los supuestos están todos explícitos para que se puedan discutir y
> reemplazar. Moneda: **USD** (moneda oficial del Ecuador).

---

## 1. Inversión inicial

El proyecto es **bootstrapped**: no requiere levantar capital, y esa es una de sus características
más fuertes.

| Concepto | USD | Nota |
|---|---:|---|
| Constitución legal y RUC | 450 | Sociedad simplificada |
| Dominio (2 años) + correo corporativo | 90 | |
| Cuenta de desarrollador de Google Play | 25 | Pago único |
| Material de venta (impresos, adhesivos QR para canchas) | 180 | Para la captación en terreno |
| Reserva para asesoría contable inicial | 250 | |
| Equipos de desarrollo | **0** | Ya disponibles |
| Licencias de software | **0** | Todo el stack es de código abierto |
| **Total inversión inicial** | **995** | |
| **Capital de trabajo requerido** *(§5)* | **570** | Cubre el flujo negativo de los meses 1 a 7 |
| **Capital total necesario** | **≈ 1 565** | Aportado por los dos socios |

Que la inversión sea de cuatro cifras bajas no es casualidad: el stack es de código abierto, la
infraestructura se paga por uso y el producto ya está construido.

---

## 2. Supuestos del modelo

Cambiar cualquiera de estos números cambia todo el plan. Por eso van al frente y no escondidos en
una nota al pie.

| # | Supuesto | Valor | De dónde sale |
|---|---|---|---|
| S1 | Canchas de básquet alquilables en Quito | ~460 | Conteo propio sobre mapas y recorrido de sectores |
| S2 | Precio medio de alquiler por hora | 12 USD | Precios observados en canchas del norte |
| S3 | Ticket medio de suscripción, año 1 | 26 USD/mes | Mezcla 60 % Básico (19) / 35 % Pro (39) / 5 % Multi (79) |
| S4 | Ticket medio, años 2 y 3 | 28 y 30 USD/mes | Migración gradual a planes superiores |
| S5 | Costo de adquisición por cancha (CAC) | 35 USD | Transporte, tiempo de visita y material |
| S6 | Abandono mensual (*churn*) | 3 % | Supuesto conservador para SaaS con contrato mensual |
| S7 | Reservas gestionadas por cancha al mes | 25 | Meta de uso mínimo del producto |
| S8 | Reservas pagadas en línea | 0 % (año 1) · 20 % (año 2) · 35 % (año 3) | El pago en efectivo es la norma; el cambio de hábito es gradual |
| S9 | Comisión cobrada sobre pago en línea | 5 % | Definida en el Business Model Canvas |
| S10 | Costo de la pasarela de pagos | 3,5 % + IVA | Tarifas publicadas de pasarelas locales |
| S11 | Infraestructura AWS | 127 → 200 → 400 USD/mes | Fases 2 y siguientes del plan de nube |
| S12 | Los socios **no cobran sueldo** en el año 1 | 0 USD | Reinversión total. Ver §7, que cuantifica lo que esto oculta |

---

## 3. Estructura de costos fijos mensuales (año 1)

| Concepto | USD/mes |
|---|---:|
| Infraestructura AWS *(arquitectura completa: ALB, 2× EC2, RDS Multi-AZ, NAT, ECR, CloudWatch)* | 127 |
| Dominio, correo y herramientas | 25 |
| Contabilidad y obligaciones tributarias | 80 |
| **Total fijo** | **232** |

Costos variables: adquisición (35 USD por cancha nueva), marketing digital (100 USD/mes desde el
mes 7) y, desde el año 2, la comisión de la pasarela.

---

## 4. Proyección de ingresos

### 4.1 Crecimiento de la base instalada

| Mes | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 |
|---|--:|--:|--:|--:|--:|--:|--:|--:|--:|--:|--:|--:|
| **Canchas activas** | 3 | 6 | 9 | 12 | 16 | 20 | 24 | 28 | 31 | 34 | 37 | **40** |
| **MRR (USD)** | 78 | 156 | 234 | 312 | 416 | 520 | 624 | 728 | 806 | 884 | 962 | **1 040** |

Cuarenta canchas al cierre del año 1 son el **8,7 %** de las ~460 estimadas en Quito. Es una meta
de captación de terreno —unas 4 altas al mes con dos personas trabajando medio tiempo—, no una
proyección de crecimiento viral.

### 4.2 Ingresos por año y por fuente

| Fuente | Año 1 | Año 2 | Año 3 |
|---|---:|---:|---:|
| Suscripción SaaS | 6 760 | 32 000 | 90 000 |
| Comisión por pago en línea *(bruta, 5 %)* | 0 | 3 420 | 15 750 |
| Torneos (1 USD por equipo inscrito) | 240 | 700 | 2 000 |
| Destacados en el catálogo | 0 | 0 | 3 000 |
| **Total ingresos** | **7 000** | **36 120** | **110 750** |
| Canchas activas al cierre | 40 | 150 | 350 |
| MRR al cierre | 1 040 | 4 200 | 10 500 |

---

## 5. Flujo de caja del año 1, mes a mes

Es el año que decide si el proyecto existe. Se detalla mes a mes porque el dato que importa —cuánto
dinero hay que tener guardado antes de empezar— solo aparece aquí.

| Mes | Canchas | Ingresos | Costo fijo | Adquisición | Marketing | Flujo del mes | **Acumulado** |
|---|--:|--:|--:|--:|--:|--:|--:|
| 1 | 3 | 78 | 232 | 105 | 0 | −259 | **−259** |
| 2 | 6 | 156 | 232 | 105 | 0 | −181 | **−440** |
| 3 | 9 | 234 | 232 | 105 | 0 | −103 | **−543** |
| 4 | 12 | 312 | 232 | 105 | 0 | −25 | **−568** ← punto más bajo |
| 5 | 16 | 416 | 232 | 140 | 0 | +44 | −524 |
| 6 | 20 | 520 | 232 | 140 | 0 | +148 | −376 |
| 7 | 24 | 624 | 232 | 140 | 100 | +152 | −224 |
| 8 | 28 | 728 | 232 | 140 | 100 | +256 | **+32** ← acumulado positivo |
| 9 | 31 | 806 | 232 | 140 | 100 | +334 | +366 |
| 10 | 34 | 884 | 232 | 140 | 100 | +412 | +778 |
| 11 | 37 | 962 | 232 | 140 | 100 | +490 | +1 268 |
| 12 | 40 | 1 040 | 232 | 140 | 100 | +568 | +1 836 |
| | | **6 760** | **2 784** | **1 540** | **600** | | |

*(La tabla omite los 240 USD de torneos, que se reciben de forma irregular y elevan el resultado
final del año a ≈ 2 076 USD.)*

**Tres lecturas de esta tabla:**

- **El capital de trabajo necesario son 570 USD**, en el mes 4. No 10 000, ni un inversionista.
- **El flujo mensual se vuelve positivo en el mes 5**, con 16 canchas.
- **El acumulado se recupera en el mes 8.** A partir de ahí el negocio se financia solo.

---

## 6. Estado de resultados proyectado

| | Año 1 | Año 2 | Año 3 |
|---|---:|---:|---:|
| **Ingresos** | **7 000** | **36 120** | **110 750** |
| Infraestructura AWS | (1 524) | (2 400) | (4 800) |
| Otros costos fijos | (1 260) | (1 260) | (1 500) |
| Comisión de la pasarela (3,5 %) | 0 | (2 394) | (11 025) |
| Adquisición de clientes | (1 540) | (4 900) | (8 750) |
| Marketing digital | (600) | (2 400) | (6 000) |
| Remuneraciones | **0** | (17 100) | (55 200) |
| **Total costos** | **(4 924)** | **(30 454)** | **(87 275)** |
| **Resultado del ejercicio** | **+2 076** | **+5 666** | **+23 475** |
| Margen sobre ingresos | 30 % | 16 % | 21 % |
| Resultado acumulado | +2 076 | +7 742 | +31 217 |

**Por qué el margen baja en el año 2 y vuelve a subir en el 3:** en el año 2 entran las
remuneraciones —los socios empiezan a cobrar 800 USD desde el mes 4 y se incorpora una persona de
soporte a medio tiempo— y esa carga se come el crecimiento. En el año 3 la base instalada ya es lo
bastante grande para absorberla, y el costo marginal de cada cancha nueva sigue siendo cercano a
cero. **Es la forma típica de la curva de un SaaS**, no un error de cálculo.

---

## 7. Lo que el año 1 no muestra

El resultado de +2 076 USD del año 1 es real **solo porque los socios no cobran** (supuesto S12).
Poner ese costo sobre la mesa es lo honesto:

| | USD |
|---|---:|
| Horas invertidas por los dos socios en el año 1 | ~2 080 h *(2 personas × 20 h/semana × 52 semanas)* |
| Valorizadas a 8 USD/hora | 16 640 |
| Resultado contable del año 1 | +2 076 |
| **Resultado económico real del año 1** | **−14 564** |

**La inversión real del proyecto no es dinero, es tiempo.** Y se recupera en el año 3, cuando las
remuneraciones ya están dentro del estado de resultados y este sigue siendo positivo. Un plan que
presenta el primer año como rentable sin decir esto está escondiendo su costo principal.

---

## 8. Punto de equilibrio

| Escenario | Costo fijo mensual | Ticket medio | **Canchas necesarias** |
|---|---:|---:|---:|
| Año 1, sin sueldos | 232 | 26 | **9** |
| Año 1, cubriendo también la adquisición | 372 | 26 | **15** |
| Con los dos socios cobrando 800 USD | 1 832 | 26 | **71** |
| Año 3, estructura completa | 7 273 | 30 | **243** |

Los tres primeros números son los que importan para decidir si empezar:

- **9 canchas** es el 2 % del mercado de Quito. Se alcanza en el mes 3.
- **15 canchas** —el equilibrio de caja real, incluyendo lo que cuesta conseguir cada cliente— se
  alcanza en el mes 5.
- **71 canchas** es cuándo el proyecto puede dejar de ser un trabajo de fin de semana. Ocurre a
  mitad del año 2.

---

## 9. Indicadores del negocio

| Indicador | Fórmula | Valor | Referencia sana |
|---|---|---:|---|
| **CAC** | Costo de adquisición por cancha | 35 USD | — |
| **Recuperación del CAC** | CAC ÷ ticket medio | **1,3 meses** | < 12 meses |
| **Vida media del cliente** | 1 ÷ churn mensual (3 %) | 33 meses | — |
| **LTV** | Ticket × vida media | 858 USD | — |
| **LTV / CAC** | | **24,5** | > 3 |
| **Margen bruto** | (Ingresos − costo de servir) ÷ ingresos | ~85 % | > 70 % en SaaS |
| **VAN a 3 años** (tasa 12 %) | | **≈ 21 800 USD** | > 0 |

La relación **LTV/CAC de 24,5** es alta porque la venta es directa y sin costo de canal: se cierra
en la propia cancha, con el dueño mirando la app. Es sostenible **mientras la captación sea en
terreno**; cuando haya que comprar tráfico digital el CAC subirá y ese indicador bajará hacia
valores más normales.

Una advertencia sobre la TIR: con una inversión inicial de 995 USD, la tasa interna de retorno
supera el 150 % y **no aporta información útil**. Cuando el denominador es tan pequeño, cualquier
resultado positivo produce una TIR espectacular. El indicador que sí manda aquí es el de §7: el
tiempo de los socios.

---

## 10. Análisis de sensibilidad

Qué pasa si los supuestos fallan. La columna que importa es la pesimista.

| Escenario | Captación | Churn | Canchas al cierre del año 1 | Resultado año 1 | ¿Sobrevive? |
|---|---|---|---:|---:|---|
| **Pesimista** | 2 altas/mes | 6 % | 18 | **−320 USD** | **Sí**, con 900 USD de capital de trabajo en vez de 570 |
| **Realista** *(el plan)* | 4 altas/mes | 3 % | 40 | +2 076 USD | Sí |
| **Optimista** | 6 altas/mes | 2 % | 65 | +5 900 USD | Sí, y adelanta la contratación al año 1 |

**El escenario pesimista no quiebra el proyecto**, solo lo alarga: con 18 canchas el negocio sigue
por encima del equilibrio operativo de 9. Ese es el argumento central de este plan financiero — la
estructura de costos es tan baja que el riesgo de ruina es mínimo.

### Las tres variables más peligrosas

| Variable | Si empeora un 50 % | Impacto | Mitigación |
|---|---|---|---|
| **Churn** (3 % → 6 %) | LTV cae de 858 a 429 USD | LTV/CAC baja a 12; sigue sano | Reporte mensual de ocupación como herramienta de retención; plan anual con descuento |
| **Ritmo de captación** (4 → 2 altas/mes) | Se alcanza el equilibrio 4 meses más tarde | Requiere 330 USD más de capital | Concentrarse en un solo sector hasta lograr densidad |
| **Precio** (26 → 19 USD) | El equilibrio sube de 9 a 13 canchas | Un mes más de retraso | Es el margen de negociación disponible; no compromete la viabilidad |

---

## 11. Uso de los fondos y decisiones de reinversión

| Prioridad | Destino | Cuándo | Monto estimado |
|---|---|---|---:|
| 1 | Reserva de caja equivalente a 3 meses de costo fijo | Al llegar a 20 canchas | 700 |
| 2 | Publicación de la app en Google Play y pasarela de pagos | Mes 6–8 | 400 |
| 3 | Marketing digital sostenido | Desde el mes 7 | 100/mes |
| 4 | Primer sueldo para los socios | Año 2, mes 4 | 800 c/u |
| 5 | Persona de soporte y ventas a medio tiempo | Año 2, mes 7 | 450/mes |
| 6 | Desarrollador adicional | Año 3 | 1 000/mes |

**Regla de reinversión:** no se contrata a nadie hasta que el MRR cubra **1,5 veces** el costo fijo
resultante. Es la disciplina que mantiene bajo el riesgo de ruina en el escenario pesimista.

---

## 12. Conclusión financiera

1. **Se necesitan menos de 1 600 USD para arrancar.** El stack es de código abierto, la
   infraestructura es de pago por uso y el producto ya está construido.
2. **El equilibrio operativo son 9 canchas** — el 2 % del mercado de Quito — y se alcanza en el
   mes 3. El equilibrio de caja, 15 canchas, en el mes 5.
3. **La caja se recupera en el mes 8** y el negocio se autofinancia desde ahí.
4. **El costo real del primer año es el tiempo de los socios**, unos 16 600 USD valorizados. Es la
   inversión verdadera y se recupera en el año 3, ya con sueldos dentro del estado de resultados.
5. **El escenario pesimista no quiebra el proyecto.** Con la mitad de la captación y el doble de
   abandono, el año 1 cierra en −320 USD: un mal año, no el final.
6. **El modelo escala con margen creciente.** El costo marginal de la cancha número 101 es
   prácticamente cero; su ingreso es idéntico al de la primera.
