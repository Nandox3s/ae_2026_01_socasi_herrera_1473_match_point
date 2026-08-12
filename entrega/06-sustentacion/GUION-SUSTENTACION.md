# MatchPoint · Guion de sustentación

**Criterio 6 de la rúbrica (/10).**

| Sub-criterio | Puntos | Dónde se gana |
|---|:--:|---|
| 6.1 Vende la idea articulando el valor para los usuarios | /2 | Bloques 1 y 7 |
| 6.2 Lenguaje técnico apropiado para las asignaturas | /2 | Todo el guion · glosario §4 |
| 6.3 Describe con soltura estructura y funcionamiento | /2 | Bloques 2 a 5 |
| 6.4 Claridad y seguridad | /2 | Preparación §5 |
| 6.5 Responde adecuadamente las preguntas | /2 | Banco de preguntas §3 |

---

## 1. Antes de entrar

### Checklist de 15 minutos antes

```bash
cd ~/josue/match
docker compose down -v          # arranque limpio, sin datos de pruebas anteriores
docker compose up -d --build
docker compose ps               # esperar a que los seis digan (healthy)
```

- [ ] Los seis servicios en `healthy`.
- [ ] `curl -s http://localhost:9090/health` responde `ok`.
- [ ] Postman abierto, environment **MatchPoint - local (nginx 9090)** seleccionado, con
      `cognitoClientSecret`, `managerPassword` y `playerPassword` completos.
- [ ] `manager_josue` y `player_fernando` existen y están **CONFIRMED** en el User Pool.
      Si alguno está en `FORCE_CHANGE_PASSWORD`, el token no sale: cambiar la clave **antes**.
- [ ] Prueba de 10 segundos del login, sin Postman de por medio:
      ```bash
      ./scripts/cognito-token.sh manager_josue
      ```
- [ ] Cuatro ventanas listas y en el orden en que se van a usar:
      **1)** presentación · **2)** Postman · **3)** terminal con los logs · **4)** pgAdmin.
- [ ] Terminal de logs corriendo y visible toda la sustentación:
      ```bash
      docker compose logs -f --tail=0 users matchpoint
      ```
- [ ] Batería y cargador. Notificaciones silenciadas. Modo presentación activado.

### Reparto y tiempos

| Bloque | Minutos | Quién | Asignatura que se evalúa |
|---|:--:|---|---|
| 1 · El problema y la propuesta de valor | 3 | Ambos | Emprendimiento |
| 2 · Análisis y proceso | 3 | *(quien llevó `users`)* | Análisis y Diseño |
| 3 · Arquitectura y demo en vivo | 6 | *(quien llevó `matchpoint`)* | Arquitectura Empresarial |
| 4 · Contenedores y nube | 3 | Ambos | Computación en la Nube |
| 5 · App móvil | 3 | *(quien la desarrolla)* | Desarrollo Móvil |
| 6 · Modelo de negocio y números | 3 | Ambos | Emprendimiento |
| 7 · Cierre | 1 | Ambos | — |
| **Preguntas** | 8–10 | Ambos | Todas |

> El bloque 5 lo presenta quien está desarrollando la app; este guion no cubre su contenido.

---

## 2. El guion

### Bloque 1 · El problema y la propuesta de valor · 3 min

> **Abrir así, no con "buenos días, nuestro proyecto es…".**

*"Un sábado a las ocho de la noche, en cualquier cancha de barrio de Quito, pasan dos cosas: dos
grupos llegan con la misma hora reservada por WhatsApp y discuten en la puerta; y a tres cuadras hay
una cancha vacía que nadie sabe que está libre.*

*Para el dueño eso son unos **1 500 dólares al mes** que no factura. Para el jugador es llamar a
cinco números para encontrar dónde jugar.*

*MatchPoint resuelve las dos cosas con un solo producto: reservas y torneos."*

**Las tres promesas, una por segmento:**

| Para… | La promesa |
|---|---|
| El **dueño** de la cancha | *"Tu cancha se llena sola y no vuelves a tener dos reservas en la misma hora"* |
| El **jugador** | *"Ves qué cancha está libre hoy y la reservas en 30 segundos"* |
| El **organizador** | *"Tu torneo se arma solo: cuadro, avance y campeón, sin planillas"* |

