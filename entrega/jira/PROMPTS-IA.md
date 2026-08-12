# MatchPoint · Prompts para crear el backlog con la IA de Jira

Catorce prompts listos para pegar en **Atlassian Intelligence / Rovo** (el botón *IA* del proyecto, o el chat de Rovo). Se generan desde [`generar-backlog.py`](generar-backlog.py), igual que [`BACKLOG.md`](BACKLOG.md), [`SPRINTS.md`](SPRINTS.md) y [`jira-import.csv`](jira-import.csv): dicen exactamente lo mismo.

---

## Antes de empezar: qué esperar

Conviene saberlo antes de invertir una hora en esto:

| | Importar el CSV | Crear con la IA |
|---|---|---|
| Tiempo | ~10 min | ~45–60 min |
| Fiabilidad de resumen y descripción | Total | Alta |
| Story points, sprint y estado | Se importan tal cual | **La IA suele ignorarlos o inventarlos** |
| Jerarquía épica → historia | Exacta | Suele funcionar, hay que revisarla |
| Subtareas | Exactas | Poco fiables |
| Fechas de sprint | No las lleva ninguno de los dos | — |

**Recomendación honesta: importa el CSV** ([`COMO-IMPORTAR.md`](COMO-IMPORTAR.md)) y usa estos prompts solo si prefieres el flujo conversacional o si el importador no está disponible en tu plan de Jira. Si usas la IA, **revisa después** puntos, sprint y estado: son los campos que peor maneja.

> Los modelos de Atlassian cambian seguido y no todos los planes tienen las mismas capacidades. Si un prompt crea menos elementos de los pedidos, córtalo por la mitad y mándalo en dos tandas; es el fallo más frecuente.

---

## Prompt 0 · Preparar el proyecto

Pégalo primero. Configura el terreno antes de crear nada.

```text
Voy a cargar el backlog de un proyecto llamado MatchPoint (backend de reserva de canchas de básquet y torneos de eliminación directa). Antes de crear elementos de trabajo, prepara el proyecto:

1. Crea estos componentes: docs, infra, matchpoint, movil, producto, transversal, users.
2. Crea estos sprints con sus fechas y objetivos:
   - Sprint 1 | 2026-07-07 a 2026-07-13 | estado: Cerrado | objetivo: Fundación: identidad antes que negocio
   - Sprint 2 | 2026-07-14 a 2026-07-20 | estado: Cerrado | objetivo: Perfiles completos y catálogo de canchas
   - Sprint 3 | 2026-07-21 a 2026-07-27 | estado: Cerrado | objetivo: Motor de reservas de punta a punta
   - Sprint 4 | 2026-07-28 a 2026-08-03 | estado: Cerrado | objetivo: Torneos y cuadro de eliminación directa
   - Sprint 5 | 2026-08-04 a 2026-08-10 | estado: Cerrado | objetivo: Observabilidad, calidad e infraestructura
   - Sprint 6 | 2026-08-11 a 2026-08-17 | estado: En curso | objetivo: Documentación de la entrega e higiene del repositorio
   - Sprint 7 | 2026-08-18 a 2026-08-24 | estado: Planificado | objetivo: App móvil Android
3. Deja los sprints 1 a 5 marcados como completados, el Sprint 6 como sprint activo y el Sprint 7 sin iniciar.

Reglas que debes seguir en todos mis mensajes siguientes:
- Usa EXACTAMENTE los valores que te doy: resumen, descripción, tipo, story points, prioridad, estado, sprint, componente, etiquetas y responsable.
- No inventes elementos de trabajo, ni criterios de aceptación, ni estimaciones que yo no te haya dado.
- No resumas ni acortes las descripciones: cópialas completas.
- Si un campo no existe en el proyecto, créalo o avísame; no lo descartes en silencio.
- Al terminar cada mensaje, dime cuántos elementos creaste y con qué claves.

Confirma que está listo y espera mi siguiente mensaje.
```

---

## Prompt 1 · Identidad y perfiles de usuario

Crea 1 épica y 4 elementos hijos (15 story points).

```text
Crea una épica y sus elementos hijos en el proyecto MatchPoint. Respeta los valores exactos y no inventes nada.

ÉPICA
Resumen: Identidad y perfiles de usuario
Tipo: Epic | Prioridad: Highest | Estado: Done | Componente: users | Sprint: Sprint 1
Etiquetas: users, backend
Descripción:
Microservicio users: registro, consulta, actualización y administración de perfiles. El perfil se ancla al claim sub del JWT de Cognito; no almacena contraseñas ni roles.

DENTRO DE ESA ÉPICA, CREA ESTOS 4 ELEMENTOS:

1. HU-01 Registro automático de perfil de usuario
   Tipo: Story | Story points: 5 | Prioridad: Highest | Estado: Done | Sprint: Sprint 1 | Componente: users | Responsable: Fernando Socasi
   Etiquetas: users, RF-01, RF-02, HU-01
   Descripción:
   Como usuario autenticado quiero registrar mi perfil para poder operar en la plataforma, sin escribir mi identificador porque el sistema lo toma del token.

   Criterios de aceptación:
   - POST /users/me crea el perfil tomando cognito_id y username de los claims del JWT, no del body
   - GET /users/me devuelve el perfil asociado al sub del token
   - Un mismo usuario de Cognito no puede tener dos perfiles: responde 409
   - Nombre en blanco responde 400
   - Sin token responde 401

   Evidencia: UserController.createMyProfile · UserService.createUser · UserControllerTest

   Rúbrica: 1.1 · RF-01 RF-02

2. HU-02 Actualización de datos de contacto
   Tipo: Story | Story points: 3 | Prioridad: Medium | Estado: Done | Sprint: Sprint 1 | Componente: users | Responsable: Fernando Socasi
   Etiquetas: users, RF-03, HU-02
   Descripción:
   Como jugador quiero actualizar mi nombre, correo y teléfono para mantener mis datos al día.

   Criterios de aceptación:
   - PUT /users/me actualiza únicamente el perfil propio, resuelto por el sub del token
   - El nombre no puede quedar en blanco: 400
   - Si el usuario nunca se registró: 404
   - El cambio queda en audit_log con valores anteriores y nuevos enmascarados
   - No existe endpoint para editar el perfil de otro usuario

   Evidencia: UserService.updateUser · BlankNameException

   Rúbrica: 1.1 · RF-03

3. HU-03 Administración de perfiles exclusiva de MANAGER
   Tipo: Story | Story points: 5 | Prioridad: Medium | Estado: Done | Sprint: Sprint 2 | Componente: users | Responsable: Fernando Socasi
   Etiquetas: users, RF-04, RF-05, RF-06, HU-03
   Descripción:
   Como MANAGER quiero listar, consultar y dar de baja perfiles para administrar la plataforma.

   Criterios de aceptación:
   - GET /users devuelve todos los perfiles solo para MANAGER; un PLAYER recibe 403
   - GET /users/{id} devuelve un perfil concreto solo para MANAGER
   - DELETE /users/{id} elimina el perfil y responde 204
   - La baja deja registro DELETE en audit_log con los valores previos
   - audit_log no tiene FK hacia users: el rastro sobrevive al borrado

   Evidencia: SecurityConfig hasRole(MANAGER) · UserService.deleteUser

   Rúbrica: 1.1 · RF-04 RF-05 RF-06

4. Resolución de perfil por identificador de Cognito
   Tipo: Story | Story points: 2 | Prioridad: Medium | Estado: Done | Sprint: Sprint 2 | Componente: users | Responsable: Fernando Socasi
   Etiquetas: users, RF-07, integración
   Descripción:
   Como microservicio matchpoint necesito resolver un perfil a partir del cognitoId para validar que el jugador está registrado antes de crear una reserva.

   Criterios de aceptación:
   - GET /users/cognito/{cognitoId} disponible para cualquier usuario autenticado
   - Devuelve 404 si no existe perfil para ese identificador
   - Es el endpoint que consume UsersClient desde matchpoint

   Evidencia: UserController · UsersClient

   Rúbrica: 1.1 · RF-07

Al terminar, confírmame que creaste 1 épica y 4 elementos hijos, con sus claves.
```

---

## Prompt 2 · Seguridad: autenticación y autorización

Crea 1 épica y 3 elementos hijos (16 story points).

