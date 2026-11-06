-- выполнить в базе tournament_db

INSERT INTO users (name, email) VALUES
    ('Иванов Иван', 'ivanov@mail.ru'),
    ('Петров Пётр', 'petrov@mail.ru'),
    ('Сидоров Олег', 'sidorov@mail.ru'),
    ('Смирнов Антон', 'smirnov@mail.ru'),
    ('Кузнецов Илья', 'kuznetsov@mail.ru'),
    ('Попов Денис', 'popov@mail.ru');

INSERT INTO teams (name, captain_id) VALUES
    ('Орлы', 1),
    ('Волки', 3),
    ('Лисы', 5);

INSERT INTO team_members (team_id, user_id) VALUES
    (1, 1), (1, 2),
    (2, 3), (2, 4),
    (3, 5), (3, 6);

INSERT INTO tournaments (name, team_limit, status, winner_id, finished_at) VALUES
    ('Осенний кубок', 4, 'REGISTRATION', NULL, NULL),
    ('Летний кубок', 2, 'FINISHED', 1, now() - interval '30 days');

INSERT INTO team_applications (team_id, tournament_id, status, applied_at) VALUES
    (1, 1, 'PENDING', now() - interval '2 days'),
    (2, 1, 'APPROVED', now() - interval '3 days'),
    (1, 2, 'APPROVED', now() - interval '40 days'),
    (2, 2, 'APPROVED', now() - interval '40 days');

INSERT INTO matches (tournament_id, team1_id, team2_id, score1, score2, status, played_at) VALUES
    (2, 1, 2, 2, 1, 'FINISHED', now() - interval '31 days');
