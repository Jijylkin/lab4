-- выполнить под postgres в базе tournament_db после скриптов предыдущей лабы

ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'PLAYER'
    CHECK (role IN ('PLAYER', 'ORGANIZER'));
ALTER TABLE users ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;

CREATE UNIQUE INDEX IF NOT EXISTS users_email_ignore_case ON users (lower(email));

INSERT INTO users (name, email, password_hash, role, enabled)
VALUES ('Организатор', 'organizer@tournament.local', '$2a$10$ER9SibYuBt2Ceq5h0HGJf.9lW9skHDH40bZwN9KnR3P3nB54r8p5i', 'ORGANIZER', TRUE)
ON CONFLICT DO NOTHING;
