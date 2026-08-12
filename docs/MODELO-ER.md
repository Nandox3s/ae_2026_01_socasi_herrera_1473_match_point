# MatchPoint · Modelo entidad-relación

Dos bases PostgreSQL 16 **independientes**, una por microservicio (*database-per-service*).
No hay ninguna clave foránea entre ellas: el dato ajeno viaja por HTTP, nunca por `JOIN`.

| Base | Microservicio | Tablas |
|---|---|---|
| `users_db` | `users` | `users`, `audit_log` |
| `matchpoint_db` | `matchpoint` | `courts`, `reservations`, `tournaments`, `teams`, `matches`, `audit_log` |

Los tres diagramas de este documento están exportados en [`er/`](er) como `.svg` y `.png`
(listos para pegar en el informe), junto a su fuente `.mmd`.

El DDL en ejecución lo genera Hibernate (`ddl-auto=update`); el modelo escrito a mano está en
[`users/src/main/resources/db/users_schema.sql`](../users/src/main/resources/db/users_schema.sql)
y [`matchpoint/src/main/resources/db/matchpoint_schema.sql`](../matchpoint/src/main/resources/db/matchpoint_schema.sql).

---

## 1. Base `users_db` (microservicio `users`)

```mermaid
erDiagram
    users {
        bigint      id          PK "identity"
        varchar_80  cognito_id  UK "claim sub del JWT"
        varchar_60  username       "claim username del JWT"
        varchar_80  name
        varchar_120 email          "opcional"
        varchar_30  phone          "opcional"
        timestamp   created_at
    }

    audit_log {
        bigint       id          PK "identity"
        varchar_40   entity_name    "siempre 'users'"
        bigint       entity_id      "id de la fila auditada"
        varchar_10   action         "INSERT | UPDATE | DELETE"
        varchar_80   user_sub       "quien: sub del JWT"
        varchar_60   user_name      "quien: username del JWT"
        varchar_2000 old_values     "valores anteriores, enmascarados"
        varchar_2000 new_values     "valores nuevos, enmascarados"
        timestamp    created_at     "cuando"
    }

    users ||..o{ audit_log : "deja rastro en"
```

> `audit_log` **no** tiene FK a `users`: apunta a cualquier entidad con el par
> (`entity_name`, `entity_id`) y las filas de auditoría deben sobrevivir al borrado del perfil.
> Por eso la relación se dibuja punteada (lógica, no declarada en el motor).

Regla del dominio: **un usuario de Cognito = un perfil**, garantizado por el `UNIQUE` de
`cognito_id`. El perfil no guarda contraseñas ni roles: la identidad y los grupos
(`MANAGER` / `PLAYER`) viven en el User Pool y llegan dentro del token.

---

## 2. Base `matchpoint_db` (microservicio `matchpoint`)

```mermaid
erDiagram
    courts {
        bigint       id             PK "identity"
        varchar_80   name
        varchar_40   sector
        boolean      has_parking
        varchar_30   sport_type        "BASKET"
        varchar_40   floor_type
        decimal_8_2  price_per_hour    "> 0"
        boolean      active
        varchar_60   manager_user      "dueño: claim username del JWT"
        timestamp    created_at
    }

    reservations {
        bigint      id               PK "identity"
        bigint      court_id         FK "-> courts.id"
        varchar_60  owner_user          "dueño: claim username del JWT"
        varchar_80  owner_name          "COPIA de lo que devolvio el micro users"
        timestamp   starts_at
        integer     duration_minutes    "> 0"
        varchar_20  status              "CONFIRMED | CANCELLED"
        timestamp   created_at
    }

    tournaments {
        bigint      id               PK "identity"
        varchar_80  name
        varchar_30  sport_type          "BASKET"
        integer     max_teams           "2 | 4 | 8 | 16 | 32"
        varchar_150 prize               "opcional"
        varchar_20  status              "REGISTRATION | IN_PROGRESS | FINISHED"
        varchar_60  manager_user        "dueño: claim username del JWT"
        bigint      court_id         FK "-> courts.id (sede, opcional)"
        bigint      champion_team_id FK "-> teams.id (opcional)"
        timestamp   created_at
    }

    teams {
        bigint       id                 PK "identity"
        bigint       tournament_id      FK "-> tournaments.id"
        varchar_60   registered_by_user    "PLAYER que inscribe"
        varchar_80   name                  "UNIQUE por torneo"
        varchar_80   contact_name
        varchar_120  contact_email
        varchar_30   contact_phone
        boolean      eliminated
        integer      matches_played
        integer      matches_won
        integer      matches_lost
        integer      points_for
        integer      points_against
        integer      current_round
        timestamp    created_at
    }

    matches {
        bigint      id                PK "identity"
        bigint      tournament_id     FK "-> tournaments.id"
        integer     round_number         "UNIQUE con position_in_round"
        integer     position_in_round
        bigint      home_team_id      FK "-> teams.id"
        bigint      away_team_id      FK "-> teams.id"
        integer     home_score
        integer     away_score
        bigint      winner_team_id    FK "-> teams.id (home o away)"
        varchar_20  status               "PENDING | READY | PLAYED"
        timestamp   scheduled_at         "opcional"
        timestamp   created_at
    }

    audit_log {
        bigint       id          PK "identity"
        varchar_40   entity_name    "courts | reservations | tournaments | teams | matches"
        bigint       entity_id
        varchar_10   action         "INSERT | UPDATE | DELETE"
        varchar_80   user_sub       "quien"
        varchar_60   user_name      "quien"
        varchar_2000 old_values
        varchar_2000 new_values
        timestamp    created_at     "cuando"
    }

    courts      ||--o{ reservations : "recibe"
    courts      |o--o{ tournaments  : "es sede de"
    tournaments ||--o{ teams        : "inscribe"
    tournaments ||--o{ matches      : "programa"
    teams       |o--o{ matches      : "juega de local"
    teams       |o--o{ matches      : "juega de visitante"
    teams       |o--o{ matches      : "gana"
    teams       |o--o| tournaments  : "es campeon de"
```

