INSERT INTO instrument (id, name, short_name, sort_order)
VALUES (1, 'Altsaxofon 1', 'Asax 1', 10),
       (2, 'Tenorsaxofon', 'Tsax', 30),
       (3, 'Altsaxofon 2', 'Asax 2', 20),
       (4, 'Trumpet 1', 'Tp 1', 41),
       (5, 'Trumpet 2', 'Tp 2', 50),
       (6, 'Trombone', 'Tb', 60),
       (7, 'Piano', 'Pi', 70),
       (8, 'Gitarr', 'Git', 80),
       (9, 'Bas', 'Bas', 90),
       (10, 'Trummor', 'Trum', 100),
       (11, 'Sång', 'Sång', 75),
       (12, 'Tenorsaxofon 2', 'Tsax 2', 31),
       (13, 'Barytonsax', 'Bsax', 34),
       (14, 'Trumpet 3', 'Tp 3', 55),
       (15, 'Altsaxofon 3', 'Asax 3', 25),
       (16, 'Violin', 'Vi', 110),
       (17, 'Cello', 'Cel', 120),
       (18, 'Dragspel', 'Drg', 130),
       (19, 'Violin 1', 'Vi 1', 111),
       (20, 'Violin 2', 'Vi 2', 112),
       (22, 'Violin 3', 'Vi 3', 113),
       (23, 'Klarinett', 'Klar', 36),
       (24, 'Trumpet', 'Tp', 40),
       (25, 'Tenorsaxofon 3', 'Tsax 3', 32),
       (26, 'Klarinett 1', 'Klar 1', 37),
       (27, 'Klarinett 2', 'Klar 2', 38),
       (28, 'Althorn', '', 65),
       (29, 'Tenorbasun', '', 66),
       (30, 'Flöjt', 'Flt', 140),
       (31, 'Trombone 2', 'Tb 2', 61),
       (32, 'Trombone 3', 'Tb 3', 62),
       (33, 'Violin obligato', 'Vi obl', 114),
       (34, 'Altsaxofon', 'Asx', 8),
       (35, 'Viola', 'Vla', 116),
       (36, 'Klockspel', 'Klo', 150),
       (37, 'Klarinett 3', 'Kl 3', 39),
       (38, 'Banjo', 'Bjo', 81),
       (39, 'Trumpet 4', 'Tp 4', 56),
       (40, 'Flöjt 2', 'Flt 2', 141);

INSERT INTO `setting` (`id`, `name`)
VALUES (1, 'Min första sättning'),
       (2, 'Terrassorkestern');

INSERT INTO `setting_instrument` (`setting_id`, `instrument_id`)
VALUES (2, 1),
       (2, 2),
       (2, 3),
       (2, 4),
       (2, 5),
       (2, 6),
       (2, 7),
       (2, 8),
       (2, 9),
       (2, 10),
       (2, 11),
       (1, 14),
       (1, 18),
       (1, 19);


INSERT INTO privilege (id, name)
VALUES (1, 'PRINT_SCORE'),
       (2, 'EDIT_SONG'),
       (3, 'EDIT_INSTRUMENT'),
       (4, 'EDIT_PLAYLIST'),
       (5, 'EDIT_USER'),
       (6, 'CONVERT_SCORE'),
       (7, 'UPDATE_TOC');

INSERT INTO role (id, name)
VALUES (3, 'ROLE_ADMIN'),
       (4, 'ROLE_USER');

INSERT INTO role_privilege (role_id, privilege_id)
VALUES (3, 1),
       (4, 1),
       (3, 2),
       (3, 3),
       (3, 4),
       (4, 4),
       (3, 5),
       (3, 6),
       (3, 7);