**Cerrar el bloque con el diferencial, que es la frase que hay que dejar clavada:**

> *"Aplicaciones de reserva de canchas hay varias. Plataformas de torneos también. **Nadie une las
> dos**, y son la misma cancha: la que se alquila entre semana es la sede del torneo del fin de
> semana."*

---

### Bloque 2 · Análisis y proceso · 3 min

**Qué se muestra:** [`REQUERIMIENTOS.md`](../01-analisis/REQUERIMIENTOS.md), el `git log`, y la
tabla de ADR.

> *"Levantamos **33 requerimientos funcionales y 16 no funcionales**. Los 33 están implementados y
> cubiertos por pruebas — no es una lista de deseos, cada fila apunta al endpoint y a la clase que
> lo evidencian.*
>
> *Los priorizamos con tres factores: valor, riesgo técnico y dependencia. De ahí salió una regla
> que gobierna todo el proyecto: **primero lo que habilita, después lo que luce**. La autenticación
> no le sirve a nadie por sí sola, pero ninguna otra historia se puede probar sin ella. Fue la
> primera."*

```bash
git log --oneline --reverse | head -6
```

> *"Y esa priorización se ve en el historial: el commit de seguridad está **antes** que todos los
> commits de negocio. El orden del historial es la evidencia de la decisión.*
>
> *Cada historia entró por su rama `feature/HU-nn`. **Las once siguen publicadas**, sin borrar, para
> que se pueda abrir cualquiera y ver exactamente qué entró con ella."*

```bash
git branch -a
```

> *"Documentamos **once decisiones de arquitectura en ADR**. Cada una dice qué descartamos y qué
> costo aceptamos a cambio. Un ADR sin costos es propaganda."*

**Cerrar con pruebas:**

> *"214 pruebas entre los dos microservicios, **100 % de cobertura de líneas** sobre el código
> propio. Y no es un número declarado: el build falla si baja."*

---

### Bloque 3 · Arquitectura y demo en vivo · 6 min

**El bloque que más peso tiene. Se hace en Postman, con los logs visibles al lado.**

#### 3.1 La arquitectura en tres frases · 1 min

*(Mostrar el diagrama del README)*

> *"Tres cosas y seguimos:*
> 1. *Dos microservicios, **cada uno con su propia base**. No comparten tablas ni hacen `JOIN`: lo
>    ajeno se pide por HTTP.*
> 2. *Un solo punto de entrada: nginx en el 9090. Los microservicios **no publican puerto**.*
> 3. *La identidad no es nuestra: la pone AWS Cognito. Cada servicio valida el JWT contra el JWKS
>    del User Pool."*

```bash
docker compose ps
```

> *"Un único servicio con puerto mapeado al host."*

#### 3.2 El flujo completo · 3 min

**Postman, carpeta por carpeta, con los logs al lado:**

| Paso | Qué se hace | Qué se señala en el log |
|---|---|---|
| 1 | Carpeta `0` — token de Cognito para los dos roles | Dos llamadas: `InitiateAuth` + `RespondToAuthChallenge` |
| 2 | `POST /matchpoint/courts` con MANAGER | `event=court.created` con `managerUser` |
| 3 | `GET /matchpoint/courts/available` **sin token** | Público: el catálogo no pide cuenta |
| 4 | `POST /matchpoint/reservations` con PLAYER | **Tres líneas seguidas:** `users.profile.requested` → `reservation.created` → `http.response 201` |

> *"Miren la línea del medio: `matchpoint` **llamó al microservicio `users`** antes de guardar,
> propagando el token del propio usuario. No inventamos un token de servicio: si el usuario no
> puede ver algo, nosotros tampoco podemos verlo por él."*

| 5 | Repetir la **misma franja** | `409` · `event=state.conflict` |

> *"Esa es la regla que le vende el producto al dueño: **no hay dos reservas en la misma hora**."*

#### 3.3 Las dos capas de seguridad · 1,5 min

**Cuatro peticiones. La cuarta es la que importa.**

