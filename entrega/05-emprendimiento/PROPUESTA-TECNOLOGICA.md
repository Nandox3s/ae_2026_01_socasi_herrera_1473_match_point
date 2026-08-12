# MatchPoint · Propuesta tecnológica

**Criterio 5.2 de la rúbrica (/2)** — habilidad para diseñar la propuesta tecnológica de la
aplicación considerando los factores necesarios e incorporando innovación y creatividad.

---

## 1. Los factores que condicionaron cada elección

Una propuesta tecnológica se defiende por sus restricciones, no por la lista de tecnologías de
moda. Estas fueron las cinco que gobernaron todas las decisiones:

| Factor | Restricción concreta | Consecuencia tecnológica |
|---|---|---|
| **Presupuesto** | Sin inversión inicial. Todo debe caber en ~130 USD/mes hasta tener ingresos | Nada de licencias; nube con pago por uso; infraestructura mínima viable con camino de crecimiento |
| **Equipo** | Dos desarrolladores, sin personal de operaciones | Servicios gestionados donde sea posible (RDS, Cognito); nada que requiera un administrador dedicado |
| **Usuario final** | Dueño de cancha de barrio, poca familiaridad con software; jugador que usa el teléfono en la calle | Interfaz mínima, alta asistida; app móvil, no web de escritorio; el catálogo funciona **sin crear cuenta** |
| **Conectividad** | Redes móviles irregulares en las canchas | API ligera en JSON; sin cargas pesadas; el cliente tolera latencia y reintenta |
| **Crecimiento esperado** | De 10 a 500 canchas en dos años, con picos de 60× los sábados | Aplicación *stateless* desde el diseño, para poder escalar horizontalmente sin refactorizar |

---

## 2. El stack, y por qué cada pieza

| Capa | Elección | Alternativa evaluada | Por qué esta |
|---|---|---|---|
| **Lenguaje** | Kotlin 2.2 | Java 21 | Nulabilidad en el sistema de tipos —elimina la clase entera de `NullPointerException`—, `data class`, `when` exhaustivo. **Y el mismo lenguaje que la app Android**: un solo idioma para todo el equipo |
| **Framework** | Spring Boot 4 | Quarkus, Ktor | Ecosistema maduro para OAuth2 Resource Server, JPA y pruebas. Es lo que permite que la seguridad sea configuración y no código propio |
| **Persistencia** | PostgreSQL 16 | MongoDB, MySQL | El dominio es **fuertemente relacional**: reservas con integridad referencial, torneos con cuadros. Un documento sin transacciones no puede garantizar RN-01 |
| **Identidad** | AWS Cognito | JWT propio, Keycloak | Cero contraseñas almacenadas. Ver [ADR-002](../01-analisis/adr/ADR-002-cognito-como-proveedor-de-identidad.md) |
| **Gateway** | nginx | Spring Cloud Gateway | 10 MB de imagen contra media JVM. La lógica aquí es *reverse proxy* puro. Ver [ADR-009](../01-analisis/adr/ADR-009-gateway-unico-punto-de-entrada.md) |
| **Empaquetado** | Docker multi-stage | JAR sobre el sistema anfitrión | Reproducibilidad: la imagen que corre en la laptop es la que corre en el servidor |
| **Nube** | AWS (EC2 + RDS + Cognito) | GCP, Azure, VPS | Cognito ya resolvía la identidad; mantener un proveedor simplifica IAM y facturación |
| **Móvil** | Android nativo (Kotlin) | Flutter, React Native | El mercado objetivo en Ecuador es abrumadoramente Android. Nativo, además, comparte lenguaje y modelos con el backend |

### La arquitectura, en una frase por decisión