```text
Crea una épica y sus elementos hijos en el proyecto MatchPoint. Respeta los valores exactos y no inventes nada.

ÉPICA
Resumen: Seguridad: autenticación y autorización
Tipo: Epic | Prioridad: Highest | Estado: Done | Componente: transversal | Sprint: Sprint 1
Etiquetas: security, backend
Descripción:
Identidad delegada a AWS Cognito. Cada microservicio es un OAuth2 Resource Server que valida firma, emisor y expiración contra el JWKS. Autorización en dos capas: rol y propiedad del recurso.

DENTRO DE ESA ÉPICA, CREA ESTOS 3 ELEMENTOS:

1. HU-04 Validación de JWT y JWKS con AWS Cognito
   Tipo: Story | Story points: 8 | Prioridad: Highest | Estado: Done | Sprint: Sprint 1 | Componente: transversal | Responsable: Fernando Socasi
   Etiquetas: security, cognito, RF-08, HU-04, ADR-001
   Descripción:
   Como sistema necesito validar cada token contra el User Pool para no confiar en nadie sin identidad verificada. PRIMERA historia del proyecto: ninguna otra se puede probar sin ella (ADR-001).

   Criterios de aceptación:
   - Cada microservicio configura issuer-uri y descarga el JWKS del User Pool
   - Se valida firma, emisor y expiración de cada token
   - Token ausente, expirado o manipulado responde 401
   - No existe JWT propio, ni sesión, ni Basic Auth, ni usuarios en memoria
   - Los dos servicios arman el issuer con las mismas variables: un token que uno acepta el otro también
   - GET /matchpoint/me con token real devuelve username, sub y groups

   Evidencia: SecurityConfig (ambos servicios) · LoggingAuthenticationEntryPoint · commit 92e4e56

   Rúbrica: 3.6 · RF-08 · ADR-001 ADR-002

2. Mapeo de cognito:groups a roles de Spring Security
   Tipo: Story | Story points: 3 | Prioridad: Highest | Estado: Done | Sprint: Sprint 1 | Componente: transversal | Responsable: Fernando Socasi
   Etiquetas: security, cognito, RF-09
   Descripción:
   Como sistema necesito traducir los grupos de Cognito a autoridades para restringir endpoints por rol.

   Criterios de aceptación:
   - El claim cognito:groups se traduce a ROLE_MANAGER / ROLE_PLAYER
   - El conversor es idéntico en los dos microservicios
   - Un rol equivocado en un endpoint protegido responde 403 con event=authz.denied
   - Si el claim no existe, el usuario queda autenticado sin autoridades

   Evidencia: CognitoGroupsConverter · CognitoGroupsConverterTest

   Rúbrica: 3.6 · RF-09

3. Autorización por propiedad del recurso
   Tipo: Story | Story points: 5 | Prioridad: Highest | Estado: Done | Sprint: Sprint 2 | Componente: transversal | Responsable: Josué Herrera
   Etiquetas: security, RF-10, ADR-007
   Descripción:
   Como dueño de un recurso quiero que nadie más pueda modificarlo, aunque tenga mi mismo rol. Segunda capa de 403, decidida en el service (ADR-007).

   Criterios de aceptación:
   - findCourtOwnedBy, findReservationOwnedBy y findTournamentOwnedBy comparan contra el username del token
   - Un MANAGER no puede editar la cancha de otro MANAGER: 403
   - Un PLAYER no puede cancelar la reserva de otro: 403
   - Los DTO de request NO tienen campo de propietario: la suplantación por body es imposible
   - El log distingue event=ownership.denied de event=authz.denied

   Evidencia: CourtService · ReservationService · TournamentService · GlobalExceptionHandler

   Rúbrica: 3.6 · RF-10 · ADR-007

Al terminar, confírmame que creaste 1 épica y 3 elementos hijos, con sus claves.
```

---

## Prompt 3 · Gestión de canchas

Crea 1 épica y 3 elementos hijos (16 story points).

```text
Crea una épica y sus elementos hijos en el proyecto MatchPoint. Respeta los valores exactos y no inventes nada.

ÉPICA
Resumen: Gestión de canchas
Tipo: Epic | Prioridad: High | Estado: Done | Componente: matchpoint | Sprint: Sprint 2
Etiquetas: matchpoint, backend
Descripción:
Publicación, catálogo público y motor de búsqueda de disponibilidad por franja horaria. El catálogo es público sin token: es el escaparate del producto.

DENTRO DE ESA ÉPICA, CREA ESTOS 3 ELEMENTOS:

1. HU-05 Publicación y actualización de canchas
   Tipo: Story | Story points: 5 | Prioridad: High | Estado: Done | Sprint: Sprint 2 | Componente: matchpoint | Responsable: Josué Herrera
   Etiquetas: matchpoint, canchas, RF-11, RF-15, HU-05
   Descripción:
   Como MANAGER quiero publicar mis canchas y ajustar precio y disponibilidad para que los jugadores puedan encontrarlas y reservarlas.

   Criterios de aceptación:
   - POST /matchpoint/courts crea la cancha; el managerUser sale del claim del token
   - Nombre, sector y tipo de piso son obligatorios: 400 si faltan
   - El precio por hora debe ser mayor que 0: 400
   - PATCH /matchpoint/courts/{id} actualiza precio y estado activo, solo del dueño
   - Cancha ajena: 403 · Cancha inexistente: 404 · Un PLAYER: 403
   - Toda mutación deja fila en audit_log

   Evidencia: CourtService.createCourt · CourtService.updateCourt · CourtServiceTest

   Rúbrica: 3.3 · RF-11 RF-15

2. HU-06 Motor de búsqueda de disponibilidad
   Tipo: Story | Story points: 8 | Prioridad: Highest | Estado: Done | Sprint: Sprint 2 | Componente: matchpoint | Responsable: Josué Herrera
   Etiquetas: matchpoint, canchas, RF-14, HU-06
   Descripción:
   Como jugador quiero ver qué canchas están libres en una franja horaria concreta para no tener que llamar a cinco números.

   Criterios de aceptación:
   - GET /matchpoint/courts/available acepta sector, sport, startsAt y durationMinutes
   - Devuelve SOLO canchas activas
   - Descarta canchas con reserva CONFIRMED solapada: startsAt < reserva.endsAt AND reserva.startsAt < endsAt
   - Sin franja horaria devuelve todas las activas que cumplen los filtros
   - Endpoint público, sin token
   - Lista vacía responde 200: no es un error

   Evidencia: CourtService.availableCourts · CourtService.isFree · 19 pruebas en CourtServiceTest

   Rúbrica: 3.3 · RF-14

3. Catálogo público de canchas con filtros
   Tipo: Story | Story points: 3 | Prioridad: Medium | Estado: Done | Sprint: Sprint 2 | Componente: matchpoint | Responsable: Josué Herrera
   Etiquetas: matchpoint, canchas, RF-12, RF-13
   Descripción:
   Como visitante quiero ver el catálogo sin crear cuenta, porque pedir registro antes de mostrar valor es la mayor fricción de un marketplace.

   Criterios de aceptación:
   - GET /matchpoint/courts es público y admite filtros por sector y deporte
   - GET /matchpoint/courts/{id} es público; 404 si no existe
   - No consulta Cognito ni al microservicio users: resuelve contra la base local

   Evidencia: SecurityConfig permitAll · CourtService.listCourts

   Rúbrica: 3.3 · RF-12 RF-13

Al terminar, confírmame que creaste 1 épica y 3 elementos hijos, con sus claves.
```

---

## Prompt 4 · Motor de reservas

Crea 1 épica y 4 elementos hijos (20 story points).