| # | Petición | Resultado | Qué prueba |
|---|---|:--:|---|
| 1 | `GET /reservations/me` **sin token** | `401` | Sin identidad no se pasa |
| 2 | Mismo endpoint, **token alterado en un carácter** | `401` | La firma se valida de verdad contra el JWKS |
| 3 | `POST /courts` con token de **PLAYER** | `403` | Restricción por **rol** → `event=authz.denied` |
| 4 | `PATCH /courts/4` con `manager_josue` *(la cancha es de `manager_ana`)* | `403` | Restricción por **propiedad** → `event=ownership.denied` |

> *"La cuarta es la que cuenta: **el rol es el correcto y aun así no puede**. Son dos capas
> distintas, y el `event` distinto en el log lo prueba.*
>
> *Y hay algo que no se ve en la petición: **los DTO de request no tienen campo de propietario**. No
> es que lo ignoremos si llega — no hay forma de enviarlo. El dueño sale del claim del token."*

#### 3.4 El torneo completo · 0,5 min

> *"Cuatro equipos, arrancamos el cuadro y registramos un marcador."*

*(Ejecutar la carpeta `4. torneo completo` del Collection Runner)*

> *"Al puntuar el partido, en una sola operación: se actualizan las estadísticas de los dos equipos,
> el perdedor queda eliminado y **el ganador se mueve solo al slot de la siguiente ronda**. Al
> puntuar la final, el torneo se cierra con campeón. El organizador no toca ninguna planilla."*

---

### Bloque 4 · Contenedores y nube · 3 min

> *"El sistema completo son **seis contenedores en tres capas**: gateway, aplicación y datos.
> Levanta en cualquier máquina con Docker, **sin JDK, sin Gradle y sin PostgreSQL instalados**."*

**Lo que hay que nombrar, porque es lo que se evalúa:**

- Build **multi-stage**: se compila con JDK y se ejecuta sobre JRE. La imagen final no lleva ni el
  código fuente ni Gradle.
- **Versiones fijas**, nunca `latest`: `postgres:16-alpine`, `nginx:1.27-alpine`, `pgadmin4:8.14`.
- `healthcheck` reales en los seis. Las apps dependen de su base con `service_healthy`; entre
  microservicios, con `service_started` — *"una dependencia de datos no debe convertirse en una
  dependencia de arranque"*.
- Volúmenes con nombre: el contenedor es desechable, el dato no.
- Variables obligatorias: si falta un secreto, **el stack no arranca**, en lugar de arrancar con un
  valor por defecto inseguro.

**El escalamiento, que es el sub-criterio con más peso del bloque:**

> *"Escalamos distinto por capa, y hay una razón concreta: **el sábado en la mañana es unas 60 veces
> la carga de un martes**.*
>
> *La capa de aplicación escala **horizontal**: los microservicios ya son* stateless *—el JWT es
> autocontenido, no guardamos sesión— así que añadir instancias es cambiar un número en el Auto
> Scaling Group. Dimensionar verticalmente para el pico significaría pagar el pico las 24 horas de
> los 7 días.*
>
> *La base de datos escala **vertical**, más una réplica de lectura. Repartir escrituras entre nodos
> exige* sharding*: complejidad enorme para unas decenas de reservas por hora."*

**La honestidad del alcance, dicha por nosotros y no por el tribunal:**

> *"La demo corre en local. La arquitectura AWS está **diseñada y costeada, no desplegada**: VPC en
> tres niveles, ALB, Auto Scaling Group, RDS Multi-AZ, ECR y Cognito. **127 dólares al mes.**
> Lo decidimos así porque una demo que depende de la red del aula es una demo que puede no ocurrir,
> y está documentado en el ADR-011 con sus consecuencias."*

---

### Bloque 5 · App móvil · 3 min

*(Lo presenta quien la desarrolla. Punto de enlace con este guion:)*

> *"Todo lo que acaban de ver por Postman es exactamente lo que consume la app: **la misma API, por
> el mismo gateway, con el mismo token de Cognito**."*

---

### Bloque 6 · Modelo de negocio y números · 3 min

> *"Quien paga no es el jugador: es el **dueño de la cancha**. Es el que tiene un dolor medible en
> dinero y el que trae el inventario. Sin canchas, la app no le sirve a ningún jugador."*

