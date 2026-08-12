# ADR-003 · Dos microservicios, una base de datos por servicio

- **Estado:** Aceptado
- **Fecha:** 2026-08-01
- **Decide:** Josué Herrera, Fernando Socasi
- **Requerimientos afectados:** RNF-04 · RF-01 … RF-07
- **Historias:** HU-01, HU-02, HU-03

## Contexto

El dominio tiene dos partes con ciclos de vida distintos:

- **Perfiles de usuario:** cambian poco, dependen del proveedor de identidad, y su modelo es una
  tabla con siete columnas.
- **Operación deportiva:** canchas, reservas, torneos, equipos y partidos. Es donde vive toda la
  complejidad —solapamiento de horarios, cuadros de eliminación, avance de ganadores— y donde se
  concentran los cambios.

Además, el equipo son dos personas que necesitan trabajar en paralelo sin pisarse.

## Decisión

**Dos microservicios independientes, cada uno con su propia base de datos PostgreSQL** y sin
ningún acceso a la base del vecino.

| Servicio | Base | Usuario | Volumen | Tablas |
|---|---|---|---|---|
| `users` | `users_db` | `users_app` | `users_data` | `users`, `audit_log` |
| `matchpoint` | `matchpoint_db` | `matchpoint_app` | `matchpoint_data` | `courts`, `reservations`, `tournaments`, `teams`, `matches`, `audit_log` |

Consecuencia dura y sin excepciones: **no hay claves foráneas entre bases ni `JOIN` entre
dominios.** Cuando `matchpoint` necesita un dato de `users`, se lo pide por HTTP.

## Alternativas consideradas

| Alternativa | Por qué se descartó |
|---|---|
| **Monolito con una sola base** | Es lo más simple y honestamente lo más adecuado para el tamaño real del problema. Se descartó porque el objetivo de la asignatura es demostrar una arquitectura distribuida, y porque el reparto del trabajo entre dos personas se vuelve un campo de minas de conflictos de merge sobre los mismos archivos. |
| **Dos microservicios compartiendo una base** | El antipatrón clásico. Elimina la ventaja principal —el aislamiento— y conserva todos los costos: dos despliegues, dos configuraciones, latencia de red. Además hace imposible evolucionar el esquema de un servicio sin coordinar con el otro. |
| **Más microservicios** (uno por agregado: canchas, reservas, torneos) | Habría multiplicado los puntos de fallo y la complejidad de la demo sin ningún beneficio: reservas y torneos comparten la entidad `courts` y se consultan juntos. Dividir por tablas en lugar de por dominios es cómo se llega a un monolito distribuido. |

## Consecuencias

**A favor**

- **Aislamiento real de fallos.** Si `users-db` se corrompe, el catálogo de canchas y los torneos
  siguen funcionando.
- **Trabajo en paralelo sin fricción.** El reparto por microservicio coincide con el reparto por
  persona; los únicos conflictos de merge fueron en `docker-compose.yml` y el `README.md`.
- **Escalado independiente.** `matchpoint` recibe todo el tráfico de consulta pública; `users` casi
  ninguno. Se escalan por separado (ver el documento de nube, criterio 4.1).
- **La frontera es explícita y se puede señalar en el código:** `clients/UsersClient.kt` es el
  único archivo por el que un servicio ve al otro.

**En contra**

- **Consistencia eventual.** `reservations.owner_name` es una **copia** del nombre que devolvió
  `users` al crear la reserva. Si el usuario cambia su nombre después, la reserva vieja conserva el
  anterior. Se aceptó a conciencia: el nombre en la reserva es un dato histórico ("quién reservó
  entonces"), no una referencia viva.
- **Latencia añadida.** Crear una reserva implica un salto HTTP extra. Mitigado con *timeout*
  explícito de 3 s y degradación a `503` (ver [ADR-004](ADR-004-comunicacion-http-con-token-propagado.md)).
- **Operación más pesada.** Seis contenedores en lugar de dos: dos aplicaciones, dos bases, el
  gateway y pgAdmin.
- **No hay integridad referencial entre dominios.** `reservations.owner_user` no es FK a
  `users.username`; la integridad de la identidad la garantiza Cognito, no PostgreSQL.

## Puesta en práctica

| Dónde | Qué |
|---|---|
| `docker-compose.yml` | Dos servicios de aplicación, dos contenedores Postgres con credenciales, base y volumen distintos |
| `DB_URL` de cada servicio | Apunta **solo** a su propia base, por DNS interno de Compose (`users-db:5432`, `matchpoint-db:5432`) |
| `matchpoint/.../clients/UsersClient.kt` | Único punto de contacto entre los dos dominios |
| `docs/MODELO-ER.md` | Los dos modelos, con la frontera dibujada explícitamente |

Verificación: en pgAdmin, el usuario `matchpoint_app` **no puede** conectarse a `users_db`. La
separación no es una convención del equipo, es una restricción del motor.
