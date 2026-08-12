INSERT INTO users (id, cognito_id, username, name, email, phone, created_at) VALUES
    (1, 'seed-sub-manager-josue', 'manager_josue', 'Josué Herrera',  'josue.herrera@puce.edu.ec',  '0999111222', TIMESTAMP '2026-07-01 09:00:00'),
    (2, 'seed-sub-manager-ana',   'manager_ana',   'Ana Lopez',      'ana.lopez@puce.edu.ec',      '0999333444', TIMESTAMP '2026-07-01 09:05:00'),
    (3, 'seed-sub-player-fernando',    'player_fernando',    'Fernando Socasi',  'fernando.socasi@puce.edu.ec',  '0999555666', TIMESTAMP '2026-07-02 10:00:00'),
    (4, 'seed-sub-player-luis',   'player_luis',   'Luis Cabrera',   'luis.cabrera@puce.edu.ec',   '0999777888', TIMESTAMP '2026-07-02 10:10:00')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE((SELECT MAX(id) FROM users), 1), true);