**Modelo de ingresos:**

| Fuente | Modelo | Fase |
|---|---|---|
| **Suscripción SaaS** | 19 / 39 / 79 USD al mes según canchas | 1 — **72 % del ingreso** |
| Comisión por reserva pagada en línea | 5 % | 2 |
| Torneos | 1 USD por equipo inscrito | 1 |

> *"Elegimos suscripción y no solo comisión por una razón práctica: **el 90 % del pago hoy es en
> efectivo en la cancha**. La comisión depende de un cambio de hábito que no controlamos; la
> suscripción da ingreso recurrente desde el primer mes."*

**Los cuatro números que hay que decir:**

| | |
|---|---|
| Capital para arrancar | **menos de 1 600 USD** — stack de código abierto, nube de pago por uso |
| Punto de equilibrio | **9 canchas** = el 2 % del mercado de Quito |
| La caja se recupera | **mes 8** |
| Costo real del año 1 | **el tiempo de los socios**: ~16 600 USD valorizados |

> *"El precio se defiende solo: 19 dólares al mes son **1,6 horas de alquiler**. Si le recuperamos
> dos horas muertas, ya se pagó."*

**Y la frase que cierra el bloque:**

> *"En el escenario pesimista —la mitad de captación y el doble de abandono— el año 1 cierra en
> −320 dólares. Es un mal año, no el final del proyecto. Con esta estructura de costos el riesgo de
> quiebra es mínimo."*

---

### Bloque 7 · Cierre · 1 min

> *"Resumiendo lo que trajimos:*
>
> - *Un backend de **dos microservicios** con seguridad real de AWS Cognito, **214 pruebas** y 100 %
>   de cobertura.*
> - *Un sistema **completamente contenedorizado**, que levanta en cualquier máquina con un comando.*
> - ***Once decisiones de arquitectura documentadas**, cada una con lo que descartamos y el costo
>   que aceptamos.*
> - *Un modelo de negocio con punto de equilibrio en **9 canchas** y capital de arranque por debajo
>   de los 1 600 dólares.*
>
> *Y lo que falta, lo sabemos y está fechado: pagos en línea, precio sugerido por franja y el
> despliegue en AWS.*
>
> *Gracias. Quedamos atentos a sus preguntas."*

---

## 3. Banco de preguntas

**Regla para todas:** responder en **dos frases**, y si hay evidencia, abrirla. Si no se sabe,
decirlo: *"No lo medimos. Lo que sí puedo decir es…"*. Inventar un dato es lo único que hunde una
sustentación por completo.

### Análisis y Diseño de Sistemas

| Pregunta | Respuesta |
|---|---|
| **¿Cómo priorizaron los requerimientos?** | Con tres factores: valor ×3, riesgo técnico ×2, dependencia ×1. De ahí salió la regla "primero lo que habilita": la autenticación fue lo primero porque ninguna otra historia se podía probar sin ella. Está en el índice de los ADR con la tabla de puntajes. |
| **¿Por qué HU-01 va antes que HU-05 si tiene menor puntaje?** | Porque HU-07 depende de HU-01 —no se reserva sin perfil— y adelantar HU-05 habría dejado la dependencia abierta. **La dependencia manda sobre el puntaje cuando hay conflicto**; es la única excepción y está documentada como tal. |
| **¿Usaron GitFlow completo?** | Usamos `main`, `develop` y **nueve ramas `feature/*`**, una por historia. **No** usamos `release/*` ni `hotfix/*`: con dos personas y una entrega no aportaban nada. Lo omitimos a conciencia y está escrito en la autocrítica del documento de GitFlow. |
| **Su historial es lineal, ¿de verdad usaron ramas?** | Sí. Es lineal porque hacíamos `rebase` sobre `develop` antes de integrar, así que el merge quedaba *fast-forward*. **Las nueve ramas siguen publicadas en `origin`** y las nueve son ancestros de `develop`: se lo puedo comprobar con `git merge-base --is-ancestor`. Son ellas las que prueban la separación, no la forma del grafo. |
| **¿Por qué HU-01 a HU-04 aparecen dos veces en el historial?** | Porque empezamos en repositorios separados: Fernando `users` con la seguridad, yo `matchpoint`. Al unificarlos, el commit `d32f9bb` reorganizó el árbol y esas cuatro historias se re-aplicaron sobre la estructura nueva. **Es el mismo trabajo reubicado, no trabajo repetido.** Y el orden de prioridad se respetó las dos veces: HU-04 va primera en las dos series. |
| **¿Por qué hay commits de "Your Name"?** | Porque en una de las máquinas la identidad de git no estaba configurada. Son cinco commits y **están identificados uno por uno en la autocrítica del documento de GitFlow**, con su autor real. No los corregimos porque reescribir historia ya publicada rompería los hashes que cita toda la documentación; preferimos declararlo. La atribución correcta sí está en los commits originales de esas mismas historias. |
| **¿Qué es un ADR y para qué les sirvió?** | Un documento corto e inmutable con una decisión: contexto, decisión, alternativas descartadas y consecuencias. Nos sirvió para no volver a discutir lo mismo y para poder responder *por qué* sin depender de la memoria. Tenemos once. |