```text
Crea una épica y sus elementos hijos en el proyecto MatchPoint. Respeta los valores exactos y no inventes nada.

ÉPICA
Resumen: Motor de reservas
Tipo: Epic | Prioridad: Highest | Estado: Done | Componente: matchpoint | Sprint: Sprint 3
Etiquetas: matchpoint, backend
Descripción:
Creación de reservas con validación de solapamiento y de perfil remoto, listado y cancelación. Es el único flujo que cruza la frontera entre los dos microservicios.

DENTRO DE ESA ÉPICA, CREA ESTOS 2 ELEMENTOS:

1. HU-07 Creación de reserva con validación de solapamiento
   Tipo: Story | Story points: 8 | Prioridad: Highest | Estado: Done | Sprint: Sprint 3 | Componente: matchpoint | Responsable: Josué Herrera
   Etiquetas: matchpoint, reservas, RF-17, HU-07
   Descripción:
   Como jugador quiero reservar una cancha en una franja concreta y que el sistema impida que dos personas reserven la misma hora. Es la regla que le vende el producto al dueño.

   Criterios de aceptación:
   - POST /matchpoint/reservations crea la reserva con ownerUser del token
   - La cancha debe existir (404) y estar activa (409)
   - durationMinutes debe ser mayor que 0 (400)
   - No puede existir ninguna reserva CONFIRMED solapada en esa cancha (409)
   - El solapamiento se evalúa con inicioA < finB AND inicioB < finA, cubriendo contención parcial
   - Un MANAGER recibe 403

   Evidencia: ReservationService.createReservation · ReservationServiceTest (12 pruebas)

   Rúbrica: 3.3 · RF-16 RF-17 · RN-01 RN-02

   Subtarea 1.1 de «HU-07 Creación de reserva con validación de solapamiento»:
   1.1. Validar perfil llamando al microservicio users
      Tipo: Sub-task | Story points: 5 | Prioridad: Highest | Estado: Done | Sprint: Sprint 3 | Componente: matchpoint | Responsable: Josué Herrera
      Etiquetas: integración, RF-16, ADR-004
      Descripción:
      Antes de guardar la reserva, matchpoint pide el perfil del jugador a users propagando el token del usuario.

      Criterios de aceptación:
      - GET /users/me con la cabecera Authorization original reenviada
      - Timeout explícito de 3 segundos
      - Si el jugador no tiene perfil: 409
      - Si users no responde: 503, y matchpoint se recupera solo
      - El nombre devuelto se copia en reservations.owner_name
      - NUNCA se fabrica un token de servicio

      Evidencia: UsersClient · HttpClientConfig · UsersClientTest (7 pruebas)

      Rúbrica: 3.3 · RF-16 · ADR-004

   Subtarea 1.2 de «HU-07 Creación de reserva con validación de solapamiento»:
   1.2. Sembrar datos que disparan cada validación de reserva
      Tipo: Sub-task | Story points: 2 | Prioridad: Medium | Estado: Done | Sprint: Sprint 3 | Componente: matchpoint | Responsable: Josué Herrera
      Etiquetas: datos, demo
      Descripción:
      data.sql debe incluir los casos que permiten demostrar cada regla sin preparar nada.

      Criterios de aceptación:
      - Cancha 3 inactiva: POST /reservations responde 409
      - Reserva 1 ocupando una franja: misma franja responde 409
      - Reserva 4 de player_luis: DELETE responde 403 por propiedad
      - Los INSERT usan ON CONFLICT DO NOTHING: reiniciar no duplica

2. HU-08 Consulta y cancelación de reservas propias
   Tipo: Story | Story points: 5 | Prioridad: High | Estado: Done | Sprint: Sprint 3 | Componente: matchpoint | Responsable: Josué Herrera
   Etiquetas: matchpoint, reservas, RF-20, HU-08, ADR-008
   Descripción:
   Como jugador quiero ver y cancelar mis reservas, y que la franja cancelada vuelva a estar disponible.

   Criterios de aceptación:
   - GET /matchpoint/reservations/me devuelve solo las propias, ordenadas por fecha descendente
   - GET /matchpoint/reservations/{id} responde 403 si la reserva es de otro jugador
   - DELETE /matchpoint/reservations/{id} marca CANCELLED: borrado lógico, la fila se conserva
   - La franja cancelada vuelve a estar libre: la disponibilidad solo mira reservas CONFIRMED
   - El cambio queda en audit_log con el estado anterior

   Evidencia: ReservationService.listMine · ReservationService.cancel

   Rúbrica: 3.3 · RF-18 RF-19 RF-20 · ADR-008

Al terminar, confírmame que creaste 1 épica y 4 elementos hijos, con sus claves.
```

---

## Prompt 5 · Torneos e inscripción de equipos

Crea 1 épica y 3 elementos hijos (13 story points).

```text
Crea una épica y sus elementos hijos en el proyecto MatchPoint. Respeta los valores exactos y no inventes nada.

ÉPICA
Resumen: Torneos e inscripción de equipos
Tipo: Epic | Prioridad: High | Estado: Done | Componente: matchpoint | Sprint: Sprint 4
Etiquetas: matchpoint, backend
Descripción:
Creación de torneos de eliminación directa, inscripción y retiro de equipos, y consulta pública del progreso.

DENTRO DE ESA ÉPICA, CREA ESTOS 3 ELEMENTOS:

1. HU-09 Creación de torneos
   Tipo: Story | Story points: 5 | Prioridad: High | Estado: Done | Sprint: Sprint 4 | Componente: matchpoint | Responsable: Josué Herrera
   Etiquetas: matchpoint, torneos, RF-21, HU-09, ADR-006
   Descripción:
   Como MANAGER quiero crear un torneo de eliminación directa con su cupo y su sede opcional.

   Criterios de aceptación:
   - POST /matchpoint/tournaments crea el torneo en estado REGISTRATION
   - maxTeams debe ser potencia de dos entre 2 y 32: cualquier otro valor responde 400
   - El mensaje de error enumera los valores válidos (2, 4, 8, 16, 32)
   - Si se indica sede, la cancha debe existir (404) y ser del propio MANAGER (403)
   - Un PLAYER recibe 403

   Evidencia: TournamentService.createTournament · isPowerOfTwo · resolveCourt

   Rúbrica: 3.3 · RF-21 · ADR-006

2. HU-10 Inscripción y retiro de equipos
   Tipo: Story | Story points: 5 | Prioridad: High | Estado: Done | Sprint: Sprint 4 | Componente: matchpoint | Responsable: Josué Herrera
   Etiquetas: matchpoint, torneos, RF-23, RF-24, HU-10
   Descripción:
   Como jugador quiero inscribir mi equipo en un torneo abierto y poder retirarlo antes de que arranque.

   Criterios de aceptación:
   - POST /tournaments/{id}/teams inscribe con registeredByUser del token
   - Torneo que ya arrancó o terminó: 409
   - Cupo lleno: 409
   - Nombre de equipo repetido dentro del torneo: 409
   - Datos de contacto en blanco: 400
   - DELETE .../teams/{teamId} solo mientras el torneo siga en REGISTRATION (409 si no)
   - Solo puede retirarlo quien lo inscribió: 403
   - El correo se enmascara en el log y en audit_log

   Evidencia: TournamentService.registerTeam · TournamentService.withdrawTeam

   Rúbrica: 3.3 · RF-23 RF-24

3. Consulta pública de torneos y del cuadro
   Tipo: Story | Story points: 3 | Prioridad: Medium | Estado: Done | Sprint: Sprint 4 | Componente: matchpoint | Responsable: Josué Herrera
   Etiquetas: matchpoint, torneos, RF-22, RF-25
   Descripción:
   Como visitante quiero ver el cuadro de un torneo sin cuenta, para poder compartirlo por WhatsApp.

   Criterios de aceptación:
   - GET /matchpoint/tournaments lista torneos con su conteo de equipos inscritos
   - GET /matchpoint/tournaments/{id} devuelve estado, equipos, rondas y campeón
   - Las rondas llevan nombre legible: Final, Semifinals, Quarterfinals, Round of 16, Round of 32
   - Torneo en REGISTRATION devuelve lista de rondas vacía, no error
   - Endpoints públicos, sin token

   Evidencia: TournamentService.getProgress · buildRounds · roundName

   Rúbrica: 3.3 · RF-22 RF-25

Al terminar, confírmame que creaste 1 épica y 3 elementos hijos, con sus claves.
```

---

## Prompt 6 · Cuadro de partidos y avance automático

Crea 1 épica y 4 elementos hijos (21 story points).