### Cardinalidades, en palabras

| Relación | Lectura | Regla en el motor |
|---|---|---|
| `courts` 1 : N `reservations` | Una cancha recibe muchas reservas; una reserva es de **una** cancha. | FK obligatoria, `ON DELETE CASCADE` |
| `courts` 0..1 : N `tournaments` | Un torneo puede tener sede o no; una cancha puede albergar varios torneos. | FK **nullable**, `ON DELETE SET NULL` |
| `tournaments` 1 : N `teams` | Un torneo inscribe muchos equipos; un equipo pertenece a **un** torneo. | FK obligatoria, `ON DELETE CASCADE` + `UNIQUE (tournament_id, name)` |
| `tournaments` 1 : N `matches` | El cuadro de eliminación directa cuelga del torneo. | FK obligatoria, `ON DELETE CASCADE` + `UNIQUE (tournament_id, round_number, position_in_round)` |
| `teams` N : M `teams` | Dos equipos se enfrentan: **`matches` es la entidad asociativa**, con `home_team_id` y `away_team_id`. | Las dos FK son nullable (`ON DELETE SET NULL`): el slot del cuadro existe antes de saber quién lo ocupa |
| `matches` N : 0..1 `teams` | El ganador es uno de los dos equipos del partido, o nadie todavía. | `CHECK (winner_team_id IS NULL OR = home_team_id OR = away_team_id)` |
| `tournaments` 0..1 : 0..1 `teams` | Un torneo terminado tiene un campeón. | FK nullable añadida después de `teams` (dependencia circular) |
| cualquier tabla → `audit_log` | Toda escritura deja quién, qué, cuándo y valores antes/después. | Sin FK: `(entity_name, entity_id)` |

---

## 3. La frontera entre los dos microservicios

```mermaid
flowchart LR
    subgraph COG["AWS Cognito · User Pool"]
        JWT["JWT<br/>sub · username · cognito:groups"]
    end

    subgraph U["microservicio users"]
        UDB[("users_db<br/>users · audit_log")]
    end

    subgraph M["microservicio matchpoint"]
        MDB[("matchpoint_db<br/>courts · reservations<br/>tournaments · teams · matches · audit_log")]
    end

    JWT -->|"claim sub llena users.cognito_id"| UDB
    JWT -->|"claim username llena manager_user, owner_user, registered_by_user"| MDB
    M -->|"GET /users/me con el mismo token<br/>HTTP, nunca SQL"| U
    U -.->|"el nombre del perfil se copia en reservations.owner_name"| MDB
```

Tres cosas que conviene decir en voz alta al presentar el modelo:

1. **No hay FK entre bases.** `reservations.owner_user` o `courts.manager_user` no son claves
   foráneas a `users.username`: son el claim `username` del token, copiado al crear la fila.
   La integridad de la identidad la garantiza Cognito, no PostgreSQL.
2. **`owner_name` es una copia deliberada.** Cuando un `PLAYER` reserva, `matchpoint` pide el
   perfil a `users` por HTTP y guarda el nombre que le devolvieron. Si `users` está caído
   después, la reserva ya creada se sigue leyendo: el dato viaja, no se consulta en caliente.
3. **`courts` fusiona complejo deportivo y cancha** del diseño original: cada cancha lleva su
   `sector` y su `has_parking`, que eran atributos del complejo.

---

## 4. Ver el modelo en vivo

Con el stack levantado, en pgAdmin (`http://localhost:9090/pgadmin/`): clic derecho sobre la
base → **ERD For Database**, que dibuja el diagrama a partir de las FK reales creadas por
Hibernate. Es la comprobación de que este documento y la base coinciden.

En terminal, lo mismo se ve entrando al contenedor de la base:

```bash
docker compose exec matchpoint-db psql -U matchpoint_app -d matchpoint_db -c "\d+ matches"
```