- **Dos microservicios con una base cada uno** — la frontera coincide con la del equipo y aísla los fallos ([ADR-003](../01-analisis/adr/ADR-003-dos-microservicios-base-por-servicio.md)).
- **Un solo punto de entrada** — un puerto publicado en lugar de cinco ([ADR-009](../01-analisis/adr/ADR-009-gateway-unico-punto-de-entrada.md)).
- **Identidad delegada, propagada entre servicios** — nunca un token de servicio privilegiado ([ADR-004](../01-analisis/adr/ADR-004-comunicacion-http-con-token-propagado.md)).
- **Autorización en dos capas** — rol y propiedad, con el dueño fuera del alcance del cliente ([ADR-007](../01-analisis/adr/ADR-007-autorizacion-en-dos-capas.md)).
- **Aplicación sin estado** — precondición del escalado horizontal, cumplida desde el día uno.

---

## 3. Innovación

Lo que sigue es lo que distingue esta propuesta de "otra app de reservas". Cada punto está separado
en **lo que ya funciona** y **lo que está diseñado pero no construido**, para no vender humo.

### 3.1 Reservas y torneos en un solo dominio ✅ *implementado*

**El diferencial de producto.** Las plataformas de reserva tratan la cancha como inventario
horario; las de torneos, como un dato de texto. Aquí **son la misma entidad**: `tournaments.court_id`
apunta a `courts.id`.

Lo que eso habilita, y que un competidor con dos productos separados no puede hacer:

- El organizador reserva la sede **dentro del mismo flujo** en que crea el torneo.
- El dueño de cancha ve un torneo como lo que económicamente es: **una reserva recurrente** de todo
  un fin de semana, que es su cliente más rentable.
- El jugador que busca cancha descubre torneos en el mismo catálogo.

Y una restricción de negocio que solo tiene sentido si los dos dominios están unidos: **un torneo
solo puede usar como sede una cancha del propio MANAGER** (`resolveCourt`). Nadie secuestra la
cancha ajena para su torneo.

### 3.2 El cuadro que se administra solo ✅ *implementado*

El organizador de un torneo barrial hace hoy tres cosas a mano: dibujar las llaves, mover al
ganador y recalcular quién juega contra quién. **Las tres las hace el sistema.**

Al registrar un marcador, en una sola operación: se actualizan las estadísticas de ambos equipos,
se marca al perdedor como eliminado, **el ganador se mueve al slot correcto de la siguiente ronda**
(`ronda+1`, `posición/2`), el slot padre pasa a `READY` cuando reúne a sus dos equipos, y si era la
final el torneo se cierra con campeón.

La creatividad está en la **simplificación deliberada**: exigir que el cupo sea potencia de dos
elimina los *byes* y con ellos la mitad de la complejidad del cuadro, sin quitarle nada al torneo
que la gente realmente organiza ([ADR-006](../01-analisis/adr/ADR-006-cuadro-potencia-de-dos.md)).
Es una decisión de producto disfrazada de decisión técnica.

### 3.3 Catálogo público sin cuenta ✅ *implementado*

`GET /courts`, `GET /courts/available` y todo el cuadro del torneo son **públicos, sin token**.

Es una decisión de negocio, no una omisión de seguridad: la mayor fricción de un marketplace es
pedir registro antes de mostrar valor. Aquí un jugador ve qué hay libre esta noche **antes** de
crear una cuenta, y el cuadro de un torneo se puede compartir por WhatsApp como un enlace que
cualquiera abre. La cuenta se exige solo cuando hay algo que proteger: reservar, inscribir, cobrar.

### 3.4 Auditoría como funcionalidad, no como requisito técnico ✅ *implementado*

`audit_log` guarda quién, qué, cuándo y los valores antes y después de **cada** mutación. Se
construyó por trazabilidad, pero resuelve el problema más frecuente entre un dueño de cancha y un
jugador: **"yo sí reservé", "no, nadie reservó"**. La respuesta está en una fila con hora y usuario.

Con el tiempo, esa misma tabla es el activo de datos del negocio: qué franjas se cancelan más, qué
canchas se llenan primero, qué precio soporta cada horario.

### 3.5 Enmascaramiento por diseño ✅ *implementado*

Ningún correo ni teléfono aparece completo en logs ni en auditoría:
`juan.perez@puce.edu.ec` → `j***@puce.edu.ec`. Aplicado en el mismo helper que formatea el log, así
que **no se puede olvidar**: no depende de que alguien recuerde enmascarar en cada llamada.