```text
Crea una épica y sus elementos hijos en el proyecto MatchPoint. Respeta los valores exactos y no inventes nada.

ÉPICA
Resumen: Cuadro de partidos y avance automático
Tipo: Epic | Prioridad: High | Estado: Done | Componente: matchpoint | Sprint: Sprint 4
Etiquetas: matchpoint, backend
Descripción:
Generación del cuadro completo al arrancar, programación de partidos, registro de marcador y avance automático del ganador hasta el campeón.

DENTRO DE ESA ÉPICA, CREA ESTOS 3 ELEMENTOS:

1. HU-11 Generación automática del cuadro
   Tipo: Story | Story points: 8 | Prioridad: High | Estado: Done | Sprint: Sprint 4 | Componente: matchpoint | Responsable: Josué Herrera
   Etiquetas: matchpoint, partidos, RF-26, HU-11, ADR-006
   Descripción:
   Como MANAGER quiero arrancar el torneo y que el sistema arme todas las llaves, para no dibujar el cuadro a mano.

   Criterios de aceptación:
   - POST /tournaments/{id}/start exige exactamente maxTeams equipos inscritos: 409 si faltan o sobran
   - El torneo debe estar en REGISTRATION: 409 si ya arrancó
   - Solo el MANAGER dueño del torneo: 403
   - La primera ronda se arma por orden de inscripción, en estado READY
   - Las rondas siguientes se crean como slots vacíos en estado PENDING
   - El torneo pasa a IN_PROGRESS y devuelve el cuadro completo

   Evidencia: TournamentService.startTournament · TournamentServiceTest (46 pruebas)

   Rúbrica: 3.3 · RF-26 · ADR-006

   Subtarea 1.1 de «HU-11 Generación automática del cuadro»:
   1.1. Calcular número de rondas y nombres legibles
      Tipo: Sub-task | Story points: 2 | Prioridad: Medium | Estado: Done | Sprint: Sprint 4 | Componente: matchpoint | Responsable: Josué Herrera
      Etiquetas: matchpoint, partidos
      Descripción:
      El total de rondas se deriva del cupo, que ya se validó como potencia de dos.

      Criterios de aceptación:
      - totalRounds = Integer.numberOfTrailingZeros(maxTeams)
      - roundName traduce la distancia a la final en nombre legible
      - Cubierto por pruebas para cuadros de 2, 4, 8, 16 y 32 equipos

2. HU-12 Registro de marcador y avance del ganador
   Tipo: Story | Story points: 8 | Prioridad: Highest | Estado: Done | Sprint: Sprint 4 | Componente: matchpoint | Responsable: Josué Herrera
   Etiquetas: matchpoint, partidos, RF-28, RF-29, HU-12
   Descripción:
   Como MANAGER quiero registrar el marcador y que el ganador avance solo a la siguiente ronda.

   Criterios de aceptación:
   - PATCH /matchpoint/matches/{id}/score registra el marcador
   - Partido PENDING sin los dos equipos: 409
   - Partido ya PLAYED: 409
   - Marcadores negativos: 400
   - Empate: 400, porque en eliminación directa siempre hay ganador
   - Se actualizan estadísticas de ambos equipos: jugados, ganados, perdidos, puntos a favor y en contra
   - El perdedor queda eliminated
   - El ganador se mueve al slot (ronda+1, posición/2): local si la posición era par, visitante si impar
   - El slot padre pasa a READY cuando reúne a sus dos equipos
   - Si era la última ronda, el torneo pasa a FINISHED con championTeam

   Evidencia: TournamentService.registerScore · advanceWinner · applyStats

   Rúbrica: 3.3 · RF-28 RF-29 RF-30 · RN-08 RN-09 RN-10

3. Programación de fecha y hora de partidos
   Tipo: Story | Story points: 3 | Prioridad: Medium | Estado: Done | Sprint: Sprint 4 | Componente: matchpoint | Responsable: Josué Herrera
   Etiquetas: matchpoint, partidos, RF-27
   Descripción:
   Como MANAGER quiero fijar cuándo se juega cada partido de mi torneo.

   Criterios de aceptación:
   - PATCH /matchpoint/matches/{id}/schedule fija scheduledAt
   - Solo el MANAGER dueño del torneo: 403
   - Un partido ya jugado no se reprograma: 409

   Evidencia: TournamentService.scheduleMatch

   Rúbrica: 3.3 · RF-27

Al terminar, confírmame que creaste 1 épica y 4 elementos hijos, con sus claves.
```

---

## Prompt 7 · Observabilidad, auditoría y calidad

Crea 1 épica y 4 elementos hijos (21 story points).

```text
Crea una épica y sus elementos hijos en el proyecto MatchPoint. Respeta los valores exactos y no inventes nada.

ÉPICA
Resumen: Observabilidad, auditoría y calidad
Tipo: Epic | Prioridad: High | Estado: Done | Componente: transversal | Sprint: Sprint 5
Etiquetas: transversal, calidad
Descripción:
Estándar de logging de una línea, auditoría de todas las escrituras, manejo global de excepciones y suite de pruebas con umbral de cobertura atado al build.

DENTRO DE ESA ÉPICA, CREA ESTOS 4 ELEMENTOS:

1. Estándar de logging de una sola línea
   Tipo: Story | Story points: 5 | Prioridad: Medium | Estado: Done | Sprint: Sprint 5 | Componente: transversal | Responsable: Josué Herrera
   Etiquetas: logging, observabilidad, RF-33
   Descripción:
   Como equipo queremos que al disparar una petición se vea de inmediato qué pasó, quién lo hizo y qué SQL se ejecutó.

   Criterios de aceptación:
   - Formato fijo: timestamp | LEVEL | servicio | sub= | logger | event= | msg= | clave=valor
   - Nunca se omite un campo; sin usuario autenticado se escribe sub=anonimo
   - El sub del JWT se pone en el MDC desde un filtro, una sola vez por servicio
   - Toda petición deja línea de entrada y de salida, incluidas las que terminan en 401 y 403
   - Un token manipulado muere antes del filtro: ese 401 lo registra LoggingAuthenticationEntryPoint
   - El log de nginx usa el mismo formato
   - Prohibida la barra vertical dentro de un valor
   - Nunca se loguean contraseñas, tokens completos, correos ni teléfonos enteros

   Evidencia: ApiLoggingFilter · LogEvents · LoggingAccessDeniedHandler · LogEventsTest

   Rúbrica: RNF-02 RNF-09 RNF-11 · RF-33

2. Auditoría de todas las escrituras
   Tipo: Story | Story points: 5 | Prioridad: Medium | Estado: Done | Sprint: Sprint 5 | Componente: transversal | Responsable: Josué Herrera
   Etiquetas: auditoría, RF-32, ADR-008
   Descripción:
   Como dueño de cancha quiero poder demostrar quién reservó y cuándo, porque el conflicto más frecuente es 'yo sí reservé' contra 'nadie reservó'.

   Criterios de aceptación:
   - Cada INSERT, UPDATE y DELETE deja fila en audit_log
   - Registra quién (user_sub y user_name del token), qué (entity_name, entity_id, action), cuándo y valores antes/después
   - Correos y teléfonos se guardan enmascarados
   - audit_log NO tiene FK: el rastro sobrevive al borrado de la fila auditada
   - Las pruebas verifican que cada mutación audita y que cada rechazo NO audita

   Evidencia: AuditService · AuditServiceTest

   Rúbrica: RF-32 · ADR-008

3. Manejo global de excepciones y códigos HTTP semánticos
   Tipo: Story | Story points: 3 | Prioridad: Medium | Estado: Done | Sprint: Sprint 5 | Componente: transversal | Responsable: Josué Herrera
   Etiquetas: errores, RNF-14
   Descripción:
   Como consumidor de la API quiero errores consistentes y con el código correcto.

   Criterios de aceptación:
   - Excepciones propias agrupadas por el código HTTP que significan
   - Un único @RestControllerAdvice las traduce: 400, 403, 404, 409 y 503
   - Cero try/catch de traducción en controladores y services
   - Formato de error uniforme
   - Todo rechazo se registra con su event y su status automáticamente

   Evidencia: GlobalExceptionHandler · BookingExceptions · TournamentExceptions · IntegrationExceptions

   Rúbrica: 3.3 · RNF-14

4. Suite de pruebas y umbral de cobertura atado al build
   Tipo: Story | Story points: 8 | Prioridad: High | Estado: Done | Sprint: Sprint 5 | Componente: transversal | Responsable: Josué Herrera
   Etiquetas: testing, cobertura, RNF-08, ADR-010
   Descripción:
   Como equipo queremos que la cobertura no pueda degradarse en silencio.

   Criterios de aceptación:
   - 214 pruebas: 157 en matchpoint y 57 en users
   - Unitarias de service con mockito-kotlin, sin contexto de Spring
   - Funcionales de endpoint con @WebMvcTest, MockMvc y spring-security-test
   - Pruebas explícitas de 401 sin token y 403 con rol equivocado en los dos servicios
   - Cada regla de negocio tiene prueba de aceptación Y prueba de rechazo
   - Las pruebas no necesitan AWS ni PostgreSQL: JwtDecoder mockeado y H2 solo en test
   - ./gradlew check falla si la cobertura de líneas baja del 100 %
   - Exclusiones declaradas en build.gradle.kts, no en un documento aparte

   Evidencia: build.gradle.kts · jacocoTestCoverageVerification

   Rúbrica: 1.3 3.5 · RNF-08 · ADR-010

Al terminar, confírmame que creaste 1 épica y 4 elementos hijos, con sus claves.
```

