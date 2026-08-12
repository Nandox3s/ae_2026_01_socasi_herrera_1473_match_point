INSERT INTO courts (id, name, sector, has_parking, sport_type, floor_type, price_per_hour, active, manager_user, created_at) VALUES
    (1, 'North Court 1',   'North',   TRUE,  'BASKET', 'Concrete',  12.50, TRUE,  'manager_josue', TIMESTAMP '2026-07-05 08:00:00'),
    (2, 'South Court 2',   'South',   FALSE, 'BASKET', 'Parquet',   15.00, TRUE,  'manager_josue', TIMESTAMP '2026-07-05 08:10:00'),
    (3, 'Central Court 3', 'Central', TRUE,  'BASKET', 'Synthetic', 18.00, FALSE, 'manager_josue', TIMESTAMP '2026-07-05 08:20:00'),
    (4, 'Valley Court 4',  'Valley',  FALSE, 'BASKET', 'Concrete',  10.00, TRUE,  'manager_ana',   TIMESTAMP '2026-07-05 08:30:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO tournaments (id, name, sport_type, max_teams, prize, manager_user, status, court_id, champion_team_id, created_at) VALUES
    (1, 'MatchPoint Cup 2026', 'BASKET', 4, 'Trophy and medals', 'manager_josue', 'REGISTRATION', 1,    NULL, TIMESTAMP '2026-07-06 09:00:00'),
    (2, 'Lightning Cup',       'BASKET', 2, 'Sports kit',        'manager_josue', 'REGISTRATION', 2,    NULL, TIMESTAMP '2026-07-06 09:10:00'),
    (3, 'Summer Cup 2025',     'BASKET', 2, 'Trophy',            'manager_josue', 'FINISHED',     1,    NULL, TIMESTAMP '2025-07-10 09:00:00'),
    (4, 'Ana Open',            'BASKET', 4, NULL,                'manager_ana',   'REGISTRATION', NULL, NULL, TIMESTAMP '2026-07-06 09:20:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO teams (id, tournament_id, registered_by_user, name, contact_name, contact_email, contact_phone,
                   eliminated, matches_played, matches_won, matches_lost, points_for, points_against, current_round, created_at) VALUES
    (1, 1, 'player_fernando',  'Falcons', 'Fernando Socasi', 'falcons@puce.edu.ec', '0999555666', FALSE, 0, 0, 0,  0,  0, 0, TIMESTAMP '2026-07-07 10:00:00'),
    (2, 1, 'player_fernando',  'Eagles',  'Fernando Socasi', 'eagles@puce.edu.ec',  '0999555666', FALSE, 0, 0, 0,  0,  0, 0, TIMESTAMP '2026-07-07 10:05:00'),
    (3, 1, 'player_luis', 'Sharks',  'Luis Cabrera',  'sharks@puce.edu.ec',  '0999777888', FALSE, 0, 0, 0,  0,  0, 0, TIMESTAMP '2026-07-07 10:10:00'),
    (4, 2, 'player_fernando',  'Titans',  'Fernando Socasi', 'titans@puce.edu.ec',  '0999555666', FALSE, 0, 0, 0,  0,  0, 0, TIMESTAMP '2026-07-07 11:00:00'),
    (5, 2, 'player_luis', 'Wolves',  'Luis Cabrera',  'wolves@puce.edu.ec',  '0999777888', FALSE, 0, 0, 0,  0,  0, 0, TIMESTAMP '2026-07-07 11:05:00'),
    (6, 3, 'player_fernando',  'Comets',  'Fernando Socasi', 'comets@puce.edu.ec',  '0999555666', FALSE, 1, 1, 0, 30, 24, 1, TIMESTAMP '2025-07-11 10:00:00'),
    (7, 3, 'player_luis', 'Meteors', 'Luis Cabrera',  'meteors@puce.edu.ec', '0999777888', TRUE,  1, 0, 1, 24, 30, 1, TIMESTAMP '2025-07-11 10:05:00')
ON CONFLICT (id) DO NOTHING;

UPDATE tournaments SET champion_team_id = 6 WHERE id = 3 AND champion_team_id IS NULL;

INSERT INTO matches (id, tournament_id, round_number, position_in_round, home_team_id, away_team_id,
                     home_score, away_score, winner_team_id, status, scheduled_at, created_at) VALUES
    (1, 3, 1, 0, 6, 7, 30, 24, 6, 'PLAYED', TIMESTAMP '2025-07-20 16:00:00', TIMESTAMP '2025-07-11 12:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO reservations (id, court_id, owner_user, owner_name, starts_at, duration_minutes, status, created_at) VALUES
    (1, 1, 'player_fernando',  'Fernando Socasi', TIMESTAMP '2026-08-10 18:00:00', 60, 'CONFIRMED', TIMESTAMP '2026-07-08 12:00:00'),
    (2, 2, 'player_fernando',  'Fernando Socasi', TIMESTAMP '2026-08-11 20:00:00', 90, 'CONFIRMED', TIMESTAMP '2026-07-08 12:05:00'),
    (3, 1, 'player_luis', 'Luis Cabrera',  TIMESTAMP '2026-08-12 09:00:00', 60, 'CANCELLED', TIMESTAMP '2026-07-08 12:10:00'),
    (4, 2, 'player_luis', 'Luis Cabrera',  TIMESTAMP '2026-08-13 07:00:00', 60, 'CONFIRMED', TIMESTAMP '2026-07-08 12:15:00')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('courts', 'id'),       COALESCE((SELECT MAX(id) FROM courts), 1), true);
SELECT setval(pg_get_serial_sequence('tournaments', 'id'),  COALESCE((SELECT MAX(id) FROM tournaments), 1), true);
SELECT setval(pg_get_serial_sequence('teams', 'id'),        COALESCE((SELECT MAX(id) FROM teams), 1), true);
SELECT setval(pg_get_serial_sequence('matches', 'id'),      COALESCE((SELECT MAX(id) FROM matches), 1), true);
SELECT setval(pg_get_serial_sequence('reservations', 'id'), COALESCE((SELECT MAX(id) FROM reservations), 1), true);