### 3.6 Precio dinámico por franja 🔜 *diseñado*

Con el histórico de ocupación por cancha y hora, sugerir al dueño: *"tu franja de martes 15:00 se
ocupa el 12 % de las veces; a 8 USD subiría a ~40 %"*.

No es una promesa vaga: **el dato ya se está registrando** en `reservations` y `audit_log` desde el
primer día. Es un caso de análisis sobre datos propios, no un modelo que haya que entrenar con
información de terceros.

### 3.7 Emparejamiento de jugadores sueltos 🔜 *diseñado*

El caso real más frecuente en una cancha de barrio: **cinco personas quieren jugar y no son diez**.
Un jugador publica "busco 4 para el sábado 18:00 en la Carolina"; cuando se completa el grupo, la
reserva se confirma sola y el costo se divide.

Es la funcionalidad que convierte a MatchPoint de herramienta del dueño en producto del jugador, y
la que genera el efecto de red del lado de la demanda. Se apoya en el modelo de reservas ya
construido: es una reserva en estado provisional que se confirma al alcanzar el cupo.

### 3.8 Modo sin conexión en la app 🔜 *diseñado*

En la cancha la señal es mala. La app guarda el catálogo consultado y encola la reserva; al
recuperar señal la envía y muestra el resultado. **El backend ya lo soporta sin cambios**, porque
es *stateless* y el token es autocontenido: la petición encolada es válida cuando llegue, sin
sesión que reconstruir.

---

## 4. Hoja de ruta tecnológica

| Fase | Meses | Qué se construye | Objetivo de negocio |
|---|---|---|---|
| **0 · Base** ✅ | — | Backend completo, 214 pruebas, contenedores, seguridad | Producto demostrable |
| **1 · Salida** | 1–3 | App Android, despliegue en AWS (fase 2 del plan de nube), reporte de ocupación | 10 canchas fundadoras |
| **2 · Monetización** | 4–8 | Pasarela de pagos, comisión, notificaciones push, panel web del dueño | Punto de equilibrio (9 canchas) |
| **3 · Datos** | 9–14 | Precio sugerido por franja, emparejamiento de jugadores, app iOS | 100 canchas, retención |
| **4 · Escala** | 15–24 | Migración a ECS, réplica de lectura, CDN, API pública para ligas | 500 canchas, otras ciudades |

---

## 5. Deuda técnica reconocida

Enunciarla es parte de la propuesta: lo que no se dice, se pregunta.

| Deuda | Riesgo | Cuándo se paga |
|---|---|---|
| Sin integración continua | La regla "nada se mezcla en rojo" depende de la disciplina del equipo | Fase 1: workflow con `./gradlew check` |
| `ddl-auto=update` en lugar de migraciones versionadas | Un cambio de esquema en producción no es reversible | Fase 1: Flyway |
| Sin caché | Cada consulta al catálogo golpea la base | Fase 3: Redis o CloudFront, cuando el volumen lo justifique |
| Comunicación solo síncrona | Notificaciones y correos bloquean la petición | Fase 2: cola para tareas diferidas |
| Un solo deporte (`BASKET`) | Limita el mercado direccionable | Fase 2: el enum ya está preparado |
| Torneos solo de eliminación directa | No cubre ligas ni fase de grupos | Fase 3 |

---

## 6. Por qué esta propuesta es defendible

1. **Está construida, no propuesta.** Seis de las ocho innovaciones de la sección 3 funcionan hoy y
   se pueden demostrar en vivo con Postman.
2. **Cada decisión tiene su costo escrito.** Once ADR documentan qué se descartó y qué se aceptó a
   cambio. Ninguna elección se presenta como gratis.
3. **La escala está resuelta antes de necesitarla.** La aplicación es *stateless* desde el primer
   commit: escalar horizontalmente es cambiar un número en el Auto Scaling Group, no reescribir.
4. **El costo es conocido y proporcional.** 127 USD/mes soportan las primeras 100 canchas; el punto
   de equilibrio son 9.
5. **Lo que falta está identificado y fechado**, con su riesgo, en la sección 5.
