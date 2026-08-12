#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
MatchPoint · Genera el backlog completo en dos formatos desde una sola fuente:

  - jira-import.csv   listo para importar en Jira
  - BACKLOG.md        legible, con todo el contenido de cada issue

Uso:  python3 generar-backlog.py
No requiere dependencias externas.
"""

import csv
import os
import re
from collections import Counter, OrderedDict

JOSUE = "Josué Herrera"
FER = "Fernando Socasi"

rows = []
_id = [0]


def nid():
    _id[0] += 1
    return str(_id[0])


def add(tipo, resumen, descripcion, parent=None, prioridad="Medium", etiquetas=(),
        puntos="", estado="To Do", componente="", epica="", responsable="", sprint=""):
    iid = nid()
    rows.append(OrderedDict([
        ("Issue Id", iid), ("Parent Id", parent or ""), ("Issue Type", tipo),
        ("Summary", resumen), ("Description", descripcion), ("Priority", prioridad),
        ("Status", estado), ("Story Points", puntos), ("Component", componente),
        ("Epic Name", epica), ("Assignee", responsable), ("Sprint", sprint),
        ("labels", list(etiquetas)),
    ]))
    return iid


def ac(*items):
    return "\n".join("- " + i for i in items)


def desc(objetivo, criterios, evidencia=None, rubrica=None):
    p = [objetivo, "", "*Criterios de aceptación:*", criterios]
    if evidencia:
        p += ["", "*Evidencia:* " + evidencia]
    if rubrica:
        p += ["", "*Rúbrica:* " + rubrica]
    return "\n".join(p)


# ══════════════════════════════════════════════════════ ÉPICA 1
e1 = add("Epic", "Identidad y perfiles de usuario",
         "Microservicio users: registro, consulta, actualización y administración de perfiles. "
         "El perfil se ancla al claim sub del JWT de Cognito; no almacena contraseñas ni roles.",
         epica="Identidad y perfiles", etiquetas=["users", "backend"],
         prioridad="Highest", estado="Done", componente="users", responsable=FER, sprint="Sprint 1")

add("Story", "HU-01 Registro automático de perfil de usuario",
    desc("Como usuario autenticado quiero registrar mi perfil para poder operar en la plataforma, "
         "sin escribir mi identificador porque el sistema lo toma del token.",
         ac("POST /users/me crea el perfil tomando cognito_id y username de los claims del JWT, no del body",
            "GET /users/me devuelve el perfil asociado al sub del token",
            "Un mismo usuario de Cognito no puede tener dos perfiles: responde 409",
            "Nombre en blanco responde 400",
            "Sin token responde 401"),
         "UserController.createMyProfile · UserService.createUser · UserControllerTest",
         "1.1 · RF-01 RF-02"),
    parent=e1, prioridad="Highest", puntos="5", estado="Done", componente="users",
    etiquetas=["users", "RF-01", "RF-02", "HU-01"], responsable=FER, sprint="Sprint 1")

add("Story", "HU-02 Actualización de datos de contacto",
    desc("Como jugador quiero actualizar mi nombre, correo y teléfono para mantener mis datos al día.",
         ac("PUT /users/me actualiza únicamente el perfil propio, resuelto por el sub del token",
            "El nombre no puede quedar en blanco: 400",
            "Si el usuario nunca se registró: 404",
            "El cambio queda en audit_log con valores anteriores y nuevos enmascarados",
            "No existe endpoint para editar el perfil de otro usuario"),
         "UserService.updateUser · BlankNameException", "1.1 · RF-03"),
    parent=e1, puntos="3", estado="Done", componente="users",
    etiquetas=["users", "RF-03", "HU-02"], responsable=FER, sprint="Sprint 1")

add("Story", "HU-03 Administración de perfiles exclusiva de MANAGER",
    desc("Como MANAGER quiero listar, consultar y dar de baja perfiles para administrar la plataforma.",
         ac("GET /users devuelve todos los perfiles solo para MANAGER; un PLAYER recibe 403",
            "GET /users/{id} devuelve un perfil concreto solo para MANAGER",
            "DELETE /users/{id} elimina el perfil y responde 204",
            "La baja deja registro DELETE en audit_log con los valores previos",
            "audit_log no tiene FK hacia users: el rastro sobrevive al borrado"),
         "SecurityConfig hasRole(MANAGER) · UserService.deleteUser", "1.1 · RF-04 RF-05 RF-06"),
    parent=e1, puntos="5", estado="Done", componente="users",
    etiquetas=["users", "RF-04", "RF-05", "RF-06", "HU-03"], responsable=FER, sprint="Sprint 2")

add("Story", "Resolución de perfil por identificador de Cognito",
    desc("Como microservicio matchpoint necesito resolver un perfil a partir del cognitoId "
         "para validar que el jugador está registrado antes de crear una reserva.",
         ac("GET /users/cognito/{cognitoId} disponible para cualquier usuario autenticado",
            "Devuelve 404 si no existe perfil para ese identificador",
            "Es el endpoint que consume UsersClient desde matchpoint"),
         "UserController · UsersClient", "1.1 · RF-07"),
    parent=e1, puntos="2", estado="Done", componente="users",
    etiquetas=["users", "RF-07", "integración"], responsable=FER, sprint="Sprint 2")

# ══════════════════════════════════════════════════════ ÉPICA 2
e2 = add("Epic", "Seguridad: autenticación y autorización",
         "Identidad delegada a AWS Cognito. Cada microservicio es un OAuth2 Resource Server que valida "
         "firma, emisor y expiración contra el JWKS. Autorización en dos capas: rol y propiedad del recurso.",
         epica="Seguridad", etiquetas=["security", "backend"],
         prioridad="Highest", estado="Done", componente="transversal", responsable=FER, sprint="Sprint 1")

add("Story", "HU-04 Validación de JWT y JWKS con AWS Cognito",
    desc("Como sistema necesito validar cada token contra el User Pool para no confiar en nadie sin "
         "identidad verificada. PRIMERA historia del proyecto: ninguna otra se puede probar sin ella (ADR-001).",
         ac("Cada microservicio configura issuer-uri y descarga el JWKS del User Pool",
            "Se valida firma, emisor y expiración de cada token",
            "Token ausente, expirado o manipulado responde 401",
            "No existe JWT propio, ni sesión, ni Basic Auth, ni usuarios en memoria",
            "Los dos servicios arman el issuer con las mismas variables: un token que uno acepta el otro también",
            "GET /matchpoint/me con token real devuelve username, sub y groups"),
         "SecurityConfig (ambos servicios) · LoggingAuthenticationEntryPoint · commit 92e4e56",
         "3.6 · RF-08 · ADR-001 ADR-002"),
    parent=e2, prioridad="Highest", puntos="8", estado="Done", componente="transversal",
    etiquetas=["security", "cognito", "RF-08", "HU-04", "ADR-001"], responsable=FER, sprint="Sprint 1")

add("Story", "Mapeo de cognito:groups a roles de Spring Security",
    desc("Como sistema necesito traducir los grupos de Cognito a autoridades para restringir endpoints por rol.",
         ac("El claim cognito:groups se traduce a ROLE_MANAGER / ROLE_PLAYER",
            "El conversor es idéntico en los dos microservicios",
            "Un rol equivocado en un endpoint protegido responde 403 con event=authz.denied",
            "Si el claim no existe, el usuario queda autenticado sin autoridades"),
         "CognitoGroupsConverter · CognitoGroupsConverterTest", "3.6 · RF-09"),
    parent=e2, prioridad="Highest", puntos="3", estado="Done", componente="transversal",
    etiquetas=["security", "cognito", "RF-09"], responsable=FER, sprint="Sprint 1")

add("Story", "Autorización por propiedad del recurso",
    desc("Como dueño de un recurso quiero que nadie más pueda modificarlo, aunque tenga mi mismo rol. "
         "Segunda capa de 403, decidida en el service (ADR-007).",
         ac("findCourtOwnedBy, findReservationOwnedBy y findTournamentOwnedBy comparan contra el username del token",
            "Un MANAGER no puede editar la cancha de otro MANAGER: 403",
            "Un PLAYER no puede cancelar la reserva de otro: 403",
            "Los DTO de request NO tienen campo de propietario: la suplantación por body es imposible",
            "El log distingue event=ownership.denied de event=authz.denied"),
         "CourtService · ReservationService · TournamentService · GlobalExceptionHandler",
         "3.6 · RF-10 · ADR-007"),
    parent=e2, prioridad="Highest", puntos="5", estado="Done", componente="transversal",
    etiquetas=["security", "RF-10", "ADR-007"], responsable=JOSUE, sprint="Sprint 2")

# ══════════════════════════════════════════════════════ ÉPICA 3
e3 = add("Epic", "Gestión de canchas",
         "Publicación, catálogo público y motor de búsqueda de disponibilidad por franja horaria. "
         "El catálogo es público sin token: es el escaparate del producto.",
         epica="Gestión de canchas", etiquetas=["matchpoint", "backend"],
         prioridad="High", estado="Done", componente="matchpoint", responsable=JOSUE, sprint="Sprint 2")

add("Story", "HU-05 Publicación y actualización de canchas",
    desc("Como MANAGER quiero publicar mis canchas y ajustar precio y disponibilidad "
         "para que los jugadores puedan encontrarlas y reservarlas.",
         ac("POST /matchpoint/courts crea la cancha; el managerUser sale del claim del token",
            "Nombre, sector y tipo de piso son obligatorios: 400 si faltan",
            "El precio por hora debe ser mayor que 0: 400",
            "PATCH /matchpoint/courts/{id} actualiza precio y estado activo, solo del dueño",
            "Cancha ajena: 403 · Cancha inexistente: 404 · Un PLAYER: 403",
            "Toda mutación deja fila en audit_log"),
         "CourtService.createCourt · CourtService.updateCourt · CourtServiceTest",
         "3.3 · RF-11 RF-15"),
    parent=e3, prioridad="High", puntos="5", estado="Done", componente="matchpoint",
    etiquetas=["matchpoint", "canchas", "RF-11", "RF-15", "HU-05"], responsable=JOSUE, sprint="Sprint 2")

add("Story", "HU-06 Motor de búsqueda de disponibilidad",
    desc("Como jugador quiero ver qué canchas están libres en una franja horaria concreta "
         "para no tener que llamar a cinco números.",
         ac("GET /matchpoint/courts/available acepta sector, sport, startsAt y durationMinutes",
            "Devuelve SOLO canchas activas",
            "Descarta canchas con reserva CONFIRMED solapada: startsAt < reserva.endsAt AND reserva.startsAt < endsAt",
            "Sin franja horaria devuelve todas las activas que cumplen los filtros",
            "Endpoint público, sin token",
            "Lista vacía responde 200: no es un error"),
         "CourtService.availableCourts · CourtService.isFree · 19 pruebas en CourtServiceTest",
         "3.3 · RF-14"),
    parent=e3, prioridad="Highest", puntos="8", estado="Done", componente="matchpoint",
    etiquetas=["matchpoint", "canchas", "RF-14", "HU-06"], responsable=JOSUE, sprint="Sprint 2")

add("Story", "Catálogo público de canchas con filtros",
    desc("Como visitante quiero ver el catálogo sin crear cuenta, porque pedir registro antes de "
         "mostrar valor es la mayor fricción de un marketplace.",
         ac("GET /matchpoint/courts es público y admite filtros por sector y deporte",
            "GET /matchpoint/courts/{id} es público; 404 si no existe",
            "No consulta Cognito ni al microservicio users: resuelve contra la base local"),
         "SecurityConfig permitAll · CourtService.listCourts", "3.3 · RF-12 RF-13"),
    parent=e3, puntos="3", estado="Done", componente="matchpoint",
    etiquetas=["matchpoint", "canchas", "RF-12", "RF-13"], responsable=JOSUE, sprint="Sprint 2")

# ══════════════════════════════════════════════════════ ÉPICA 4
e4 = add("Epic", "Motor de reservas",
         "Creación de reservas con validación de solapamiento y de perfil remoto, listado y cancelación. "
         "Es el único flujo que cruza la frontera entre los dos microservicios.",
         epica="Motor de reservas", etiquetas=["matchpoint", "backend"],
         prioridad="Highest", estado="Done", componente="matchpoint", responsable=JOSUE, sprint="Sprint 3")

hu07 = add("Story", "HU-07 Creación de reserva con validación de solapamiento",
           desc("Como jugador quiero reservar una cancha en una franja concreta y que el sistema impida "
                "que dos personas reserven la misma hora. Es la regla que le vende el producto al dueño.",
                ac("POST /matchpoint/reservations crea la reserva con ownerUser del token",
                   "La cancha debe existir (404) y estar activa (409)",
                   "durationMinutes debe ser mayor que 0 (400)",
                   "No puede existir ninguna reserva CONFIRMED solapada en esa cancha (409)",
                   "El solapamiento se evalúa con inicioA < finB AND inicioB < finA, cubriendo contención parcial",
                   "Un MANAGER recibe 403"),
                "ReservationService.createReservation · ReservationServiceTest (12 pruebas)",
                "3.3 · RF-16 RF-17 · RN-01 RN-02"),
           parent=e4, prioridad="Highest", puntos="8", estado="Done", componente="matchpoint",
           etiquetas=["matchpoint", "reservas", "RF-17", "HU-07"], responsable=JOSUE, sprint="Sprint 3")

add("Sub-task", "Validar perfil llamando al microservicio users",
    desc("Antes de guardar la reserva, matchpoint pide el perfil del jugador a users propagando el token del usuario.",
         ac("GET /users/me con la cabecera Authorization original reenviada",
            "Timeout explícito de 3 segundos",
            "Si el jugador no tiene perfil: 409",
            "Si users no responde: 503, y matchpoint se recupera solo",
            "El nombre devuelto se copia en reservations.owner_name",
            "NUNCA se fabrica un token de servicio"),
         "UsersClient · HttpClientConfig · UsersClientTest (7 pruebas)", "3.3 · RF-16 · ADR-004"),
    parent=hu07, prioridad="Highest", puntos="5", estado="Done", componente="matchpoint",
    etiquetas=["integración", "RF-16", "ADR-004"], responsable=JOSUE, sprint="Sprint 3")

add("Sub-task", "Sembrar datos que disparan cada validación de reserva",
    desc("data.sql debe incluir los casos que permiten demostrar cada regla sin preparar nada.",
         ac("Cancha 3 inactiva: POST /reservations responde 409",
            "Reserva 1 ocupando una franja: misma franja responde 409",
            "Reserva 4 de player_luis: DELETE responde 403 por propiedad",
            "Los INSERT usan ON CONFLICT DO NOTHING: reiniciar no duplica")),
    parent=hu07, puntos="2", estado="Done", componente="matchpoint",
    etiquetas=["datos", "demo"], responsable=JOSUE, sprint="Sprint 3")

add("Story", "HU-08 Consulta y cancelación de reservas propias",
    desc("Como jugador quiero ver y cancelar mis reservas, y que la franja cancelada vuelva a estar disponible.",
         ac("GET /matchpoint/reservations/me devuelve solo las propias, ordenadas por fecha descendente",
            "GET /matchpoint/reservations/{id} responde 403 si la reserva es de otro jugador",
            "DELETE /matchpoint/reservations/{id} marca CANCELLED: borrado lógico, la fila se conserva",
            "La franja cancelada vuelve a estar libre: la disponibilidad solo mira reservas CONFIRMED",
            "El cambio queda en audit_log con el estado anterior"),
         "ReservationService.listMine · ReservationService.cancel", "3.3 · RF-18 RF-19 RF-20 · ADR-008"),
    parent=e4, prioridad="High", puntos="5", estado="Done", componente="matchpoint",
    etiquetas=["matchpoint", "reservas", "RF-20", "HU-08", "ADR-008"], responsable=JOSUE, sprint="Sprint 3")

# ══════════════════════════════════════════════════════ ÉPICA 5
e5 = add("Epic", "Torneos e inscripción de equipos",
         "Creación de torneos de eliminación directa, inscripción y retiro de equipos, y consulta pública del progreso.",
         epica="Torneos", etiquetas=["matchpoint", "backend"],
         prioridad="High", estado="Done", componente="matchpoint", responsable=JOSUE, sprint="Sprint 4")

add("Story", "HU-09 Creación de torneos",
    desc("Como MANAGER quiero crear un torneo de eliminación directa con su cupo y su sede opcional.",
         ac("POST /matchpoint/tournaments crea el torneo en estado REGISTRATION",
            "maxTeams debe ser potencia de dos entre 2 y 32: cualquier otro valor responde 400",
            "El mensaje de error enumera los valores válidos (2, 4, 8, 16, 32)",
            "Si se indica sede, la cancha debe existir (404) y ser del propio MANAGER (403)",
            "Un PLAYER recibe 403"),
         "TournamentService.createTournament · isPowerOfTwo · resolveCourt", "3.3 · RF-21 · ADR-006"),
    parent=e5, prioridad="High", puntos="5", estado="Done", componente="matchpoint",
    etiquetas=["matchpoint", "torneos", "RF-21", "HU-09", "ADR-006"], responsable=JOSUE, sprint="Sprint 4")

add("Story", "HU-10 Inscripción y retiro de equipos",
    desc("Como jugador quiero inscribir mi equipo en un torneo abierto y poder retirarlo antes de que arranque.",
         ac("POST /tournaments/{id}/teams inscribe con registeredByUser del token",
            "Torneo que ya arrancó o terminó: 409",
            "Cupo lleno: 409",
            "Nombre de equipo repetido dentro del torneo: 409",
            "Datos de contacto en blanco: 400",
            "DELETE .../teams/{teamId} solo mientras el torneo siga en REGISTRATION (409 si no)",
            "Solo puede retirarlo quien lo inscribió: 403",
            "El correo se enmascara en el log y en audit_log"),
         "TournamentService.registerTeam · TournamentService.withdrawTeam", "3.3 · RF-23 RF-24"),
    parent=e5, prioridad="High", puntos="5", estado="Done", componente="matchpoint",
    etiquetas=["matchpoint", "torneos", "RF-23", "RF-24", "HU-10"], responsable=JOSUE, sprint="Sprint 4")

add("Story", "Consulta pública de torneos y del cuadro",
    desc("Como visitante quiero ver el cuadro de un torneo sin cuenta, para poder compartirlo por WhatsApp.",
         ac("GET /matchpoint/tournaments lista torneos con su conteo de equipos inscritos",
            "GET /matchpoint/tournaments/{id} devuelve estado, equipos, rondas y campeón",
            "Las rondas llevan nombre legible: Final, Semifinals, Quarterfinals, Round of 16, Round of 32",
            "Torneo en REGISTRATION devuelve lista de rondas vacía, no error",
            "Endpoints públicos, sin token"),
         "TournamentService.getProgress · buildRounds · roundName", "3.3 · RF-22 RF-25"),
    parent=e5, puntos="3", estado="Done", componente="matchpoint",
    etiquetas=["matchpoint", "torneos", "RF-22", "RF-25"], responsable=JOSUE, sprint="Sprint 4")

# ══════════════════════════════════════════════════════ ÉPICA 6
e6 = add("Epic", "Cuadro de partidos y avance automático",
         "Generación del cuadro completo al arrancar, programación de partidos, registro de marcador "
         "y avance automático del ganador hasta el campeón.",
         epica="Cuadro de partidos", etiquetas=["matchpoint", "backend"],
         prioridad="High", estado="Done", componente="matchpoint", responsable=JOSUE, sprint="Sprint 4")

hu11 = add("Story", "HU-11 Generación automática del cuadro",
           desc("Como MANAGER quiero arrancar el torneo y que el sistema arme todas las llaves, "
                "para no dibujar el cuadro a mano.",
                ac("POST /tournaments/{id}/start exige exactamente maxTeams equipos inscritos: 409 si faltan o sobran",
                   "El torneo debe estar en REGISTRATION: 409 si ya arrancó",
                   "Solo el MANAGER dueño del torneo: 403",
                   "La primera ronda se arma por orden de inscripción, en estado READY",
                   "Las rondas siguientes se crean como slots vacíos en estado PENDING",
                   "El torneo pasa a IN_PROGRESS y devuelve el cuadro completo"),
                "TournamentService.startTournament · TournamentServiceTest (46 pruebas)",
                "3.3 · RF-26 · ADR-006"),
           parent=e6, prioridad="High", puntos="8", estado="Done", componente="matchpoint",
           etiquetas=["matchpoint", "partidos", "RF-26", "HU-11", "ADR-006"], responsable=JOSUE, sprint="Sprint 4")

add("Sub-task", "Calcular número de rondas y nombres legibles",
    desc("El total de rondas se deriva del cupo, que ya se validó como potencia de dos.",
         ac("totalRounds = Integer.numberOfTrailingZeros(maxTeams)",
            "roundName traduce la distancia a la final en nombre legible",
            "Cubierto por pruebas para cuadros de 2, 4, 8, 16 y 32 equipos")),
    parent=hu11, puntos="2", estado="Done", componente="matchpoint",
    etiquetas=["matchpoint", "partidos"], responsable=JOSUE, sprint="Sprint 4")

add("Story", "HU-12 Registro de marcador y avance del ganador",
    desc("Como MANAGER quiero registrar el marcador y que el ganador avance solo a la siguiente ronda.",
         ac("PATCH /matchpoint/matches/{id}/score registra el marcador",
            "Partido PENDING sin los dos equipos: 409",
            "Partido ya PLAYED: 409",
            "Marcadores negativos: 400",
            "Empate: 400, porque en eliminación directa siempre hay ganador",
            "Se actualizan estadísticas de ambos equipos: jugados, ganados, perdidos, puntos a favor y en contra",
            "El perdedor queda eliminated",
            "El ganador se mueve al slot (ronda+1, posición/2): local si la posición era par, visitante si impar",
            "El slot padre pasa a READY cuando reúne a sus dos equipos",
            "Si era la última ronda, el torneo pasa a FINISHED con championTeam"),
         "TournamentService.registerScore · advanceWinner · applyStats",
         "3.3 · RF-28 RF-29 RF-30 · RN-08 RN-09 RN-10"),
    parent=e6, prioridad="Highest", puntos="8", estado="Done", componente="matchpoint",
    etiquetas=["matchpoint", "partidos", "RF-28", "RF-29", "HU-12"], responsable=JOSUE, sprint="Sprint 4")

add("Story", "Programación de fecha y hora de partidos",
    desc("Como MANAGER quiero fijar cuándo se juega cada partido de mi torneo.",
         ac("PATCH /matchpoint/matches/{id}/schedule fija scheduledAt",
            "Solo el MANAGER dueño del torneo: 403",
            "Un partido ya jugado no se reprograma: 409"),
         "TournamentService.scheduleMatch", "3.3 · RF-27"),
    parent=e6, puntos="3", estado="Done", componente="matchpoint",
    etiquetas=["matchpoint", "partidos", "RF-27"], responsable=JOSUE, sprint="Sprint 4")

# ══════════════════════════════════════════════════════ ÉPICA 7
e7 = add("Epic", "Observabilidad, auditoría y calidad",
         "Estándar de logging de una línea, auditoría de todas las escrituras, manejo global de excepciones "
         "y suite de pruebas con umbral de cobertura atado al build.",
         epica="Observabilidad y calidad", etiquetas=["transversal", "calidad"],
         prioridad="High", estado="Done", componente="transversal", responsable=JOSUE, sprint="Sprint 5")

add("Story", "Estándar de logging de una sola línea",
    desc("Como equipo queremos que al disparar una petición se vea de inmediato qué pasó, quién lo hizo "
         "y qué SQL se ejecutó.",
         ac("Formato fijo: timestamp | LEVEL | servicio | sub= | logger | event= | msg= | clave=valor",
            "Nunca se omite un campo; sin usuario autenticado se escribe sub=anonimo",
            "El sub del JWT se pone en el MDC desde un filtro, una sola vez por servicio",
            "Toda petición deja línea de entrada y de salida, incluidas las que terminan en 401 y 403",
            "Un token manipulado muere antes del filtro: ese 401 lo registra LoggingAuthenticationEntryPoint",
            "El log de nginx usa el mismo formato",
            "Prohibida la barra vertical dentro de un valor",
            "Nunca se loguean contraseñas, tokens completos, correos ni teléfonos enteros"),
         "ApiLoggingFilter · LogEvents · LoggingAccessDeniedHandler · LogEventsTest",
         "RNF-02 RNF-09 RNF-11 · RF-33"),
    parent=e7, puntos="5", estado="Done", componente="transversal",
    etiquetas=["logging", "observabilidad", "RF-33"], responsable=JOSUE, sprint="Sprint 5")

add("Story", "Auditoría de todas las escrituras",
    desc("Como dueño de cancha quiero poder demostrar quién reservó y cuándo, porque el conflicto más "
         "frecuente es 'yo sí reservé' contra 'nadie reservó'.",
         ac("Cada INSERT, UPDATE y DELETE deja fila en audit_log",
            "Registra quién (user_sub y user_name del token), qué (entity_name, entity_id, action), cuándo y valores antes/después",
            "Correos y teléfonos se guardan enmascarados",
            "audit_log NO tiene FK: el rastro sobrevive al borrado de la fila auditada",
            "Las pruebas verifican que cada mutación audita y que cada rechazo NO audita"),
         "AuditService · AuditServiceTest", "RF-32 · ADR-008"),
    parent=e7, puntos="5", estado="Done", componente="transversal",
    etiquetas=["auditoría", "RF-32", "ADR-008"], responsable=JOSUE, sprint="Sprint 5")

add("Story", "Manejo global de excepciones y códigos HTTP semánticos",
    desc("Como consumidor de la API quiero errores consistentes y con el código correcto.",
         ac("Excepciones propias agrupadas por el código HTTP que significan",
            "Un único @RestControllerAdvice las traduce: 400, 403, 404, 409 y 503",
            "Cero try/catch de traducción en controladores y services",
            "Formato de error uniforme",
            "Todo rechazo se registra con su event y su status automáticamente"),
         "GlobalExceptionHandler · BookingExceptions · TournamentExceptions · IntegrationExceptions",
         "3.3 · RNF-14"),
    parent=e7, puntos="3", estado="Done", componente="transversal",
    etiquetas=["errores", "RNF-14"], responsable=JOSUE, sprint="Sprint 5")

add("Story", "Suite de pruebas y umbral de cobertura atado al build",
    desc("Como equipo queremos que la cobertura no pueda degradarse en silencio.",
         ac("214 pruebas: 157 en matchpoint y 57 en users",
            "Unitarias de service con mockito-kotlin, sin contexto de Spring",
            "Funcionales de endpoint con @WebMvcTest, MockMvc y spring-security-test",
            "Pruebas explícitas de 401 sin token y 403 con rol equivocado en los dos servicios",
            "Cada regla de negocio tiene prueba de aceptación Y prueba de rechazo",
            "Las pruebas no necesitan AWS ni PostgreSQL: JwtDecoder mockeado y H2 solo en test",
            "./gradlew check falla si la cobertura de líneas baja del 100 %",
            "Exclusiones declaradas en build.gradle.kts, no en un documento aparte"),
         "build.gradle.kts · jacocoTestCoverageVerification", "1.3 3.5 · RNF-08 · ADR-010"),
    parent=e7, prioridad="High", puntos="8", estado="Done", componente="transversal",
    etiquetas=["testing", "cobertura", "RNF-08", "ADR-010"], responsable=JOSUE, sprint="Sprint 5")

# ══════════════════════════════════════════════════════ ÉPICA 8
e8 = add("Epic", "Infraestructura, contenedores y gateway",
         "Sistema multicapa contenedorizado: seis servicios, red interna, volúmenes con nombre, "
         "healthchecks y un único puerto publicado.",
         epica="Infraestructura", etiquetas=["devops", "docker"],
         prioridad="High", estado="Done", componente="infra", responsable=JOSUE, sprint="Sprint 5")

add("Story", "Contenedorización con build multi-stage",
    desc("Como equipo queremos que el sistema levante en cualquier máquina con Docker, "
         "sin JDK, Gradle ni PostgreSQL instalados.",
         ac("Dockerfile multi-stage: se compila con JDK 21 y se ejecuta sobre JRE 21",
            "La imagen final no lleva código fuente ni Gradle",
            "Las dependencias se copian antes que src para aprovechar la caché de capas",
            "Todas las imágenes con versión fija: nunca latest",
            "docker compose up -d --build levanta los seis servicios",
            "Funciona en macOS, Linux y Windows con WSL2"),
         "matchpoint/Dockerfile · users/Dockerfile · docker-compose.yml", "4.2 · RNF-05 RNF-10"),
    parent=e8, prioridad="High", puntos="5", estado="Done", componente="infra",
    etiquetas=["docker", "RNF-10"], responsable=JOSUE, sprint="Sprint 5")

add("Story", "Gateway nginx como único punto de entrada",
    desc("Como responsable de seguridad quiero un solo puerto abierto en lugar de cinco.",
         ac("ports: aparece UNA sola vez en docker-compose.yml, en nginx",
            "Los microservicios, las bases y pgAdmin usan expose:",
            "Enrutamiento por prefijo: /users, /matchpoint, /pgadmin",
            "Endpoint /health propio del gateway",
            "access_log a stdout con el mismo formato del estándar del proyecto",
            "Verificable: curl al puerto interno falla, vía 9090 responde 200"),
         "nginx/nginx.conf · nginx/proxy_headers.conf", "4.2 · RNF-03 · ADR-009"),
    parent=e8, prioridad="High", puntos="5", estado="Done", componente="infra",
    etiquetas=["nginx", "gateway", "RNF-03", "ADR-009"], responsable=JOSUE, sprint="Sprint 5")

add("Story", "Base de datos por servicio con healthchecks",
    desc("Como arquitectura queremos aislamiento real de datos entre microservicios.",
         ac("Dos contenedores PostgreSQL 16 con credenciales, base y volumen distintos",
            "Cero FK entre bases, cero JOIN entre dominios",
            "healthcheck con pg_isready en las dos",
            "App depende de su base con service_healthy; entre microservicios con service_started",
            "Logging del motor: log_statement=all, log_duration=on, log_min_duration_statement=0",
            "Volúmenes con nombre: el dato sobrevive a docker compose down"),
         "docker-compose.yml", "3.1 4.2 · RNF-04 RNF-06 · ADR-003"),
    parent=e8, puntos="3", estado="Done", componente="infra",
    etiquetas=["postgres", "RNF-04", "ADR-003"], responsable=JOSUE, sprint="Sprint 5")

add("Story", "Explorador de base de datos detrás del gateway",
    desc("Como evaluador quiero inspeccionar las dos bases sin que ninguna abra un puerto propio.",
         ac("pgAdmin accesible en /pgadmin/ a través de nginx",
            "servers.json deja las dos conexiones ya registradas",
            "pgpass no se versiona: solo se versiona la plantilla .example",
            "SCRIPT_NAME configurado para funcionar bajo prefijo de ruta"),
         "pgadmin/servers.json · docker-compose.yml", "4.2"),
    parent=e8, puntos="2", estado="Done", componente="infra",
    etiquetas=["pgadmin", "devops"], responsable=JOSUE, sprint="Sprint 5")

add("Story", "Gestión de secretos fuera del repositorio",
    desc("Como equipo no queremos ningún secreto versionado.",
         ac(".env, pgadmin/pgpass y el client secret de Cognito están en .gitignore",
            "Se versionan solo las plantillas .example",
            "Variables obligatorias con sintaxis :? el stack no arranca si falta un secreto",
            "git log -p no contiene ninguna clave"),
         ".gitignore · .env.example", "RNF-13"),
    parent=e8, puntos="2", estado="Done", componente="infra",
    etiquetas=["security", "RNF-13"], responsable=JOSUE, sprint="Sprint 5")

# ══════════════════════════════════════════════════════ ÉPICA 9
e9 = add("Epic", "Documentación y entrega académica",
         "Entregables de la rúbrica P02: análisis, arquitectura, nube, emprendimiento y sustentación.",
         epica="Documentación", etiquetas=["docs", "entrega"],
         prioridad="High", estado="Done", componente="docs", responsable=JOSUE, sprint="Sprint 6")

docs = [
    ("Levantamiento de requerimientos RF y RNF",
     "33 requerimientos funcionales y 16 no funcionales, cada uno con descripción precisa, prioridad, "
     "evidencia en código y estado. Incluye 14 reglas de negocio y la matriz de trazabilidad "
     "requerimiento → historia → rama.", "3", "1.1", "entrega/01-analisis/REQUERIMIENTOS.md"),
    ("Tabla y detalle de casos de uso",
     "14 casos de uso con actor, precondiciones, flujo principal, flujos alternativos y postcondición, "
     "más el diagrama de casos de uso y la matriz caso de uso × código HTTP.",
     "3", "1.1", "entrega/01-analisis/CASOS-DE-USO.md"),
    ("Documentación del manejo de GitFlow",
     "Ramas permanentes y temporales, convención de nombres y de commits, ciclo de vida de una historia, "
     "explicación de por qué HU-01 a HU-04 aparecen dos veces, reglas de convivencia, comandos de "
     "verificación y autocrítica (incluida la identidad de git mal configurada en cinco commits).",
     "2", "1.2", "entrega/01-analisis/GITFLOW.md"),
    ("Estrategia y evidencia de pruebas unitarias",
     "Distribución de las 214 pruebas, regla del par (caso válido e inválido), uso de mocks y stubs, "
     "aserciones significativas y explicación de los dos números de cobertura.",
     "2", "1.3 3.5", "entrega/01-analisis/PRUEBAS-UNITARIAS.md"),
    ("Registros de decisiones de arquitectura (ADR)",
     "Criterio de priorización con tres factores y once ADR con contexto, decisión, alternativas "
     "descartadas, consecuencias y puesta en práctica en el código.",
     "5", "1.4", "entrega/01-analisis/adr/"),
    ("Documento de Arquitectura Empresarial",
     "Recorrido de los seis sub-criterios con evidencia por archivo: modelo de datos, capas, lógica de "
     "negocio y errores, calidad del código, pruebas y seguridad.",
     "3", "3.1 a 3.6", "entrega/03-arquitectura/ARQUITECTURA-EMPRESARIAL.md"),
    ("Documento de Computación en la Nube",
     "Conceptos IaaS/PaaS/SaaS, contenedores frente a máquinas virtuales, ventajas y desventajas del "
     "escalamiento vertical y horizontal, evidencia de contenedorización y arquitectura AWS objetivo "
     "con costos.", "3", "4.1 4.2 4.3", "entrega/04-nube/COMPUTACION-EN-LA-NUBE.md"),
    ("Business Model Canvas",
     "Los nueve bloques con detalle por segmento, punto de equilibrio, métricas, riesgos y la tabla de "
     "qué promesas del canvas ya funcionan en el producto.",
     "3", "5.1", "entrega/05-emprendimiento/BUSINESS-MODEL-CANVAS.md"),
    ("Propuesta tecnológica con innovación",
     "Factores que condicionaron cada elección, stack justificado, ocho puntos de innovación separados "
     "entre implementado y diseñado, hoja de ruta y deuda técnica reconocida.",
     "2", "5.2", "entrega/05-emprendimiento/PROPUESTA-TECNOLOGICA.md"),
    ("Planificación financiera a tres años",
     "Inversión inicial, supuestos declarados, flujo de caja mes a mes del año 1, estado de resultados, "
     "punto de equilibrio, indicadores SaaS y análisis de sensibilidad.",
     "3", "5.3", "entrega/05-emprendimiento/PLAN-FINANCIERO.md"),
    ("Guion de sustentación y banco de preguntas",
     "Reparto y tiempos por bloque, guion literal, banco de preguntas por asignatura con respuestas, "
     "glosario técnico y preparación para claridad y seguridad.",
     "2", "6.1 a 6.5", "entrega/06-sustentacion/GUION-SUSTENTACION.md"),
]
for titulo, detalle, pts, rub, ruta in docs:
    add("Task", titulo, detalle + "\n\n*Rúbrica:* " + rub + "\n*Archivo:* " + ruta,
        parent=e9, puntos=pts, estado="Done", componente="docs",
        etiquetas=["docs", "entrega"], responsable=JOSUE, sprint="Sprint 6")

add("Task", "Colección de Postman de punta a punta",
    desc("Colección con siete carpetas que recorren el sistema completo, con aserciones pm.test "
         "y encadenamiento automático de identificadores.",
         ac("Carpeta 0: login USER_AUTH de los dos roles, firmado con SECRET_HASH",
            "Carpetas 1 a 5: perfiles, canchas, reservas, torneo completo e identidad",
            "Carpeta 6: limpieza para poder volver a ejecutar la colección entera",
            "Apunta a nginx mediante baseUrl, nunca a puertos internos",
            "Cubre 200/201/204, 400, 401, 403 por rol, 403 por propiedad, 404, 409 y 503",
            "Ningún valor secreto versionado"),
         "postman/matchpoint.postman_collection.json"),
    parent=e9, puntos="3", estado="Done", componente="docs",
    etiquetas=["postman", "demo"], responsable=JOSUE, sprint="Sprint 6")

add("Task", "Diagramas del modelo entidad-relación",
    desc("Diagramas ER de las dos bases y de la frontera entre microservicios, exportados en SVG y PNG "
         "con su fuente Mermaid, más la tabla de cardinalidades en palabras.",
         ac("er-users_db, er-matchpoint_db y frontera-microservicios en .mmd, .svg y .png",
            "El diagrama coincide con las FK reales creadas por Hibernate, verificable con ERD For Database en pgAdmin"),
         "docs/MODELO-ER.md · docs/er/"),
    parent=e9, puntos="2", estado="Done", componente="docs",
    etiquetas=["docs", "modelo"], responsable=JOSUE, sprint="Sprint 6")

# ══════════════════════════════════════════════════════ ÉPICA 10
e10 = add("Epic", "Higiene del repositorio y GitFlow",
          "Trabajo pendiente de integración: 103 archivos sin commitear, la identidad de git sin configurar "
          "y la rama main cinco commits por detrás de develop. Es lo que evalúa el criterio 1.2.",
          epica="Higiene del repositorio", etiquetas=["git", "gitflow"],
          prioridad="Highest", estado="To Do", componente="infra", responsable=JOSUE, sprint="Sprint 6")

add("Task", "Configurar la identidad de git en las máquinas del equipo",
    desc("Cinco commits quedaron con el autor por defecto 'Your Name <your-email@example.com>' y el "
         "initial import como 'GitHub Copilot'. Sin corregir la configuración, los commits nuevos "
         "seguirán saliendo igual y el criterio 1.2 no puede atribuir el trabajo.",
         ac("git config user.name y user.email fijados en las dos máquinas antes del siguiente commit",
            "Verificado con: git log -1 --format='%an <%ae>'",
            "Los commits afectados quedan declarados en la autocrítica de GITFLOW.md con su autor real",
            "NO se reescribe la historia publicada: rompería los hashes citados en toda la documentación"),
         "entrega/01-analisis/GITFLOW.md sección 8", "1.2"),
    parent=e10, prioridad="Highest", puntos="1", estado="To Do", componente="infra",
    etiquetas=["git", "gitflow"], responsable=JOSUE, sprint="Sprint 6")

pend = [
    ("Integrar limpieza de código y configuración",
     "Rama feature/limpieza-codigo-y-configuracion, 5 commits.",
     ac("chore(repo): eliminar directorio duplicado matchpoint/matchpoint",
        "refactor(matchpoint): retirar comentarios redundantes del código fuente",
        "refactor(users): retirar comentarios redundantes y ajustar recursos de prueba",
        "chore(infra): ajustar docker-compose, Dockerfiles y variables de entorno",
        "chore(gateway): afinar configuración de nginx y cabeceras de proxy",
        "Merge --ff-only a develop y rama publicada en origin"), "3", "Highest"),
    ("Integrar herramientas de demostración",
     "Rama feature/herramientas-de-demo, 2 commits. scripts/cognito-token.sh nunca se commiteó.",
     ac("feat(scripts): añadir obtención de token de Cognito desde terminal",
        "chore(postman): actualizar colección y environment de la demo",
        "Merge --ff-only a develop y rama publicada en origin"), "2", "High"),
    ("Integrar documentación técnica del proyecto",
     "Rama feature/documentacion-tecnica, 3 commits. MODELO-ER.md, docs/er/ y DEMO.md nunca se commitearon.",
     ac("docs: añadir modelo entidad-relación con diagramas exportados",
        "docs: añadir guion de demostración paso a paso",
        "docs: actualizar README con logging, cobertura y modelo de datos",
        "Merge --ff-only a develop y rama publicada en origin"), "2", "High"),
    ("Integrar la documentación de la entrega P02",
     "Rama feature/entrega-p02, 3 commits. Toda la carpeta entrega/.",
     ac("docs(entrega): añadir requerimientos, casos de uso, GitFlow, pruebas y ADR",
        "docs(entrega): añadir arquitectura empresarial, computación en la nube y emprendimiento",
        "docs(entrega): añadir guion de sustentación y backlog importable a Jira",
        "Merge --ff-only a develop y rama publicada en origin"), "3", "Highest"),
    ("Actualizar main con el estado de develop",
     "main está en 2414dd7, cinco commits por detrás. Cada llegada a main equivale a una entrega.",
     ac("git merge --ff-only develop desde main",
        "git push origin main",
        "Verificado: git log --oneline origin/main..origin/develop devuelve vacío"), "1", "High"),
]
for titulo, detalle, criterios, pts, pri in pend:
    add("Task", titulo, detalle + "\n\n*Criterios de aceptación:*\n" + criterios + "\n\n*Rúbrica:* 1.2",
        parent=e10, prioridad=pri, puntos=pts, estado="To Do", componente="infra",
        etiquetas=["git", "gitflow"], responsable=JOSUE, sprint="Sprint 6")

# ══════════════════════════════════════════════════════ ÉPICA 11
e11 = add("Epic", "App móvil Android",
          "Cliente Android nativo en Kotlin que consume la misma API por el gateway, con login de Cognito. "
          "NOTA: lo desarrolla otro integrante. Si ya lo lleva en su propio tablero, borrar esta épica y sus "
          "historias antes de importar.",
          epica="App móvil", etiquetas=["movil", "android"],
          prioridad="High", estado="To Do", componente="movil", sprint="Sprint 7")

movil = [
    ("Autenticación con Cognito desde la app",
     "Login contra el User Pool y almacenamiento seguro del token, con renovación antes de expirar.",
     "8", "2.2"),
    ("Catálogo de canchas y búsqueda de disponibilidad",
     "Pantalla pública que consume GET /matchpoint/courts/available con filtros por sector, deporte y franja horaria.",
     "8", "2.1 2.2"),
    ("Flujo de reserva con validaciones de formulario",
     "Formulario de reserva con validación de campos y manejo de los códigos 409 de solapamiento y cancha inactiva.",
     "8", "2.3"),
    ("Mis reservas y cancelación",
     "Listado de reservas propias y cancelación desde la app.", "5", "2.1"),
    ("Vista del cuadro de torneo",
     "Visualización del cuadro por rondas, con marcadores y campeón.", "8", "2.1 2.4"),
]
for titulo, detalle, pts, rub in movil:
    add("Story", titulo, detalle + "\n\n*Rúbrica:* " + rub, parent=e11,
        puntos=pts, estado="To Do", componente="movil",
        etiquetas=["movil", "android"], sprint="Sprint 7")

# ══════════════════════════════════════════════════════ ÉPICA 12
e12 = add("Epic", "Despliegue en la nube (AWS)",
          "Llevar el sistema contenedorizado a AWS por fases, según la arquitectura diseñada en el criterio 4.3.",
          epica="Despliegue AWS", etiquetas=["cloud", "aws", "devops"],
          prioridad="Medium", estado="To Do", componente="infra", sprint="Backlog")

cloud = [
    ("Fase 1: EC2 con Docker Compose y RDS Single-AZ",
     "Primer despliegue real con URL pública y base gestionada. Estimado: 1 día.",
     ac("Instancia EC2 con Docker y la imagen desde ECR",
        "RDS PostgreSQL 16 Single-AZ, accesible solo desde el grupo de seguridad de la app",
        "Secretos en Secrets Manager, no en .env",
        "Registro DNS y certificado TLS",
        "Costo objetivo: 40 USD/mes"), "8", "Highest"),
    ("Fase 2: ALB, Auto Scaling Group y RDS Multi-AZ",
     "Alta disponibilidad y escalado automático. Estimado: 3 días.",
     ac("VPC con subredes públicas, privadas de aplicación y de datos, en dos AZ",
        "Grupos de seguridad encadenados por referencia, no por CIDR",
        "ALB con TLS y enrutamiento por prefijo de ruta",
        "ASG con mínimo 2, deseado 2, máximo 6; escala a CPU > 70 % durante 5 minutos",
        "RDS Multi-AZ con conmutación automática",
        "Costo objetivo: 127 USD/mes"), "13", "High"),
    ("Pipeline CI/CD con despliegue azul-verde",
     "Automatizar build, pruebas y despliegue sin indisponibilidad. Estimado: 3 días.",
     ac("El pipeline ejecuta ./gradlew check antes de construir la imagen",
        "Imagen etiquetada por versión y publicada en ECR",
        "Despliegue azul-verde con reversión a la imagen anterior en 5 minutos"), "8", "Medium"),
    ("Integración continua con GitHub Actions",
     "Convertir la regla 'nada se mezcla en rojo' en algo que el repositorio hace cumplir. Deuda técnica reconocida.",
     ac("Workflow que ejecuta ./gradlew check en cada push y en cada pull request",
        "El check bloquea el merge si falla",
        "Reporte de cobertura publicado como artefacto"), "5", "High"),
    ("Migraciones versionadas con Flyway",
     "Sustituir ddl-auto=update por migraciones reversibles. Deuda técnica reconocida.",
     ac("Esquema inicial como migración V1",
        "ddl-auto pasa a validate",
        "Cada cambio de esquema es una migración versionada y revisable en el diff"), "5", "High"),
    ("Centralización de logs y alarmas en CloudWatch",
     "Los contenedores ya escriben a stdout: solo falta el agente y las alarmas.",
     ac("Logs de los contenedores en CloudWatch Logs",
        "Alarmas de CPU, errores 5xx y latencia del ALB",
        "Las alarmas de CPU disparan la política de escalado"), "5", "Medium"),
]
for titulo, detalle, criterios, pts, pri in cloud:
    add("Story", titulo, detalle + "\n\n*Criterios de aceptación:*\n" + criterios,
        parent=e12, prioridad=pri, puntos=pts, estado="To Do", componente="infra",
        etiquetas=["cloud", "aws"], sprint="Backlog")

# ══════════════════════════════════════════════════════ ÉPICA 13
e13 = add("Epic", "Producto y modelo de negocio",
          "Funcionalidades que convierten el backend en un negocio: monetización, retención y captación.",
          epica="Producto y negocio", etiquetas=["producto", "negocio"],
          prioridad="Medium", estado="To Do", componente="producto", sprint="Backlog")

negocio = [
    ("Reporte mensual de ocupación para el dueño de cancha",
     "Es la herramienta de retención: le muestra en números lo que gana con la plataforma. El dato ya se "
     "registra en reservations y audit_log; falta la consulta y la vista.",
     ac("Horas ocupadas y horas muertas por cancha y por franja",
        "Ingreso estimado del mes y comparación con el anterior",
        "Envío automático el primer día de cada mes"), "8", "Highest"),
    ("Integración de pasarela de pagos local",
     "Habilita la comisión del 5 % sobre reservas pagadas en línea (fase 2 del modelo de ingresos).",
     ac("Pago en línea opcional al crear la reserva",
        "La reserva queda provisional hasta confirmar el pago",
        "Conciliación y reporte de comisiones",
        "Nota: la pasarela cobra ~3,5 %; el margen neto de la comisión es delgado y por eso la "
        "suscripción sigue siendo el modelo principal"), "13", "High"),
    ("Precio sugerido por franja horaria",
     "Con el histórico de ocupación, sugerir al dueño un precio por franja para llenar horas muertas.",
     ac("Análisis de ocupación por cancha, día de la semana y hora",
        "Sugerencia de precio con el impacto estimado en ocupación",
        "El dueño acepta o rechaza; nunca se cambia el precio automáticamente"), "13", "Medium"),
    ("Emparejamiento de jugadores sueltos",
     "El caso más frecuente en cancha de barrio: cinco personas quieren jugar y no son diez. Es lo que "
     "genera el efecto de red del lado de la demanda.",
     ac("Un jugador publica una convocatoria con cancha, fecha y cupo",
        "Otros jugadores se suman hasta completar el cupo",
        "Al completarse, la reserva se confirma sola y el costo se divide",
        "Se apoya en el modelo de reservas ya construido: es una reserva provisional"), "13", "Medium"),
    ("Modo sin conexión en la app móvil",
     "En la cancha la señal es mala. El backend ya lo soporta sin cambios porque es stateless y el token "
     "es autocontenido.",
     ac("La app cachea el catálogo consultado",
        "La reserva se encola si no hay señal y se envía al recuperarla",
        "El usuario ve claramente el estado: encolada, enviada, confirmada o rechazada"), "8", "Low"),
    ("Captación de las 10 canchas fundadoras",
     "Actividad clave de la fase 1: sin inventario no hay producto. Venta directa en cancha.",
     ac("10 dueños dados de alta con sus canchas y horarios cargados",
        "Seis meses gratis a cambio de retroalimentación semanal",
        "Concentradas en un solo sector de la ciudad para lograr densidad",
        "Meta de uso: más de 25 reservas por cancha al mes"), "8", "Highest"),
    ("Soporte de deportes adicionales",
     "El enum SportType ya está preparado; hoy solo expone BASKET. Amplía el mercado direccionable.",
     ac("Nuevos valores en SportType con su tipo de piso asociado",
        "Filtros del catálogo y de disponibilidad funcionando por deporte",
        "Sin migración destructiva de datos existentes"), "5", "Low"),
]
for titulo, detalle, criterios, pts, pri in negocio:
    add("Story", titulo, detalle + "\n\n*Criterios de aceptación:*\n" + criterios,
        parent=e13, prioridad=pri, puntos=pts, estado="To Do", componente="producto",
        etiquetas=["producto", "negocio"], sprint="Backlog")


# ══════════════════════════════════════════════════════ SPRINTS
# Iteraciones de una semana. "hoy" en el proyecto es 2026-08-11 (Sprint 6 en curso).
SPRINTS = [
    ("Sprint 1", "2026-07-07", "2026-07-13", "Cerrado",
     "Fundación: identidad antes que negocio",
     "Ninguna otra historia se puede probar sin la autenticación, así que va primera "
     "(ADR-001). Al cerrar el sprint, un token real de Cognito atraviesa el sistema y produce "
     "ROLE_MANAGER o ROLE_PLAYER.",
     "GET /matchpoint/me con token real devuelve username, sub y groups; sin token devuelve 401."),
    ("Sprint 2", "2026-07-14", "2026-07-20", "Cerrado",
     "Perfiles completos y catálogo de canchas",
     "Se cierra el microservicio users y se abre el dominio deportivo por donde entra el usuario: "
     "el catálogo público y el motor de disponibilidad.",
     "Un visitante sin cuenta consulta qué canchas están libres en una franja horaria."),
    ("Sprint 3", "2026-07-21", "2026-07-27", "Cerrado",
     "Motor de reservas de punta a punta",
     "El sprint de mayor riesgo: es el único flujo que cruza la frontera entre microservicios. "
     "Se cierra reservas completo antes de tocar torneos (ADR-005).",
     "Reservar, chocar contra el 409 de solapamiento, cancelar y volver a reservar la misma franja."),
    ("Sprint 4", "2026-07-28", "2026-08-03", "Cerrado",
     "Torneos y cuadro de eliminación directa",
     "El dominio más complejo, abordado cuando la infraestructura transversal ya estaba resuelta y "
     "solo había que aplicarla.",
     "Torneo de 4 equipos de punta a punta: crear, inscribir, arrancar, puntuar y coronar campeón."),
    ("Sprint 5", "2026-08-04", "2026-08-10", "Cerrado",
     "Observabilidad, calidad e infraestructura",
     "Lo que convierte el código en un sistema entregable: logging estándar, auditoría, umbral de "
     "cobertura atado al build y los seis contenedores con un solo puerto publicado.",
     "docker compose up levanta todo en healthy y ./gradlew check pasa en los dos servicios."),
    ("Sprint 6", "2026-08-11", "2026-08-17", "En curso",
     "Documentación de la entrega e higiene del repositorio",
     "Los once documentos de la rúbrica más el trabajo pendiente de integración: 103 archivos sin "
     "commitear, la identidad de git y main al día.",
     "Los entregables de la rúbrica versionados en develop, y main sin commits de diferencia."),
    ("Sprint 7", "2026-08-18", "2026-08-24", "Planificado",
     "App móvil Android",
     "Cliente nativo que consume la misma API por el gateway. Lo desarrolla el otro integrante.",
     "Login con Cognito, catálogo, reserva con validaciones y vista del cuadro desde el teléfono."),
    ("Backlog", "", "", "Sin planificar",
     "Despliegue en la nube y producto",
     "Lo que sigue después de la entrega académica: llevar el sistema a AWS y convertir el backend "
     "en un negocio.",
     "Sin compromiso de fecha: se planifica cuando termine la entrega."),
]

# ══════════════════════════════════════════════════════ SALIDA
AQUI = os.path.dirname(os.path.abspath(__file__))
byid = {r["Issue Id"]: r for r in rows}
maxlabels = max(len(r["labels"]) for r in rows)

# ---- CSV -------------------------------------------------------------------
header = ["Issue Id", "Parent Id", "Issue Type", "Summary", "Description", "Priority",
          "Status", "Story Points", "Component", "Epic Name", "Assignee", "Sprint"]
header += ["Labels"] * maxlabels

with open(os.path.join(AQUI, "jira-import.csv"), "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f, quoting=csv.QUOTE_MINIMAL, lineterminator="\r\n")
    w.writerow(header)
    for r in rows:
        base = [r[k] for k in header[:12]]
        labs = r["labels"] + [""] * (maxlabels - len(r["labels"]))
        w.writerow(base + labs)

# ---- Markdown --------------------------------------------------------------
def md(texto):
    """Convierte el marcado wiki de Jira (*Titulo:*) a negrita de Markdown."""
    out = []
    for linea in texto.split("\n"):
        out.append(re.sub(r"^\*(.+?):\*", r"**\1:**", linea))
    return "\n".join(out)


tot_pts = sum(int(r["Story Points"]) for r in rows if r["Story Points"])
done_pts = sum(int(r["Story Points"]) for r in rows if r["Story Points"] and r["Status"] == "Done")
por_tipo = Counter(r["Issue Type"] for r in rows)
por_estado = Counter(r["Status"] for r in rows)
epicas = [r for r in rows if r["Issue Type"] == "Epic"]

L = []
L.append("# MatchPoint · Backlog completo para Jira\n")
L.append("Contenido íntegro de cada elemento del tablero. Se genera junto con "
         "[`jira-import.csv`](jira-import.csv) desde [`generar-backlog.py`](generar-backlog.py), "
         "así que **nunca se desincronizan**: si editas el script, ambos se regeneran.\n")
L.append("> Si prefieres cargarlo automáticamente, sigue [`COMO-IMPORTAR.md`](COMO-IMPORTAR.md). "
         "Este documento es para leerlo, revisarlo o transcribirlo a mano.\n")
L.append("---\n")
L.append("## Resumen\n")
L.append("| | |\n|---|---:|")
L.append(f"| Elementos totales | **{len(rows)}** |")
for t in ("Epic", "Story", "Task", "Sub-task"):
    if por_tipo.get(t):
        L.append(f"| · {t} | {por_tipo[t]} |")
L.append(f"| Story points totales | **{tot_pts}** |")
L.append(f"| Story points terminados | **{done_pts}** ({round(done_pts * 100 / tot_pts)} %) |")
L.append(f"| Elementos en Done | {por_estado.get('Done', 0)} |")
L.append(f"| Elementos en To Do | {por_estado.get('To Do', 0)} |")
L.append("")
L.append("### Las épicas\n")
L.append("| # | Épica | Estado | Elementos | Puntos | Componente |")
L.append("|---|---|---|---:|---:|---|")
for i, e in enumerate(epicas, 1):
    hijos = [r for r in rows if r["Parent Id"] == e["Issue Id"]]
    nietos = [r for r in rows if r["Parent Id"] in {h["Issue Id"] for h in hijos}]
    fam = hijos + nietos
    pts = sum(int(r["Story Points"]) for r in fam if r["Story Points"])
    ic = "✅" if e["Status"] == "Done" else "⬜"
    L.append(f"| {i} | [{e['Summary']}](#épica-{i}) | {ic} {e['Status']} | {len(fam)} | {pts} | {e['Component']} |")
L.append("")
L.append("---\n")

for i, e in enumerate(epicas, 1):
    L.append(f"## Épica {i}\n")
    L.append(f"### 🗂 {e['Summary']}\n")
    L.append(f"> {e['Description']}\n")
    L.append(f"`Epic` · **{e['Status']}** · prioridad {e['Priority']} · componente `{e['Component']}` · "
             f"sprint {e['Sprint'] or '—'} · etiquetas: " +
             ", ".join(f"`{x}`" for x in e["labels"]) + "\n")

    hijos = [r for r in rows if r["Parent Id"] == e["Issue Id"]]
    for h in hijos:
        marca = "✅" if h["Status"] == "Done" else "⬜"
        L.append(f"#### {marca} {h['Summary']}\n")
        meta = [f"`{h['Issue Type']}`", f"**{h['Status']}**"]
        if h["Story Points"]:
            meta.append(f"{h['Story Points']} pts")
        meta.append(f"prioridad {h['Priority']}")
        if h["Assignee"]:
            meta.append(h["Assignee"])
        if h["Sprint"]:
            meta.append(h["Sprint"])
        L.append(" · ".join(meta) + "\n")
        if h["labels"]:
            L.append("Etiquetas: " + ", ".join(f"`{x}`" for x in h["labels"]) + "\n")
        L.append(md(h["Description"]) + "\n")

        subs = [r for r in rows if r["Parent Id"] == h["Issue Id"]]
        for s in subs:
            m2 = "✅" if s["Status"] == "Done" else "⬜"
            L.append(f"##### {m2} ↳ {s['Summary']}\n")
            meta2 = [f"`{s['Issue Type']}`", f"**{s['Status']}**"]
            if s["Story Points"]:
                meta2.append(f"{s['Story Points']} pts")
            L.append(" · ".join(meta2) + "\n")
            L.append(md(s["Description"]) + "\n")
    L.append("---\n")

with open(os.path.join(AQUI, "BACKLOG.md"), "w", encoding="utf-8") as f:
    f.write("\n".join(L))


# ---- SPRINTS.md ------------------------------------------------------------
def items_de(sprint):
    """Elementos con puntos asignados a un sprint, sin contar las épicas."""
    return [r for r in rows if r["Sprint"] == sprint and r["Issue Type"] != "Epic"]


def pts_de(items):
    return sum(int(r["Story Points"]) for r in items if r["Story Points"])


S = []
S.append("# MatchPoint · Plan de sprints\n")
S.append("Siete iteraciones de **una semana** más el backlog sin planificar. Se genera junto con "
         "[`BACKLOG.md`](BACKLOG.md) y [`jira-import.csv`](jira-import.csv) desde "
         "[`generar-backlog.py`](generar-backlog.py).\n")
S.append("> Las fechas de inicio y fin **no viajan en el CSV**: Jira no las importa. Hay que crearlas "
         "a mano en el tablero con la tabla de la sección 1. El resto —qué elemento va en qué "
         "sprint— sí lo trae la columna `Sprint`.\n")
S.append("---\n")

cerrados = [s for s in SPRINTS if s[3] == "Cerrado"]
vel = [pts_de(items_de(s[0])) for s in cerrados]
velocidad = round(sum(vel) / len(vel)) if vel else 0

S.append("## 1. Los sprints de un vistazo\n")
S.append("| Sprint | Fechas | Estado | Objetivo | Elementos | Puntos | Hechos |")
S.append("|---|---|---|---|---:|---:|---:|")
for nombre, ini, fin, estado, obj, _, _ in SPRINTS:
    it = items_de(nombre)
    hechos = pts_de([r for r in it if r["Status"] == "Done"])
    fechas = f"{ini} → {fin}" if ini else "—"
    ic = {"Cerrado": "✅", "En curso": "🟡", "Planificado": "⬜", "Sin planificar": "📥"}[estado]
    S.append(f"| **{nombre}** | {fechas} | {ic} {estado} | {obj} | {len(it)} | "
             f"{pts_de(it)} | {hechos} |")
S.append(f"| | | | **Total** | **{len([r for r in rows if r['Issue Type'] != 'Epic'])}** | "
         f"**{tot_pts}** | **{done_pts}** |")
S.append("")
S.append(f"**Velocidad media de los sprints cerrados: {velocidad} puntos por semana** "
         f"({' · '.join(str(v) for v in vel)}).\n")
S.append("---\n")

S.append("## 2. Velocidad\n")
S.append("```")
maxv = max(vel + [pts_de(items_de(s[0])) for s in SPRINTS if s[3] != "Cerrado"] + [1])
for nombre, ini, fin, estado, _, _, _ in SPRINTS:
    p = pts_de(items_de(nombre))
    barra = "█" * max(1, round(p * 40 / maxv)) if p else ""
    nota = "" if estado == "Cerrado" else f"  ({estado.lower()})"
    S.append(f"{nombre:<10} {p:>3} pts  {barra}{nota}")
S.append(f"{'media 1-5':<10} {velocidad:>3} pts  " + "·" * round(velocidad * 40 / maxv))
S.append("```\n")
S.append(f"El Sprint 6 está **por encima de la velocidad histórica** ({pts_de(items_de('Sprint 6'))} "
         f"puntos frente a {velocidad}). Es deliberado y está bajo control: "
         f"{pts_de([r for r in items_de('Sprint 6') if r['Status'] == 'Done'])} de esos puntos ya "
         "están terminados —la documentación— y lo que queda son las seis tareas de integración de "
         "la épica 10, que son mecánicas.\n")
S.append("El Backlog no se compara contra la velocidad: no tiene compromiso de fecha.\n")
S.append("---\n")

S.append("## 3. Sprint por sprint\n")
for nombre, ini, fin, estado, obj, detalle, criterio in SPRINTS:
    it = items_de(nombre)
    ic = {"Cerrado": "✅", "En curso": "🟡", "Planificado": "⬜", "Sin planificar": "📥"}[estado]
    S.append(f"### {ic} {nombre} · {obj}\n")
    if ini:
        S.append(f"**{ini} → {fin}** · {estado} · {len(it)} elementos · **{pts_de(it)} puntos** "
                 f"({pts_de([r for r in it if r['Status'] == 'Done'])} hechos)\n")
    else:
        S.append(f"{estado} · {len(it)} elementos · **{pts_de(it)} puntos**\n")
    S.append(f"{detalle}\n")
    S.append(f"> **Criterio de cierre:** {criterio}\n")
    if it:
        S.append("| | Elemento | Tipo | Épica | Pts | Responsable |")
        S.append("|---|---|---|---|---:|---|")
        for r in it:
            marca = "✅" if r["Status"] == "Done" else "⬜"
            padre = byid.get(r["Parent Id"])
            while padre is not None and padre["Issue Type"] != "Epic":
                padre = byid.get(padre["Parent Id"])
            ep = padre["Summary"] if padre else "—"
            S.append(f"| {marca} | {r['Summary']} | {r['Issue Type']} | {ep} | "
                     f"{r['Story Points'] or '—'} | {r['Assignee'] or '—'} |")
        S.append("")
    S.append("---\n")

S.append("## 4. Cómo crear los sprints en Jira\n")
S.append("1. Importa el CSV siguiendo [`COMO-IMPORTAR.md`](COMO-IMPORTAR.md) y **mapea la columna "
         "`Sprint`**. Jira crea los sprints que no existan y reparte los elementos.\n")
S.append("2. En el *backlog* del tablero, abre cada sprint → *Editar sprint* y rellena las fechas de "
         "la tabla de la sección 1. **Este paso es manual**: el importador no admite fechas de "
         "sprint.\n")
S.append("3. Pega el objetivo del sprint en el campo *Objetivo del sprint*. Son las frases de la "
         "columna «Objetivo».\n")
S.append("4. Cierra los sprints 1 a 5 (*Completar sprint*) para que quede el histórico y el informe "
         "de velocidad tenga datos. Los elementos ya vienen en `Done`, así que se cierran sin "
         "arrastrar nada.\n")
S.append("5. Inicia el **Sprint 6** como sprint activo.\n")
S.append("6. Deja el Sprint 7 y el Backlog sin iniciar.\n")
S.append("\n> Si tu proyecto es Kanban en lugar de Scrum, no hay sprints: usa la columna `Sprint` "
         "como una etiqueta más, o crea *versiones* (`Fix Version`) con los mismos nombres.\n")

with open(os.path.join(AQUI, "SPRINTS.md"), "w", encoding="utf-8") as f:
    f.write("\n".join(S))


# ---- PROMPTS-IA.md ---------------------------------------------------------
def plano(texto):
    """Quita el marcado wiki (*Titulo:*) para que el prompt sea texto llano."""
    return "\n".join(re.sub(r"^\*(.+?):\*", r"\1:", ln) for ln in texto.split("\n"))


def bloque_item(r, n, sangria=""):
    """Un elemento formateado para el prompt."""
    b = [f"{sangria}{n}. {r['Summary']}"]
    campos = [f"Tipo: {r['Issue Type']}"]
    if r["Story Points"]:
        campos.append(f"Story points: {r['Story Points']}")
    campos.append(f"Prioridad: {r['Priority']}")
    campos.append(f"Estado: {r['Status']}")
    if r["Sprint"]:
        campos.append(f"Sprint: {r['Sprint']}")
    if r["Component"]:
        campos.append(f"Componente: {r['Component']}")
    if r["Assignee"]:
        campos.append(f"Responsable: {r['Assignee']}")
    b.append(f"{sangria}   {' | '.join(campos)}")
    if r["labels"]:
        b.append(f"{sangria}   Etiquetas: {', '.join(r['labels'])}")
    b.append(f"{sangria}   Descripción:")
    for ln in plano(r["Description"]).split("\n"):
        b.append(f"{sangria}   {ln}" if ln.strip() else "")
    return "\n".join(b)


P = []
P.append("# MatchPoint · Prompts para crear el backlog con la IA de Jira\n")
P.append("Catorce prompts listos para pegar en **Atlassian Intelligence / Rovo** (el botón *IA* del "
         "proyecto, o el chat de Rovo). Se generan desde "
         "[`generar-backlog.py`](generar-backlog.py), igual que "
         "[`BACKLOG.md`](BACKLOG.md), [`SPRINTS.md`](SPRINTS.md) y "
         "[`jira-import.csv`](jira-import.csv): dicen exactamente lo mismo.\n")
P.append("---\n")
P.append("## Antes de empezar: qué esperar\n")
P.append("Conviene saberlo antes de invertir una hora en esto:\n")
P.append("| | Importar el CSV | Crear con la IA |\n|---|---|---|")
P.append("| Tiempo | ~10 min | ~45–60 min |")
P.append("| Fiabilidad de resumen y descripción | Total | Alta |")
P.append("| Story points, sprint y estado | Se importan tal cual | **La IA suele ignorarlos o inventarlos** |")
P.append("| Jerarquía épica → historia | Exacta | Suele funcionar, hay que revisarla |")
P.append("| Subtareas | Exactas | Poco fiables |")
P.append("| Fechas de sprint | No las lleva ninguno de los dos | — |")
P.append("")
P.append("**Recomendación honesta: importa el CSV** ([`COMO-IMPORTAR.md`](COMO-IMPORTAR.md)) y usa "
         "estos prompts solo si prefieres el flujo conversacional o si el importador no está "
         "disponible en tu plan de Jira. Si usas la IA, **revisa después** puntos, sprint y estado: "
         "son los campos que peor maneja.\n")
P.append("> Los modelos de Atlassian cambian seguido y no todos los planes tienen las mismas "
         "capacidades. Si un prompt crea menos elementos de los pedidos, córtalo por la mitad y "
         "mándalo en dos tandas; es el fallo más frecuente.\n")
P.append("---\n")

P.append("## Prompt 0 · Preparar el proyecto\n")
P.append("Pégalo primero. Configura el terreno antes de crear nada.\n")
comps = sorted({r["Component"] for r in rows if r["Component"]})
P.append("```text")
P.append("Voy a cargar el backlog de un proyecto llamado MatchPoint (backend de reserva de canchas "
         "de básquet y torneos de eliminación directa). Antes de crear elementos de trabajo, prepara "
         "el proyecto:")
P.append("")
P.append("1. Crea estos componentes: " + ", ".join(comps) + ".")
P.append("2. Crea estos sprints con sus fechas y objetivos:")
for nombre, ini, fin, estado, obj, _, _ in SPRINTS:
    if not ini:
        continue
    P.append(f"   - {nombre} | {ini} a {fin} | estado: {estado} | objetivo: {obj}")
P.append("3. Deja los sprints 1 a 5 marcados como completados, el Sprint 6 como sprint activo y el "
         "Sprint 7 sin iniciar.")
P.append("")
P.append("Reglas que debes seguir en todos mis mensajes siguientes:")
P.append("- Usa EXACTAMENTE los valores que te doy: resumen, descripción, tipo, story points, "
         "prioridad, estado, sprint, componente, etiquetas y responsable.")
P.append("- No inventes elementos de trabajo, ni criterios de aceptación, ni estimaciones que yo no "
         "te haya dado.")
P.append("- No resumas ni acortes las descripciones: cópialas completas.")
P.append("- Si un campo no existe en el proyecto, créalo o avísame; no lo descartes en silencio.")
P.append("- Al terminar cada mensaje, dime cuántos elementos creaste y con qué claves.")
P.append("")
P.append("Confirma que está listo y espera mi siguiente mensaje.")
P.append("```\n")
P.append("---\n")

for i, e in enumerate(epicas, 1):
    hijos = [r for r in rows if r["Parent Id"] == e["Issue Id"]]
    subs_total = [r for r in rows if r["Parent Id"] in {h["Issue Id"] for h in hijos}]
    fam = len(hijos) + len(subs_total)
    P.append(f"## Prompt {i} · {e['Summary']}\n")
    P.append(f"Crea 1 épica y {fam} elementos hijos "
             f"({pts_de(hijos + subs_total)} story points).\n")
    P.append("```text")
    P.append(f"Crea una épica y sus elementos hijos en el proyecto MatchPoint. Respeta los valores "
             f"exactos y no inventes nada.")
    P.append("")
    P.append("ÉPICA")
    P.append(f"Resumen: {e['Summary']}")
    P.append(f"Tipo: Epic | Prioridad: {e['Priority']} | Estado: {e['Status']} | "
             f"Componente: {e['Component']}" + (f" | Sprint: {e['Sprint']}" if e["Sprint"] else ""))
    P.append(f"Etiquetas: {', '.join(e['labels'])}")
    P.append("Descripción:")
    P.append(e["Description"])
    P.append("")
    P.append(f"DENTRO DE ESA ÉPICA, CREA ESTOS {len(hijos)} ELEMENTOS:")
    P.append("")
    for n, h in enumerate(hijos, 1):
        P.append(bloque_item(h, n))
        subs = [r for r in rows if r["Parent Id"] == h["Issue Id"]]
        for m, s in enumerate(subs, 1):
            P.append("")
            P.append(f"   Subtarea {n}.{m} de «{h['Summary']}»:")
            P.append(bloque_item(s, f"{n}.{m}", sangria="   "))
        P.append("")
    P.append(f"Al terminar, confírmame que creaste 1 épica y {fam} elementos hijos, "
             f"con sus claves.")
    P.append("```\n")
    P.append("---\n")

P.append("## Prompt final · Verificación\n")
P.append("Pégalo al terminar, para detectar lo que se haya perdido por el camino.\n")
P.append("```text")
P.append("Ya cargué todo el backlog de MatchPoint. Verifica y dime, sin arreglar nada todavía:")
P.append("")
P.append(f"1. ¿Cuántos elementos hay en total? Deberían ser {len(rows)}: {por_tipo.get('Epic', 0)} "
         f"épicas, {por_tipo.get('Story', 0)} historias, {por_tipo.get('Task', 0)} tareas y "
         f"{por_tipo.get('Sub-task', 0)} subtareas.")
P.append(f"2. ¿Cuántos story points suman en total? Deberían ser {tot_pts}.")
P.append(f"3. ¿Cuántos elementos están en Done? Deberían ser {por_estado.get('Done', 0)}. "
         f"¿Y en To Do? Deberían ser {por_estado.get('To Do', 0)}.")
P.append("4. Lista los elementos que NO tengan story points asignados, o que no tengan sprint.")
P.append("5. Lista las historias que hayan quedado sueltas, sin épica padre.")
P.append("6. Dime los puntos por sprint. Deberían ser:")
for nombre, *_ in SPRINTS:
    P.append(f"   - {nombre}: {pts_de(items_de(nombre))} puntos")
P.append("")
P.append("Preséntamelo como una tabla de esperado contra encontrado, marcando las diferencias.")
P.append("```\n")
P.append("---\n")
P.append("## Si algo salió mal\n")
P.append("| Síntoma | Qué hacer |\n|---|---|")
P.append("| La IA creó menos elementos de los pedidos | Corta el prompt por la mitad y mándalo en "
         "dos tandas. Es el fallo más común en las épicas grandes (la 9 tiene 13 elementos). |")
P.append("| Los story points quedaron vacíos | Pídeselo aparte: «asigna estos story points: "
         "<resumen> = 5, <resumen> = 8…». O edítalos a mano desde el backlog. |")
P.append("| Todo quedó en el primer estado | Pídele que mueva a Done los elementos de los sprints 1 "
         "a 5 y los de documentación del Sprint 6. |")
P.append("| Las descripciones salieron resumidas | Es lo que peor hace. Reenvía ese elemento solo, "
         "con «copia la descripción literal, sin resumir». |")
P.append("| Se duplicaron elementos | Pídele que liste duplicados por resumen y los borre. "
         "Revisa antes de aceptar. |")
P.append("\n**Si acumulas más de dos o tres de estos problemas, sale más a cuenta borrar el "
         "proyecto y volver a empezar con el CSV.** No es una derrota: el importador existe "
         "justamente para esto.\n")

with open(os.path.join(AQUI, "PROMPTS-IA.md"), "w", encoding="utf-8") as f:
    f.write("\n".join(P))

print(f"OK · {len(rows)} elementos · {len(epicas)} épicas · {tot_pts} puntos ({done_pts} terminados)")
print(f"     velocidad media sprints cerrados: {velocidad} pts")
for nombre, *_ in SPRINTS:
    print(f"       {nombre:<10} {pts_de(items_de(nombre)):>3} pts  ({len(items_de(nombre))} elementos)")
print("  ->", os.path.join(AQUI, "jira-import.csv"))
print("  ->", os.path.join(AQUI, "BACKLOG.md"))
print("  ->", os.path.join(AQUI, "SPRINTS.md"))
print("  ->", os.path.join(AQUI, "PROMPTS-IA.md"))