### Arquitectura Empresarial

| Pregunta | Respuesta |
|---|---|
| **¿Por qué microservicios y no un monolito?** | Con honestidad: para el tamaño real del problema, un monolito habría bastado. Elegimos microservicios porque es el objetivo de la asignatura y porque el reparto entre dos personas coincide con la frontera de los servicios. Los costos —consistencia eventual, latencia, seis contenedores— están escritos en el ADR-003. |
| **¿Cómo se comunican los microservicios?** | HTTP síncrono, por el nombre de servicio de Compose, **propagando la cabecera `Authorization` del usuario**. Nunca fabricamos un token de servicio: eso convertiría a `matchpoint` en un cliente privilegiado capaz de leer cualquier perfil. |
| **¿Qué pasa si `users` se cae?** | `matchpoint` devuelve `503` en las reservas, sigue sirviendo catálogo y torneos, y se recupera solo. Se lo puedo mostrar: `docker compose stop users`. |
| **¿Por qué el controlador no tiene lógica?** | Porque la lógica de negocio pertenece al *service*. El controlador mapea la ruta, lee el claim del token y delega. Se lo puedo demostrar con un `grep`: ningún controlador inyecta un repositorio y ningún *service* conoce `HttpServletRequest`. |
| **¿Cómo manejan los errores?** | Excepciones propias agrupadas **por el código HTTP que significan**, y un único `@RestControllerAdvice` que las traduce. No hay ni un `try/catch` de traducción en controladores ni *services*. Y todo rechazo se registra automáticamente con su `event`. |
| **¿Cómo evitan que alguien edite un recurso ajeno?** | Dos capas de `403`: la de rol la decide Spring Security; la de propiedad la decide el *service*. Y el dueño sale del token: **los DTO de request ni siquiera tienen ese campo**. |
| **¿100 % de cobertura no es artificial?** | Es 100 % sobre el código con lógica de decisión; excluimos `main()`, DTOs, entidades y configuración, y las exclusiones están declaradas en el `build.gradle.kts`. Reportamos los dos números: 100 % en JaCoCo y 99.8 % en el IDE, que cuenta todo. |
| **¿Sus pruebas necesitan AWS?** | No. El `JwtDecoder` se sustituye por un mock y los JWT se fabrican con `spring-security-test`. Las 214 pruebas corren sin red, sin AWS y sin PostgreSQL. |
| **¿Dónde está la relación N:M?** | Entre equipos, resuelta con `matches` como entidad asociativa: `home_team_id` y `away_team_id`. No es una tabla puente vacía — lleva ronda, posición, marcador, estado y ganador. |

### Computación en la Nube

