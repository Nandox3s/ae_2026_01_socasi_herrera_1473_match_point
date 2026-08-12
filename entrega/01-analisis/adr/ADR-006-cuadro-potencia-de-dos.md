# ADR-006 · Cupo potencia de dos y cuadro completo generado al arrancar

- **Estado:** Aceptado
- **Fecha:** 2026-08-03
- **Decide:** Josué Herrera
- **Requerimientos afectados:** RF-21, RF-26, RF-29
- **Historias:** HU-09, HU-11, HU-12

## Contexto

Un torneo de eliminación directa con un número de equipos que **no** es potencia de dos necesita
*byes*: equipos que pasan de ronda sin jugar. Con 6 equipos, dos pasan directo a semifinales. Eso
obliga a decidir, y a implementar, cuatro cosas más:

- qué equipos reciben el *bye* y con qué criterio (siembra, sorteo, orden de inscripción);
- cómo se representa en la base un partido que nunca se juega;
- cómo se calculan las estadísticas de un equipo que avanzó sin jugar;
- cómo se dibuja un cuadro asimétrico.

La segunda decisión, independiente de la anterior: **cuándo se crean los partidos**. Se puede
crear solo la primera ronda y generar la siguiente cuando se conozcan los ganadores, o crear
el cuadro entero desde el arranque con los slots futuros vacíos.

## Decisión

**Dos restricciones deliberadas del dominio:**

1. **`maxTeams` debe ser potencia de dos entre 2 y 32** (2, 4, 8, 16, 32). Cualquier otro valor se
   rechaza con `400` y un mensaje que enumera los valores válidos. **No hay *byes* en el sistema.**
2. **El cuadro completo se genera de una sola vez** al arrancar el torneo: la primera ronda con los
   emparejamientos reales en estado `READY`, y todas las rondas siguientes como slots vacíos en
   estado `PENDING`.

El avance se vuelve entonces una operación aritmética, no una creación condicional: el ganador del
partido en (`ronda`, `posición`) va al slot (`ronda+1`, `posición/2`), como local si la posición era
par y como visitante si era impar. Cuando el slot padre reúne a sus dos equipos, pasa a `READY`.

## Alternativas consideradas

| Alternativa | Por qué se descartó |
|---|---|
| **Admitir cualquier número de equipos con *byes*** | Multiplica la complejidad del cuadro y de las pruebas para resolver un caso que **la demo no necesita**: un torneo de 4 u 8 equipos demuestra exactamente lo mismo. Es complejidad que no compra nada. |
| **Completar con equipos fantasma** hasta la siguiente potencia de dos | Ensucia el modelo con filas que no son equipos reales y contamina las estadísticas y los listados públicos. |
| **Generar cada ronda al terminar la anterior** | Ahorra unas pocas filas y a cambio: el cuadro no es visible hasta que termina la ronda en curso, el avance del ganador pasa a ser "buscar o crear" en lugar de una actualización, y hay que sincronizar la creación cuando dos partidos de la misma ronda terminan a la vez. Más código y más casos de carrera para ahorrar espacio que no falta. |
| **Otros formatos** (liga, grupos + eliminación) | Fuera del alcance declarado. La eliminación directa se eligió porque es el formato real de los torneos amateur de fin de semana, que es el usuario objetivo. |

## Consecuencias

**A favor**

- **El cuadro es visible desde el minuto uno.** `GET /tournaments/{id}` justo después del `start`
  ya devuelve todas las rondas con sus nombres (*Quarterfinals*, *Semifinals*, *Final*). Eso es lo
  que un usuario espera ver, y es una demo mucho mejor.
- **El avance es determinista y trivial de probar.** `posición/2` no tiene casos límite. Las 46
  pruebas de `TournamentServiceTest` cubren el avance completo de un cuadro de 4 y de 8 equipos sin
  ninguna rama especial.
- **El número de rondas se calcula con una operación de bits:**
  `Integer.numberOfTrailingZeros(maxTeams)`, correcta por construcción porque el valor ya se validó
  como potencia de dos.
- **`UNIQUE (tournament_id, round_number, position_in_round)`** garantiza en el motor que no haya
  dos partidos en el mismo slot.

**En contra**

- **Un torneo real de 6 equipos no se puede modelar.** Es la limitación funcional más visible del
  sistema y hay que decirla antes de que la pregunten. La ruta de evolución está clara: admitir
  cualquier `maxTeams`, calcular la potencia de dos superior y sembrar los *byes* como partidos
  pre-resueltos en la primera ronda — se implementa **sin tocar** la lógica de avance, que es
  justamente la ventaja de haber generado el cuadro completo.
- **Se crean filas de partidos que quizá nunca se jueguen** si el torneo se abandona a medias.
  Para 32 equipos son 31 filas: irrelevante.
- **El emparejamiento inicial es por orden de inscripción**, no por siembra según ranking. Para un
  torneo amateur es aceptable y además es transparente: el primero que se inscribe sabe contra
  quién juega.

## Puesta en práctica

| Dónde | Qué |
|---|---|
| `TournamentService.isPowerOfTwo` | `value > 0 && (value and (value - 1)) == 0` |
| `TournamentService.createTournament` | Valida el rango 2–32 y lanza `InvalidTournamentException` → `400` |
| `TournamentService.startTournament` | Exige el cupo exacto y genera **todas** las rondas |
| `TournamentService.advanceWinner` | `parentRound = round + 1`, `parentPosition = position / 2`, `goesHome = position % 2 == 0` |
| `TournamentService.totalRounds` | `Integer.numberOfTrailingZeros(maxTeams)` |
| `TournamentService.roundName` | Traduce la distancia a la final en nombre legible |
| `TournamentServiceTest` | 46 pruebas, incluido el recorrido completo de un cuadro hasta el campeón |

Verificación: `POST /matchpoint/tournaments` con `"maxTeams": 6` responde `400` con el mensaje
*"maxTeams must be a power of two between 2 and 32 (2, 4, 8, 16, 32); received 6"*. El mensaje de
error **enseña la regla**, no solo la aplica.