---

## Prompt 8 · Infraestructura, contenedores y gateway

Crea 1 épica y 5 elementos hijos (17 story points).

```text
Crea una épica y sus elementos hijos en el proyecto MatchPoint. Respeta los valores exactos y no inventes nada.

ÉPICA
Resumen: Infraestructura, contenedores y gateway
Tipo: Epic | Prioridad: High | Estado: Done | Componente: infra | Sprint: Sprint 5
Etiquetas: devops, docker
Descripción:
Sistema multicapa contenedorizado: seis servicios, red interna, volúmenes con nombre, healthchecks y un único puerto publicado.

DENTRO DE ESA ÉPICA, CREA ESTOS 5 ELEMENTOS:

1. Contenedorización con build multi-stage
   Tipo: Story | Story points: 5 | Prioridad: High | Estado: Done | Sprint: Sprint 5 | Componente: infra | Responsable: Josué Herrera
   Etiquetas: docker, RNF-10
   Descripción:
   Como equipo queremos que el sistema levante en cualquier máquina con Docker, sin JDK, Gradle ni PostgreSQL instalados.

   Criterios de aceptación:
   - Dockerfile multi-stage: se compila con JDK 21 y se ejecuta sobre JRE 21
   - La imagen final no lleva código fuente ni Gradle
   - Las dependencias se copian antes que src para aprovechar la caché de capas
   - Todas las imágenes con versión fija: nunca latest
   - docker compose up -d --build levanta los seis servicios
   - Funciona en macOS, Linux y Windows con WSL2

   Evidencia: matchpoint/Dockerfile · users/Dockerfile · docker-compose.yml

   Rúbrica: 4.2 · RNF-05 RNF-10

2. Gateway nginx como único punto de entrada
   Tipo: Story | Story points: 5 | Prioridad: High | Estado: Done | Sprint: Sprint 5 | Componente: infra | Responsable: Josué Herrera
   Etiquetas: nginx, gateway, RNF-03, ADR-009
   Descripción:
   Como responsable de seguridad quiero un solo puerto abierto en lugar de cinco.

   Criterios de aceptación:
   - ports: aparece UNA sola vez en docker-compose.yml, en nginx
   - Los microservicios, las bases y pgAdmin usan expose:
   - Enrutamiento por prefijo: /users, /matchpoint, /pgadmin
   - Endpoint /health propio del gateway
   - access_log a stdout con el mismo formato del estándar del proyecto
   - Verificable: curl al puerto interno falla, vía 9090 responde 200

   Evidencia: nginx/nginx.conf · nginx/proxy_headers.conf

   Rúbrica: 4.2 · RNF-03 · ADR-009

3. Base de datos por servicio con healthchecks
   Tipo: Story | Story points: 3 | Prioridad: Medium | Estado: Done | Sprint: Sprint 5 | Componente: infra | Responsable: Josué Herrera
   Etiquetas: postgres, RNF-04, ADR-003
   Descripción:
   Como arquitectura queremos aislamiento real de datos entre microservicios.

   Criterios de aceptación:
   - Dos contenedores PostgreSQL 16 con credenciales, base y volumen distintos
   - Cero FK entre bases, cero JOIN entre dominios
   - healthcheck con pg_isready en las dos
   - App depende de su base con service_healthy; entre microservicios con service_started
   - Logging del motor: log_statement=all, log_duration=on, log_min_duration_statement=0
   - Volúmenes con nombre: el dato sobrevive a docker compose down

   Evidencia: docker-compose.yml

   Rúbrica: 3.1 4.2 · RNF-04 RNF-06 · ADR-003

4. Explorador de base de datos detrás del gateway
   Tipo: Story | Story points: 2 | Prioridad: Medium | Estado: Done | Sprint: Sprint 5 | Componente: infra | Responsable: Josué Herrera
   Etiquetas: pgadmin, devops
   Descripción:
   Como evaluador quiero inspeccionar las dos bases sin que ninguna abra un puerto propio.

   Criterios de aceptación:
   - pgAdmin accesible en /pgadmin/ a través de nginx
   - servers.json deja las dos conexiones ya registradas
   - pgpass no se versiona: solo se versiona la plantilla .example
   - SCRIPT_NAME configurado para funcionar bajo prefijo de ruta

   Evidencia: pgadmin/servers.json · docker-compose.yml

   Rúbrica: 4.2

5. Gestión de secretos fuera del repositorio
   Tipo: Story | Story points: 2 | Prioridad: Medium | Estado: Done | Sprint: Sprint 5 | Componente: infra | Responsable: Josué Herrera
   Etiquetas: security, RNF-13
   Descripción:
   Como equipo no queremos ningún secreto versionado.

   Criterios de aceptación:
   - .env, pgadmin/pgpass y el client secret de Cognito están en .gitignore
   - Se versionan solo las plantillas .example
   - Variables obligatorias con sintaxis :? el stack no arranca si falta un secreto
   - git log -p no contiene ninguna clave

   Evidencia: .gitignore · .env.example

   Rúbrica: RNF-13

Al terminar, confírmame que creaste 1 épica y 5 elementos hijos, con sus claves.
```

---

## Prompt 9 · Documentación y entrega académica

Crea 1 épica y 13 elementos hijos (36 story points).