| Pregunta | Respuesta |
|---|---|
| **¿Está desplegado en la nube?** | No. Corre en Docker Compose local y la arquitectura AWS está diseñada y costeada. Fue una decisión con su ADR: una demo que depende de la red del aula es una demo que puede no ocurrir. El camino está en cinco fases; la fase 1 es un día de trabajo. |
| **¿Diferencia entre contenedor y máquina virtual?** | La VM virtualiza hardware y lleva un sistema operativo invitado completo: gigabytes y arranque de minutos. El contenedor comparte el kernel del anfitrión y aísla procesos: megabytes y arranque en segundos. **En nuestra arquitectura conviven**: contenedores Docker dentro de instancias EC2, que son VM. |
| **¿Vertical u horizontal?** | Las dos, por capa. La aplicación **horizontal**, porque ya es *stateless* y el pico del sábado es 60 veces la carga base. La base de datos **vertical** más réplica de lectura, porque repartir escrituras exige *sharding*. |
| **¿Por qué no Kubernetes?** | Sobredimensionado para dos microservicios: el costo del plano de control por sí solo supera al de toda nuestra arquitectura. Tendría sentido pasadas las decenas de servicios. |
| **¿Cuánto costaría en producción?** | 127 dólares al mes en la arquitectura completa con alta disponibilidad; 40 en una configuración mínima. El desglose está en el documento de nube. El NAT Gateway es el 26 % del total, que fue la sorpresa. |
| **¿Qué pasa si se cae una zona de disponibilidad?** | El ALB enruta a la otra AZ y RDS conmuta al standby. Uno o dos minutos. El ASG tiene mínimo dos instancias justamente por eso: con una sola no hay alta disponibilidad, por grande que sea. |

### Emprendimiento

| Pregunta | Respuesta |
|---|---|
| **¿Quién paga?** | El dueño de la cancha, por suscripción mensual. El jugador no paga suscripción. Entramos por el lado del dueño porque tiene el dolor medible en dinero y trae el inventario. |
| **¿Y si nadie paga por esto?** | El precio son 1,6 horas de alquiler al mes. Si le recuperamos dos horas muertas, ya se pagó. Ese es el argumento de venta, y por eso el reporte mensual de ocupación es parte del producto: le muestra en números lo que ganó. |
| **¿Cuál es su ventaja frente a un competidor con más capital?** | El software se clona en tres meses; **conseguir 100 canchas afiliadas toma un año**. La red de canchas y el histórico de ocupación son la barrera, no el código. |
| **¿Por qué suscripción y no comisión?** | Porque el 90 % del pago hoy es en efectivo. La comisión depende de un cambio de hábito que no controlamos; la suscripción da ingreso recurrente desde el primer mes. La comisión entra en fase 2 como complemento. |
| **¿Cuánto necesitan para arrancar?** | Menos de 1 600 dólares: 995 de constitución y materiales, más 570 de capital de trabajo para los primeros meses. |
| **¿Cuándo son rentables?** | El equilibrio operativo son 9 canchas, en el mes 3. La caja se recupera en el mes 8. Pero el costo real del año 1 es nuestro tiempo: unos 16 600 dólares valorizados, y eso está en el plan. |
| **¿Qué pasa si crecen la mitad de lo previsto?** | Cerramos el año 1 en −320 dólares con 18 canchas: por encima del equilibrio operativo. Un mal año, no el final. Está en el análisis de sensibilidad. |
| **¿Cómo consiguen las primeras canchas?** | Venta directa, en la cancha, con la app abierta. Las primeras diez entran gratis seis meses a cambio de retroalimentación semanal. |

### Preguntas incómodas — hay que tenerlas ensayadas

| Pregunta | Respuesta |
|---|---|
| **¿Qué es lo peor de su proyecto?** | Que no está desplegado en la nube. Es la limitación más visible y fue una decisión con su ADR, no un olvido. La segunda: no tenemos integración continua, así que la regla de "nada se mezcla en rojo" depende de nuestra disciplina. |
| **¿Qué harían distinto si empezaran hoy?** | Pull requests con revisión cruzada desde el inicio, y migraciones versionadas con Flyway en lugar de `ddl-auto=update`. Las dos están en la deuda técnica reconocida, con fecha. |
| **¿Un torneo de 6 equipos?** | Hoy no se puede: exigimos potencia de dos para evitar los *byes*. Fue una simplificación deliberada con su ADR. La ruta de evolución está clara —sembrar los *byes* como partidos pre-resueltos en la primera ronda— y **no toca la lógica de avance**, que es la ventaja de haber generado el cuadro completo. |
| **¿Este proyecto tiene futuro real o es solo académico?** | El backend está terminado y probado, y el punto de equilibrio son nueve clientes. La pregunta abierta no es técnica: es si conseguimos las primeras diez canchas. Esa es la parte que todavía no hemos validado. |
| **¿Qué aprendieron?** | Que decidir temprano lo que da miedo —la seguridad, en nuestro caso— es lo que hace que el resto avance rápido. Y que escribir por qué descartamos una alternativa vale más que documentar la que elegimos. |

