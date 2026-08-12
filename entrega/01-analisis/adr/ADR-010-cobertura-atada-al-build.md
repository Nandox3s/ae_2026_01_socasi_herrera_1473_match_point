# ADR-010 · Umbral de cobertura del 100 % atado al build, con exclusiones declaradas

- **Estado:** Aceptado
- **Fecha:** 2026-08-03
- **Decide:** Josué Herrera, Fernando Socasi
- **Requerimientos afectados:** RNF-08
- **Historia:** configuración y documentación

## Contexto

"Tenemos buena cobertura" es una afirmación que se degrada sola. Sin una condición que la haga
cumplir, la cobertura baja commit a commit y nadie se entera hasta que alguien mira el reporte.

El problema del 100 % es que empuja a escribir pruebas sin valor —pruebas de *getters*, de *data
classes*, del `main()`— solo para completar el número. Una prueba que existe únicamente para subir
un porcentaje es peor que no tenerla: consume tiempo de ejecución, hay que mantenerla y no detecta
nada.

Y hay un tercer problema: el número que reporta el IDE y el que reporta JaCoCo **no coinciden**, y
esa diferencia genera desconfianza si no se explica.

## Decisión

**Umbral del 100 % de líneas, atado al `check` del build, sobre un conjunto de clases
explícitamente delimitado.**

```kotlin
tasks.jacocoTestCoverageVerification {
    violationRules { rule { limit { counter = "LINE"; minimum = "1.00".toBigDecimal() } } }
}
tasks.check { dependsOn(tasks.jacocoTestCoverageVerification) }
```

Con exclusiones **declaradas en el propio `build.gradle.kts`**, no en un documento aparte:

```kotlin
val coverageExclusions = listOf(
    "com/pucetec/matchpoint/MatchpointApplication*",   // el main()
    "com/pucetec/matchpoint/config/**",                // configuración, sin lógica propia
    "com/pucetec/matchpoint/dto/**",                   // data classes de frontera
    "com/pucetec/matchpoint/entities/**",              // entidades sin comportamiento
    "com/pucetec/matchpoint/enums/**",
    "com/pucetec/matchpoint/audit/AuditLog*",
    "com/pucetec/matchpoint/clients/UserProfile*",
)
```

El criterio de exclusión es uno solo y se puede defender en una frase: **se excluye el código sin
lógica de decisión**. Una `data class` no tiene ramas; probarla es probar al compilador de Kotlin.
Todo lo que tiene un `if`, un `when`, una validación o una transformación está dentro y al 100 %.

Y se reportan **los dos números**, no el que conviene.

## Alternativas consideradas

| Alternativa | Por qué se descartó |
|---|---|
| **Umbral del 80 % sobre todo el código** | Es lo habitual y suena razonable, pero deja indefinido *qué* 20 % puede faltar. En la práctica el 20 % descubierto termina siendo la lógica difícil, que es exactamente la que más pruebas necesita. Un 100 % sobre un conjunto acotado es más honesto que un 80 % sobre todo. |
| **100 % sobre todo el código, sin exclusiones** | Obliga a probar `main()`, `data class` y constructores sintéticos. Pruebas sin valor que hay que mantener. |
| **Cobertura de ramas** en lugar de líneas | Es una métrica mejor. Se descartó porque Kotlin genera ramas sintéticas para los chequeos de nulabilidad que no corresponden a ningún camino real del programa, lo que produce un porcentaje engañoso y difícil de explicar. Las ramas reales quedan cubiertas de todos modos por la regla de "caso válido + caso inválido" de cada regla de negocio. |
| **Medir sin umbral**, solo reportar | Es lo mismo que no medir. Sin una condición que falle, el número solo baja. |

## Consecuencias

**A favor**

- **La cobertura no puede degradarse en silencio.** Un `git push` con código no probado hace fallar
  `./gradlew check`.
- **El criterio de exclusión vive en el código**, versionado y revisable: si alguien quiere excluir
  una clase más, aparece en el diff y hay que justificarlo.
- **Se reportan dos números y los dos son correctos**, lo que cierra la pregunta antes de que se
  formule:

  | Medición | `users` | `matchpoint` | Qué cuenta |
  |---|---:|---:|---|
  | JaCoCo (`./gradlew check`) | 100 % | 100 % | Solo el código propio, con las exclusiones |
  | Coverage del IDE | 99.3 % | 99.8 % | Todo, incluidas las clases excluidas |

  Lo único que el IDE marca sin cubrir es el `main()` y los constructores sintéticos de las
  entidades: exactamente las dos categorías excluidas. Los números concuerdan.

**En contra**

- **La cobertura no mide calidad.** Se puede llegar al 100 % con aserciones vacías. Se compensó con
  la regla de aserciones significativas de [`PRUEBAS-UNITARIAS.md`](../PRUEBAS-UNITARIAS.md#5-aserciones-significativas):
  se afirma sobre el valor, sobre el tipo exacto de la excepción y sobre el efecto colateral.
- **El umbral puede volverse un obstáculo** en un cambio urgente. Es el punto: obliga a escribir la
  prueba en el mismo commit, no "después".
- **Las exclusiones hay que revisarlas.** Si algún día una entidad gana comportamiento, debe salir
  de la lista. Es una deuda de mantenimiento asumida.

## Puesta en práctica

| Dónde | Qué |
|---|---|
| `matchpoint/build.gradle.kts` · `users/build.gradle.kts` | Plugin `jacoco`, exclusiones y `check` dependiendo de la verificación |
| `tasks.withType<Test> { finalizedBy(tasks.jacocoTestReport) }` | El reporte se genera solo al correr las pruebas |
| `build/reports/jacoco/test/html/index.html` | Reporte HTML por servicio (no se versiona: se regenera) |
| `docs/coverage/` | Capturas del coverage del IDE de los dos servicios |

Demostración de que la red funciona, no solo de que existe: comentar la validación de empates en
`TournamentService.registerScore` y ejecutar `./gradlew check`. Falla la prueba
`rejects a tie in a single elimination bracket`. Deshacer con `git checkout`.