```text
Crea una épica y sus elementos hijos en el proyecto MatchPoint. Respeta los valores exactos y no inventes nada.

ÉPICA
Resumen: Documentación y entrega académica
Tipo: Epic | Prioridad: High | Estado: Done | Componente: docs | Sprint: Sprint 6
Etiquetas: docs, entrega
Descripción:
Entregables de la rúbrica P02: análisis, arquitectura, nube, emprendimiento y sustentación.

DENTRO DE ESA ÉPICA, CREA ESTOS 13 ELEMENTOS:

1. Levantamiento de requerimientos RF y RNF
   Tipo: Task | Story points: 3 | Prioridad: Medium | Estado: Done | Sprint: Sprint 6 | Componente: docs | Responsable: Josué Herrera
   Etiquetas: docs, entrega
   Descripción:
   33 requerimientos funcionales y 16 no funcionales, cada uno con descripción precisa, prioridad, evidencia en código y estado. Incluye 14 reglas de negocio y la matriz de trazabilidad requerimiento → historia → rama.

   Rúbrica: 1.1
   Archivo: entrega/01-analisis/REQUERIMIENTOS.md

2. Tabla y detalle de casos de uso
   Tipo: Task | Story points: 3 | Prioridad: Medium | Estado: Done | Sprint: Sprint 6 | Componente: docs | Responsable: Josué Herrera
   Etiquetas: docs, entrega
   Descripción:
   14 casos de uso con actor, precondiciones, flujo principal, flujos alternativos y postcondición, más el diagrama de casos de uso y la matriz caso de uso × código HTTP.

   Rúbrica: 1.1
   Archivo: entrega/01-analisis/CASOS-DE-USO.md

3. Documentación del manejo de GitFlow
   Tipo: Task | Story points: 2 | Prioridad: Medium | Estado: Done | Sprint: Sprint 6 | Componente: docs | Responsable: Josué Herrera
   Etiquetas: docs, entrega
   Descripción:
   Ramas permanentes y temporales, convención de nombres y de commits, ciclo de vida de una historia, explicación de por qué HU-01 a HU-04 aparecen dos veces, reglas de convivencia, comandos de verificación y autocrítica (incluida la identidad de git mal configurada en cinco commits).

   Rúbrica: 1.2
   Archivo: entrega/01-analisis/GITFLOW.md

4. Estrategia y evidencia de pruebas unitarias
   Tipo: Task | Story points: 2 | Prioridad: Medium | Estado: Done | Sprint: Sprint 6 | Componente: docs | Responsable: Josué Herrera
   Etiquetas: docs, entrega
   Descripción:
   Distribución de las 214 pruebas, regla del par (caso válido e inválido), uso de mocks y stubs, aserciones significativas y explicación de los dos números de cobertura.

   Rúbrica: 1.3 3.5
   Archivo: entrega/01-analisis/PRUEBAS-UNITARIAS.md

5. Registros de decisiones de arquitectura (ADR)
   Tipo: Task | Story points: 5 | Prioridad: Medium | Estado: Done | Sprint: Sprint 6 | Componente: docs | Responsable: Josué Herrera
   Etiquetas: docs, entrega
   Descripción:
   Criterio de priorización con tres factores y once ADR con contexto, decisión, alternativas descartadas, consecuencias y puesta en práctica en el código.

   Rúbrica: 1.4
   Archivo: entrega/01-analisis/adr/

6. Documento de Arquitectura Empresarial
   Tipo: Task | Story points: 3 | Prioridad: Medium | Estado: Done | Sprint: Sprint 6 | Componente: docs | Responsable: Josué Herrera
   Etiquetas: docs, entrega
   Descripción:
   Recorrido de los seis sub-criterios con evidencia por archivo: modelo de datos, capas, lógica de negocio y errores, calidad del código, pruebas y seguridad.

   Rúbrica: 3.1 a 3.6
   Archivo: entrega/03-arquitectura/ARQUITECTURA-EMPRESARIAL.md

7. Documento de Computación en la Nube
   Tipo: Task | Story points: 3 | Prioridad: Medium | Estado: Done | Sprint: Sprint 6 | Componente: docs | Responsable: Josué Herrera
   Etiquetas: docs, entrega
   Descripción:
   Conceptos IaaS/PaaS/SaaS, contenedores frente a máquinas virtuales, ventajas y desventajas del escalamiento vertical y horizontal, evidencia de contenedorización y arquitectura AWS objetivo con costos.

   Rúbrica: 4.1 4.2 4.3
   Archivo: entrega/04-nube/COMPUTACION-EN-LA-NUBE.md

8. Business Model Canvas
   Tipo: Task | Story points: 3 | Prioridad: Medium | Estado: Done | Sprint: Sprint 6 | Componente: docs | Responsable: Josué Herrera
   Etiquetas: docs, entrega
   Descripción:
   Los nueve bloques con detalle por segmento, punto de equilibrio, métricas, riesgos y la tabla de qué promesas del canvas ya funcionan en el producto.

   Rúbrica: 5.1
   Archivo: entrega/05-emprendimiento/BUSINESS-MODEL-CANVAS.md

9. Propuesta tecnológica con innovación
   Tipo: Task | Story points: 2 | Prioridad: Medium | Estado: Done | Sprint: Sprint 6 | Componente: docs | Responsable: Josué Herrera
   Etiquetas: docs, entrega
   Descripción:
   Factores que condicionaron cada elección, stack justificado, ocho puntos de innovación separados entre implementado y diseñado, hoja de ruta y deuda técnica reconocida.

   Rúbrica: 5.2
   Archivo: entrega/05-emprendimiento/PROPUESTA-TECNOLOGICA.md

10. Planificación financiera a tres años
   Tipo: Task | Story points: 3 | Prioridad: Medium | Estado: Done | Sprint: Sprint 6 | Componente: docs | Responsable: Josué Herrera
   Etiquetas: docs, entrega
   Descripción:
   Inversión inicial, supuestos declarados, flujo de caja mes a mes del año 1, estado de resultados, punto de equilibrio, indicadores SaaS y análisis de sensibilidad.

   Rúbrica: 5.3
   Archivo: entrega/05-emprendimiento/PLAN-FINANCIERO.md

11. Guion de sustentación y banco de preguntas
   Tipo: Task | Story points: 2 | Prioridad: Medium | Estado: Done | Sprint: Sprint 6 | Componente: docs | Responsable: Josué Herrera
   Etiquetas: docs, entrega
   Descripción:
   Reparto y tiempos por bloque, guion literal, banco de preguntas por asignatura con respuestas, glosario técnico y preparación para claridad y seguridad.

   Rúbrica: 6.1 a 6.5
   Archivo: entrega/06-sustentacion/GUION-SUSTENTACION.md

12. Colección de Postman de punta a punta
   Tipo: Task | Story points: 3 | Prioridad: Medium | Estado: Done | Sprint: Sprint 6 | Componente: docs | Responsable: Josué Herrera
   Etiquetas: postman, demo
   Descripción:
   Colección con siete carpetas que recorren el sistema completo, con aserciones pm.test y encadenamiento automático de identificadores.

   Criterios de aceptación:
   - Carpeta 0: login USER_AUTH de los dos roles, firmado con SECRET_HASH
   - Carpetas 1 a 5: perfiles, canchas, reservas, torneo completo e identidad
   - Carpeta 6: limpieza para poder volver a ejecutar la colección entera
   - Apunta a nginx mediante baseUrl, nunca a puertos internos
   - Cubre 200/201/204, 400, 401, 403 por rol, 403 por propiedad, 404, 409 y 503
   - Ningún valor secreto versionado

   Evidencia: postman/matchpoint.postman_collection.json

13. Diagramas del modelo entidad-relación
   Tipo: Task | Story points: 2 | Prioridad: Medium | Estado: Done | Sprint: Sprint 6 | Componente: docs | Responsable: Josué Herrera
   Etiquetas: docs, modelo
   Descripción:
   Diagramas ER de las dos bases y de la frontera entre microservicios, exportados en SVG y PNG con su fuente Mermaid, más la tabla de cardinalidades en palabras.

   Criterios de aceptación:
   - er-users_db, er-matchpoint_db y frontera-microservicios en .mmd, .svg y .png
   - El diagrama coincide con las FK reales creadas por Hibernate, verificable con ERD For Database en pgAdmin

   Evidencia: docs/MODELO-ER.md · docs/er/

Al terminar, confírmame que creaste 1 épica y 13 elementos hijos, con sus claves.
```

---

## Prompt 10 · Higiene del repositorio y GitFlow

Crea 1 épica y 6 elementos hijos (12 story points).