---

## 4. Glosario: el vocabulario de cada asignatura

**No se trata de usar palabras difíciles: se trata de usar la palabra correcta.**

| Asignatura | Términos que hay que usar con naturalidad |
|---|---|
| **Análisis y Diseño** | requerimiento funcional / no funcional · caso de uso · flujo principal y alternativo · precondición y postcondición · trazabilidad · GitFlow · *feature branch* · ADR · priorización por valor y riesgo · criterio de aceptación |
| **Arquitectura Empresarial** | microservicio · *database per service* · arquitectura en capas · DTO · *mapper* · entidad asociativa · cardinalidad · *Resource Server* · JWKS · *claim* · autorización por rol y por propiedad · manejador global de excepciones · cobertura de líneas · *mock* y *stub* |
| **Computación en la Nube** | IaaS · PaaS · SaaS · virtualización · contenedor · imagen · *build multi-stage* · orquestación · escalamiento vertical y horizontal · elasticidad · *stateless* · alta disponibilidad · zona de disponibilidad · balanceador · Auto Scaling Group · Multi-AZ · RPO y RTO |
| **Emprendimiento** | Business Model Canvas · propuesta de valor · segmento de cliente · fuente de ingreso · MRR · CAC · LTV · *churn* · punto de equilibrio · capital de trabajo · análisis de sensibilidad · escalabilidad del modelo |

**Trampa frecuente:** decir "microservicios" y describir un monolito partido en dos, o decir
"escalable" sin poder explicar **qué** escala y **cómo**. Cada término que se usa hay que poder
aterrizarlo en el proyecto en una frase.

---

## 5. Cómo se gana el punto de claridad y seguridad (6.4)

### Lo que hay que hacer

1. **Ensayar en voz alta dos veces, con cronómetro.** Leer el guion en silencio no sirve: la boca
   tarda más que la cabeza.
2. **Ensayar una vez con las cosas rotas a propósito.** Apagar el wifi, cerrar Postman a mitad de la
   demo. Lo que da seguridad no es que todo salga bien: es haber visto qué se hace cuando no sale.
3. **Tener un plan B para la demo:** capturas de pantalla de cada paso en la presentación. Si el
   backend no levanta, se muestran las capturas **sin pedir disculpas y sin perder el ritmo**.
4. **Empezar por el problema, no por la tecnología.** Nadie se engancha con "usamos Kotlin y Spring
   Boot 4". Todos se enganchan con dos grupos discutiendo en la puerta de una cancha.
5. **Mirar al tribunal, no a la pantalla.** La pantalla es para ellos.
6. **Decir los números exactos.** "Doscientas catorce pruebas" transmite más control que "muchas
   pruebas". Los números concretos son la señal de que se conoce el proyecto.
7. **Reconocer los límites antes de que los pregunten.** Decir *"no está desplegado en la nube y fue
   una decisión, aquí está el ADR"* es una posición de fuerza; que lo descubran ellos, no.

### Lo que hay que evitar

- Leer las diapositivas.
- "Este es un proyecto muy completo que abarca…" — decir qué hace, no adjetivos.
- Justificarse: *"no nos dio tiempo de…"*. Se dice **qué falta y cuándo se haría**.
- Improvisar un dato. Si no se midió, se dice que no se midió.
- Pelear con una pregunta. Si el tribunal señala un problema real: *"Tiene razón, y así lo
   resolveríamos."*
- Pasar de los 20 minutos. Cortar un bloque es preferible a que corten la presentación.

### Los cinco segundos antes de empezar

Respirar. Mirar al tribunal. Y arrancar con la cancha del sábado a las ocho de la noche.
