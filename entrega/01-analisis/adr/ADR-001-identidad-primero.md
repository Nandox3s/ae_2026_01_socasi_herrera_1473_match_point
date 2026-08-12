# ADR-001 · Construir la identidad antes que cualquier funcionalidad de negocio

- **Estado:** Aceptado
- **Fecha:** 2026-08-01
- **Decide:** Josué Herrera, Fernando Socasi
- **Requerimientos afectados:** RF-08, RF-09, RF-10 · RNF-01
- **Historia:** HU-04

## Contexto

El backlog tenía doce historias. Once producen algo visible —canchas, reservas, torneos— y una,
HU-04, no produce nada que el usuario vea: solo valida tokens.

La tentación obvia era dejarla para el final y empezar por lo demostrable. Tres hechos lo
desaconsejaban:

1. **Toda historia posterior depende de ella.** `courts.manager_user`, `reservations.owner_user`
   y `teams.registered_by_user` no son campos que envíe el cliente: son el claim `username` del
   token. Sin identidad, esas columnas no tienen de dónde salir y los DTO se diseñan mal —con un
   campo de propietario que después habría que quitar.
2. **Es el mayor riesgo técnico del proyecto.** Es la única pieza que depende de un servicio
   externo (AWS Cognito), de un flujo de autenticación con *client secret* y `SECRET_HASH`, y de
   una validación de firma contra un JWKS remoto. Si algo iba a salir mal, iba a salir mal ahí.
3. **Condiciona el diseño de las pruebas.** Cómo se fabrica un JWT de prueba, cómo se simula un
   rol, cómo se verifica un `403`: si eso se resuelve al final, hay que reescribir las pruebas de
   las once historias anteriores.

Un riesgo alto descubierto en la última semana no se mitiga; se sufre.

## Decisión

**HU-04 va primera.** No se escribe ninguna línea de lógica de negocio hasta que un token real de
Cognito atraviese el sistema, se valide contra el JWKS y produzca `ROLE_MANAGER` o `ROLE_PLAYER`.

El criterio de terminado de HU-04 es una demostración concreta, no una casilla marcada:
`GET /matchpoint/me` con un token real devuelve `username`, `sub` y `groups`; sin token devuelve
`401`; con token manipulado devuelve `401`.

## Alternativas consideradas

| Alternativa | Por qué se descartó |
|---|---|
| **Dejar la seguridad para el final** y desarrollar con endpoints abiertos | Habría obligado a reescribir todos los DTO de request (quitando el campo de propietario), todos los *services* (que reciben el `username` como parámetro) y las 214 pruebas. El costo de reescritura supera con creces lo que se gana empezando antes. |
| **Autenticación provisional propia** (usuario en memoria o JWT firmado localmente) para reemplazarla al final por Cognito | Es exactamente el riesgo que se quería evitar, aplazado y disfrazado. Además la rúbrica pide integración real con un proveedor de identidad; el reemplazo final habría sido la tarea más grande de la última semana. |
| **Desarrollar en paralelo**: uno la seguridad, otro el negocio | Sí se hizo parcialmente —el reparto por microservicio lo permite— pero la seguridad se terminó **antes** de que el negocio la necesitara, no al mismo tiempo. El paralelismo fue de personas, no de dependencias. |

## Consecuencias

**A favor**

- El riesgo mayor del proyecto quedó cerrado en la primera semana, con margen para reaccionar.
- Los DTO de request quedaron limpios desde el diseño: **no tienen campo de propietario**, así que
  la suplantación por body es estructuralmente imposible (ver [ADR-007](ADR-007-autorizacion-en-dos-capas.md)).
- Cada historia posterior se probó desde el primer día con roles reales, no con endpoints abiertos
  que "después se protegen".

**En contra**

- La primera semana no produjo nada visible. Para un observador externo, el proyecto parecía no
  avanzar. Fue un costo real y asumido a conciencia.
- Obligó a resolver de entrada la configuración de Cognito (User Pool, grupos, *client secret*,
  flujo `USER_AUTH`), que es trabajo de infraestructura, no de programación.

## Puesta en práctica

| Dónde | Qué |
|---|---|
| `matchpoint/src/main/kotlin/.../config/SecurityConfig.kt` | Cadena de filtros; cada endpoint declara el rol que admite |
| `users/src/main/kotlin/.../config/SecurityConfig.kt` | Misma estructura en el otro servicio |
| `.../config/CognitoGroupsConverter.kt` | `cognito:groups` → `ROLE_*`, idéntico en ambos |
| `.../logging/LoggingAuthenticationEntryPoint.kt` | Registra el `401` de token manipulado o expirado |
| Commit `92e4e56` | `feat(security): HU-04 …` — **el primer commit de funcionalidad del proyecto**, inmediatamente después del `initial import` |
| Commit `351e254` | El mismo trabajo, re-aplicado tras la reestructuración del repositorio (ver nota abajo) |

Verificación:

```bash
git log --oneline --reverse | head -3
```

```
67af96f chore(repo): initial import
92e4e56 feat(security): HU-04 implementar validación de JWT y JWKS con AWS Cognito
5c73958 feat(users): HU-01 crear endpoints para registro automático y visualización de perfil
```

**El commit de seguridad es el primer commit de funcionalidad del repositorio**, antes que cualquier
commit de negocio. El orden del historial **es** la evidencia de la priorización.

> **Nota sobre HU-01 a HU-04 duplicadas.** El commit `d32f9bb` (`chore(repo): mover contenido de
> match a la raíz del repositorio`) reorganizó el árbol del proyecto, y las cuatro historias del
> microservicio `users` se re-aplicaron sobre la estructura nueva (`351e254`, `9284301`, `8488316`,
> `c614302`). Por eso aparecen dos veces en el historial. **No es trabajo repetido: es el mismo
> trabajo reubicado**, y está explicado en la sección 4 de [`GITFLOW.md`](../GITFLOW.md).