```text
Crea una épica y sus elementos hijos en el proyecto MatchPoint. Respeta los valores exactos y no inventes nada.

ÉPICA
Resumen: Higiene del repositorio y GitFlow
Tipo: Epic | Prioridad: Highest | Estado: To Do | Componente: infra | Sprint: Sprint 6
Etiquetas: git, gitflow
Descripción:
Trabajo pendiente de integración: 103 archivos sin commitear, la identidad de git sin configurar y la rama main cinco commits por detrás de develop. Es lo que evalúa el criterio 1.2.

DENTRO DE ESA ÉPICA, CREA ESTOS 6 ELEMENTOS:

1. Configurar la identidad de git en las máquinas del equipo
   Tipo: Task | Story points: 1 | Prioridad: Highest | Estado: To Do | Sprint: Sprint 6 | Componente: infra | Responsable: Josué Herrera
   Etiquetas: git, gitflow
   Descripción:
   Cinco commits quedaron con el autor por defecto 'Your Name <your-email@example.com>' y el initial import como 'GitHub Copilot'. Sin corregir la configuración, los commits nuevos seguirán saliendo igual y el criterio 1.2 no puede atribuir el trabajo.

   Criterios de aceptación:
   - git config user.name y user.email fijados en las dos máquinas antes del siguiente commit
   - Verificado con: git log -1 --format='%an <%ae>'
   - Los commits afectados quedan declarados en la autocrítica de GITFLOW.md con su autor real
   - NO se reescribe la historia publicada: rompería los hashes citados en toda la documentación

   Evidencia: entrega/01-analisis/GITFLOW.md sección 8

   Rúbrica: 1.2

2. Integrar limpieza de código y configuración
   Tipo: Task | Story points: 3 | Prioridad: Highest | Estado: To Do | Sprint: Sprint 6 | Componente: infra | Responsable: Josué Herrera
   Etiquetas: git, gitflow
   Descripción:
   Rama feature/limpieza-codigo-y-configuracion, 5 commits.

   Criterios de aceptación:
   - chore(repo): eliminar directorio duplicado matchpoint/matchpoint
   - refactor(matchpoint): retirar comentarios redundantes del código fuente
   - refactor(users): retirar comentarios redundantes y ajustar recursos de prueba
   - chore(infra): ajustar docker-compose, Dockerfiles y variables de entorno
   - chore(gateway): afinar configuración de nginx y cabeceras de proxy
   - Merge --ff-only a develop y rama publicada en origin

   Rúbrica: 1.2

3. Integrar herramientas de demostración
   Tipo: Task | Story points: 2 | Prioridad: High | Estado: To Do | Sprint: Sprint 6 | Componente: infra | Responsable: Josué Herrera
   Etiquetas: git, gitflow
   Descripción:
   Rama feature/herramientas-de-demo, 2 commits. scripts/cognito-token.sh nunca se commiteó.

   Criterios de aceptación:
   - feat(scripts): añadir obtención de token de Cognito desde terminal
   - chore(postman): actualizar colección y environment de la demo
   - Merge --ff-only a develop y rama publicada en origin

   Rúbrica: 1.2

4. Integrar documentación técnica del proyecto
   Tipo: Task | Story points: 2 | Prioridad: High | Estado: To Do | Sprint: Sprint 6 | Componente: infra | Responsable: Josué Herrera
   Etiquetas: git, gitflow
   Descripción:
   Rama feature/documentacion-tecnica, 3 commits. MODELO-ER.md, docs/er/ y DEMO.md nunca se commitearon.

   Criterios de aceptación:
   - docs: añadir modelo entidad-relación con diagramas exportados
   - docs: añadir guion de demostración paso a paso
   - docs: actualizar README con logging, cobertura y modelo de datos
   - Merge --ff-only a develop y rama publicada en origin

   Rúbrica: 1.2

5. Integrar la documentación de la entrega P02
   Tipo: Task | Story points: 3 | Prioridad: Highest | Estado: To Do | Sprint: Sprint 6 | Componente: infra | Responsable: Josué Herrera
   Etiquetas: git, gitflow
   Descripción:
   Rama feature/entrega-p02, 3 commits. Toda la carpeta entrega/.

   Criterios de aceptación:
   - docs(entrega): añadir requerimientos, casos de uso, GitFlow, pruebas y ADR
   - docs(entrega): añadir arquitectura empresarial, computación en la nube y emprendimiento
   - docs(entrega): añadir guion de sustentación y backlog importable a Jira
   - Merge --ff-only a develop y rama publicada en origin

   Rúbrica: 1.2

6. Actualizar main con el estado de develop
   Tipo: Task | Story points: 1 | Prioridad: High | Estado: To Do | Sprint: Sprint 6 | Componente: infra | Responsable: Josué Herrera
   Etiquetas: git, gitflow
   Descripción:
   main está en 2414dd7, cinco commits por detrás. Cada llegada a main equivale a una entrega.

   Criterios de aceptación:
   - git merge --ff-only develop desde main
   - git push origin main
   - Verificado: git log --oneline origin/main..origin/develop devuelve vacío

   Rúbrica: 1.2

Al terminar, confírmame que creaste 1 épica y 6 elementos hijos, con sus claves.
```

---

## Prompt 11 · App móvil Android

Crea 1 épica y 5 elementos hijos (37 story points).

```text
Crea una épica y sus elementos hijos en el proyecto MatchPoint. Respeta los valores exactos y no inventes nada.

ÉPICA
Resumen: App móvil Android
Tipo: Epic | Prioridad: High | Estado: To Do | Componente: movil | Sprint: Sprint 7
Etiquetas: movil, android
Descripción:
Cliente Android nativo en Kotlin que consume la misma API por el gateway, con login de Cognito. NOTA: lo desarrolla otro integrante. Si ya lo lleva en su propio tablero, borrar esta épica y sus historias antes de importar.

DENTRO DE ESA ÉPICA, CREA ESTOS 5 ELEMENTOS:

1. Autenticación con Cognito desde la app
   Tipo: Story | Story points: 8 | Prioridad: Medium | Estado: To Do | Sprint: Sprint 7 | Componente: movil
   Etiquetas: movil, android
   Descripción:
   Login contra el User Pool y almacenamiento seguro del token, con renovación antes de expirar.

   Rúbrica: 2.2

2. Catálogo de canchas y búsqueda de disponibilidad
   Tipo: Story | Story points: 8 | Prioridad: Medium | Estado: To Do | Sprint: Sprint 7 | Componente: movil
   Etiquetas: movil, android
   Descripción:
   Pantalla pública que consume GET /matchpoint/courts/available con filtros por sector, deporte y franja horaria.

   Rúbrica: 2.1 2.2

3. Flujo de reserva con validaciones de formulario
   Tipo: Story | Story points: 8 | Prioridad: Medium | Estado: To Do | Sprint: Sprint 7 | Componente: movil
   Etiquetas: movil, android
   Descripción:
   Formulario de reserva con validación de campos y manejo de los códigos 409 de solapamiento y cancha inactiva.

   Rúbrica: 2.3

4. Mis reservas y cancelación
   Tipo: Story | Story points: 5 | Prioridad: Medium | Estado: To Do | Sprint: Sprint 7 | Componente: movil
   Etiquetas: movil, android
   Descripción:
   Listado de reservas propias y cancelación desde la app.

   Rúbrica: 2.1

5. Vista del cuadro de torneo
   Tipo: Story | Story points: 8 | Prioridad: Medium | Estado: To Do | Sprint: Sprint 7 | Componente: movil
   Etiquetas: movil, android
   Descripción:
   Visualización del cuadro por rondas, con marcadores y campeón.

   Rúbrica: 2.1 2.4

Al terminar, confírmame que creaste 1 épica y 5 elementos hijos, con sus claves.
```

---

## Prompt 12 · Despliegue en la nube (AWS)

Crea 1 épica y 6 elementos hijos (44 story points).

```text
Crea una épica y sus elementos hijos en el proyecto MatchPoint. Respeta los valores exactos y no inventes nada.

ÉPICA
Resumen: Despliegue en la nube (AWS)
Tipo: Epic | Prioridad: Medium | Estado: To Do | Componente: infra | Sprint: Backlog
Etiquetas: cloud, aws, devops
Descripción:
Llevar el sistema contenedorizado a AWS por fases, según la arquitectura diseñada en el criterio 4.3.

DENTRO DE ESA ÉPICA, CREA ESTOS 6 ELEMENTOS:

1. Fase 1: EC2 con Docker Compose y RDS Single-AZ
   Tipo: Story | Story points: 8 | Prioridad: Highest | Estado: To Do | Sprint: Backlog | Componente: infra
   Etiquetas: cloud, aws
   Descripción:
   Primer despliegue real con URL pública y base gestionada. Estimado: 1 día.

   Criterios de aceptación:
   - Instancia EC2 con Docker y la imagen desde ECR
   - RDS PostgreSQL 16 Single-AZ, accesible solo desde el grupo de seguridad de la app
   - Secretos en Secrets Manager, no en .env
   - Registro DNS y certificado TLS
   - Costo objetivo: 40 USD/mes

2. Fase 2: ALB, Auto Scaling Group y RDS Multi-AZ
   Tipo: Story | Story points: 13 | Prioridad: High | Estado: To Do | Sprint: Backlog | Componente: infra
   Etiquetas: cloud, aws
   Descripción:
   Alta disponibilidad y escalado automático. Estimado: 3 días.

   Criterios de aceptación:
   - VPC con subredes públicas, privadas de aplicación y de datos, en dos AZ
   - Grupos de seguridad encadenados por referencia, no por CIDR
   - ALB con TLS y enrutamiento por prefijo de ruta
   - ASG con mínimo 2, deseado 2, máximo 6; escala a CPU > 70 % durante 5 minutos
   - RDS Multi-AZ con conmutación automática
   - Costo objetivo: 127 USD/mes

3. Pipeline CI/CD con despliegue azul-verde
   Tipo: Story | Story points: 8 | Prioridad: Medium | Estado: To Do | Sprint: Backlog | Componente: infra
   Etiquetas: cloud, aws
   Descripción:
   Automatizar build, pruebas y despliegue sin indisponibilidad. Estimado: 3 días.

   Criterios de aceptación:
   - El pipeline ejecuta ./gradlew check antes de construir la imagen
   - Imagen etiquetada por versión y publicada en ECR
   - Despliegue azul-verde con reversión a la imagen anterior en 5 minutos

4. Integración continua con GitHub Actions
   Tipo: Story | Story points: 5 | Prioridad: High | Estado: To Do | Sprint: Backlog | Componente: infra
   Etiquetas: cloud, aws
   Descripción:
   Convertir la regla 'nada se mezcla en rojo' en algo que el repositorio hace cumplir. Deuda técnica reconocida.

   Criterios de aceptación:
   - Workflow que ejecuta ./gradlew check en cada push y en cada pull request
   - El check bloquea el merge si falla
   - Reporte de cobertura publicado como artefacto

5. Migraciones versionadas con Flyway
   Tipo: Story | Story points: 5 | Prioridad: High | Estado: To Do | Sprint: Backlog | Componente: infra
   Etiquetas: cloud, aws
   Descripción:
   Sustituir ddl-auto=update por migraciones reversibles. Deuda técnica reconocida.

   Criterios de aceptación:
   - Esquema inicial como migración V1
   - ddl-auto pasa a validate
   - Cada cambio de esquema es una migración versionada y revisable en el diff

6. Centralización de logs y alarmas en CloudWatch
   Tipo: Story | Story points: 5 | Prioridad: Medium | Estado: To Do | Sprint: Backlog | Componente: infra
   Etiquetas: cloud, aws
   Descripción:
   Los contenedores ya escriben a stdout: solo falta el agente y las alarmas.

   Criterios de aceptación:
   - Logs de los contenedores en CloudWatch Logs
   - Alarmas de CPU, errores 5xx y latencia del ALB
   - Las alarmas de CPU disparan la política de escalado

Al terminar, confírmame que creaste 1 épica y 6 elementos hijos, con sus claves.
```

