# ADR-005 · Cerrar el flujo de reservas antes de empezar torneos

- **Estado:** Aceptado
- **Fecha:** 2026-08-02
- **Decide:** Josué Herrera, Fernando Socasi
- **Requerimientos afectados:** RF-11 … RF-20 (antes) · RF-21 … RF-30 (después)
- **Historias:** HU-05 … HU-08 antes de HU-09 … HU-12

## Contexto

MatchPoint tiene dos dominios independientes: **reservas** (canchas y franjas horarias) y
**torneos** (equipos y cuadro de eliminación). No comparten reglas y solo se tocan en un punto:
un torneo puede tener una cancha como sede, y esa relación es opcional.

Como no hay dependencia técnica fuerte entre ambos, se podía empezar por cualquiera, o por los dos
a la vez. Con dos semanas de desarrollo y dos personas, la pregunta real era: **¿qué pasa si nos
quedamos sin tiempo?**

Tres escenarios posibles al final del plazo:

- **A)** Reservas completo + torneos sin empezar → sistema con un flujo demostrable de punta a punta.
- **B)** Reservas al 60 % + torneos al 60 % → dos flujos que se rompen a mitad de camino. Nada demostrable.
- **C)** Torneos completo + reservas sin empezar → demostrable, pero sobre el dominio menos central.

El escenario B es el peor de los tres y es exactamente al que se llega desarrollando en paralelo
sin priorizar.

## Decisión

**Se cierra el flujo de reservas de punta a punta —publicar cancha, buscar disponibilidad,
reservar, cancelar— antes de escribir la primera línea de torneos.**

El criterio de "cerrado" es explícito y verificable: la carpeta `3. reservas` de la colección de
Postman corre completa en verde, incluidos los casos de `409` por solapamiento, `409` por cancha
inactiva y `403` por propiedad.

Entre los dos dominios, reservas va primero por tres razones:

1. **Es el problema real del usuario.** Un torneo se organiza dos veces al año; una cancha se
   reserva todas las semanas. Es también el dominio del que sale el modelo de ingresos (ver el
   Business Model Canvas del criterio 5).
2. **Contiene la integración entre microservicios.** `POST /reservations` es el único endpoint que
   cruza la frontera y llama a `users`. Cerrarlo temprano valida la decisión de
   [ADR-004](ADR-004-comunicacion-http-con-token-propagado.md) mientras todavía hay margen para
   cambiarla.
3. **Torneos depende de canchas, no al revés.** Un torneo puede tener sede; una cancha no necesita
   saber nada de torneos. La dependencia, aunque débil, apunta en esa dirección.

## Alternativas consideradas

| Alternativa | Por qué se descartó |
|---|---|
| **Los dos dominios en paralelo**, uno por persona | Era tentador: hay dos personas y dos dominios. Pero el reparto real del equipo es por **microservicio** (`users` / `matchpoint`), no por dominio de negocio, y los dos dominios viven en el mismo microservicio. Habría producido el escenario B. |
| **Torneos primero** | Es el dominio más vistoso —cuadro, avance de ganadores, campeón— y también el más complejo. Empezar por lo complejo cuando todavía no está probada la integración entre servicios es acumular riesgo, no reducirlo. |
| **Un corte vertical mínimo de cada dominio** y después profundizar | Habría dado dos demos superficiales sin ninguna regla de negocio interesante. La regla de solapamiento y el avance del cuadro son precisamente lo que hace que el sistema valga algo. |

## Consecuencias

**A favor**

- Desde la mitad del proyecto hubo **siempre** algo demostrable de punta a punta. Cualquier corte
  del plazo habría dejado un producto usable.
- La integración entre microservicios se validó con margen, no en la última semana.
- Al llegar a torneos, la infraestructura transversal —seguridad, logging, auditoría, manejo de
  errores, patrón de pruebas— ya estaba resuelta y solo hubo que aplicarla. Las cuatro historias
  de torneos avanzaron notablemente más rápido que las cuatro de reservas.

**En contra**

- **Serialización del trabajo dentro de `matchpoint`.** Durante las historias de reservas, el
  segundo integrante trabajó en `users` en lugar de en torneos. Se aceptó porque el paralelismo
  entre microservicios era suficiente para mantener a los dos ocupados.
- **Torneos quedó comprimido al final**, con menos margen ante imprevistos. Se compensó
  simplificando el dominio a conciencia (ver [ADR-006](ADR-006-cuadro-potencia-de-dos.md)).

## Puesta en práctica

Orden real del historial de git, verificable con `git log --oneline --reverse`:

```
f1cf571  feat(courts):       HU-05 HU-06 CRUD de canchas y motor de búsqueda de disponibilidad
bf84d97  feat(reservations):  HU-07 HU-08 reservas con validación de solapamiento y usuarios
f38da57  feat(tournaments):   HU-09 HU-10 creación de torneos e inscripción de equipos
63fbb3c  feat(matches):       HU-11 HU-12 llaves automáticas y lógica de avance por puntuación
```

Las cuatro historias de reservas están **antes** que las cuatro de torneos, sin entrelazado. La
colección de Postman conserva el mismo orden (`2. canchas`, `3. reservas`, `4. torneo completo`),
así que la demo recorre el proyecto en el orden en que se construyó.