---

## Prompt 13 · Producto y modelo de negocio

Crea 1 épica y 7 elementos hijos (68 story points).

```text
Crea una épica y sus elementos hijos en el proyecto MatchPoint. Respeta los valores exactos y no inventes nada.

ÉPICA
Resumen: Producto y modelo de negocio
Tipo: Epic | Prioridad: Medium | Estado: To Do | Componente: producto | Sprint: Backlog
Etiquetas: producto, negocio
Descripción:
Funcionalidades que convierten el backend en un negocio: monetización, retención y captación.

DENTRO DE ESA ÉPICA, CREA ESTOS 7 ELEMENTOS:

1. Reporte mensual de ocupación para el dueño de cancha
   Tipo: Story | Story points: 8 | Prioridad: Highest | Estado: To Do | Sprint: Backlog | Componente: producto
   Etiquetas: producto, negocio
   Descripción:
   Es la herramienta de retención: le muestra en números lo que gana con la plataforma. El dato ya se registra en reservations y audit_log; falta la consulta y la vista.

   Criterios de aceptación:
   - Horas ocupadas y horas muertas por cancha y por franja
   - Ingreso estimado del mes y comparación con el anterior
   - Envío automático el primer día de cada mes

2. Integración de pasarela de pagos local
   Tipo: Story | Story points: 13 | Prioridad: High | Estado: To Do | Sprint: Backlog | Componente: producto
   Etiquetas: producto, negocio
   Descripción:
   Habilita la comisión del 5 % sobre reservas pagadas en línea (fase 2 del modelo de ingresos).

   Criterios de aceptación:
   - Pago en línea opcional al crear la reserva
   - La reserva queda provisional hasta confirmar el pago
   - Conciliación y reporte de comisiones
   - Nota: la pasarela cobra ~3,5 %; el margen neto de la comisión es delgado y por eso la suscripción sigue siendo el modelo principal

3. Precio sugerido por franja horaria
   Tipo: Story | Story points: 13 | Prioridad: Medium | Estado: To Do | Sprint: Backlog | Componente: producto
   Etiquetas: producto, negocio
   Descripción:
   Con el histórico de ocupación, sugerir al dueño un precio por franja para llenar horas muertas.

   Criterios de aceptación:
   - Análisis de ocupación por cancha, día de la semana y hora
   - Sugerencia de precio con el impacto estimado en ocupación
   - El dueño acepta o rechaza; nunca se cambia el precio automáticamente

4. Emparejamiento de jugadores sueltos
   Tipo: Story | Story points: 13 | Prioridad: Medium | Estado: To Do | Sprint: Backlog | Componente: producto
   Etiquetas: producto, negocio
   Descripción:
   El caso más frecuente en cancha de barrio: cinco personas quieren jugar y no son diez. Es lo que genera el efecto de red del lado de la demanda.

   Criterios de aceptación:
   - Un jugador publica una convocatoria con cancha, fecha y cupo
   - Otros jugadores se suman hasta completar el cupo
   - Al completarse, la reserva se confirma sola y el costo se divide
   - Se apoya en el modelo de reservas ya construido: es una reserva provisional

5. Modo sin conexión en la app móvil
   Tipo: Story | Story points: 8 | Prioridad: Low | Estado: To Do | Sprint: Backlog | Componente: producto
   Etiquetas: producto, negocio
   Descripción:
   En la cancha la señal es mala. El backend ya lo soporta sin cambios porque es stateless y el token es autocontenido.

   Criterios de aceptación:
   - La app cachea el catálogo consultado
   - La reserva se encola si no hay señal y se envía al recuperarla
   - El usuario ve claramente el estado: encolada, enviada, confirmada o rechazada

6. Captación de las 10 canchas fundadoras
   Tipo: Story | Story points: 8 | Prioridad: Highest | Estado: To Do | Sprint: Backlog | Componente: producto
   Etiquetas: producto, negocio
   Descripción:
   Actividad clave de la fase 1: sin inventario no hay producto. Venta directa en cancha.

   Criterios de aceptación:
   - 10 dueños dados de alta con sus canchas y horarios cargados
   - Seis meses gratis a cambio de retroalimentación semanal
   - Concentradas en un solo sector de la ciudad para lograr densidad
   - Meta de uso: más de 25 reservas por cancha al mes

7. Soporte de deportes adicionales
   Tipo: Story | Story points: 5 | Prioridad: Low | Estado: To Do | Sprint: Backlog | Componente: producto
   Etiquetas: producto, negocio
   Descripción:
   El enum SportType ya está preparado; hoy solo expone BASKET. Amplía el mercado direccionable.

   Criterios de aceptación:
   - Nuevos valores en SportType con su tipo de piso asociado
   - Filtros del catálogo y de disponibilidad funcionando por deporte
   - Sin migración destructiva de datos existentes

Al terminar, confírmame que creaste 1 épica y 7 elementos hijos, con sus claves.
```

---

## Prompt final · Verificación

Pégalo al terminar, para detectar lo que se haya perdido por el camino.

```text
Ya cargué todo el backlog de MatchPoint. Verifica y dime, sin arreglar nada todavía:

1. ¿Cuántos elementos hay en total? Deberían ser 80: 13 épicas, 45 historias, 19 tareas y 3 subtareas.
2. ¿Cuántos story points suman en total? Deberían ser 336.
3. ¿Cuántos elementos están en Done? Deberían ser 52. ¿Y en To Do? Deberían ser 28.
4. Lista los elementos que NO tengan story points asignados, o que no tengan sprint.
5. Lista las historias que hayan quedado sueltas, sin épica padre.
6. Dime los puntos por sprint. Deberían ser:
   - Sprint 1: 19 puntos
   - Sprint 2: 28 puntos
   - Sprint 3: 20 puntos
   - Sprint 4: 34 puntos
   - Sprint 5: 38 puntos
   - Sprint 6: 48 puntos
   - Sprint 7: 37 puntos
   - Backlog: 112 puntos

Preséntamelo como una tabla de esperado contra encontrado, marcando las diferencias.
```

---

## Si algo salió mal

| Síntoma | Qué hacer |
|---|---|
| La IA creó menos elementos de los pedidos | Corta el prompt por la mitad y mándalo en dos tandas. Es el fallo más común en las épicas grandes (la 9 tiene 13 elementos). |
| Los story points quedaron vacíos | Pídeselo aparte: «asigna estos story points: <resumen> = 5, <resumen> = 8…». O edítalos a mano desde el backlog. |
| Todo quedó en el primer estado | Pídele que mueva a Done los elementos de los sprints 1 a 5 y los de documentación del Sprint 6. |
| Las descripciones salieron resumidas | Es lo que peor hace. Reenvía ese elemento solo, con «copia la descripción literal, sin resumir». |
| Se duplicaron elementos | Pídele que liste duplicados por resumen y los borre. Revisa antes de aceptar. |

**Si acumulas más de dos o tres de estos problemas, sale más a cuenta borrar el proyecto y volver a empezar con el CSV.** No es una derrota: el importador existe justamente para esto.
