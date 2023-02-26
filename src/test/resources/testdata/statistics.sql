-- The InitialDataLoader is called before this, so the roles and one band is already in the H2 db

insert into band (name)
values ('Band 2'),
       ('Band 3');

insert into instrument (id, band_id, name, sort_order)
values (1, 1, 'Instrument 1', 1),
       (2, 1, 'Instrument 2', 2),
       (3, 1, 'Instrument 3', 3);

insert into score (id, band_id, title, genre, scanned)
values (1, 1, 'Score 1', 'Genre A', 1),
       (2, 1, 'Score 2', 'Genre A', 0),
       (3, 1, 'Score 3', 'Genre B', 0);

insert into score_instrument (score_id, instrument_id, page, length)
values (1, 1, 1, 3),
       (1, 2, 2, 2);

insert into playlist (band_id, name)
values (1, 'Playlist 1'),
       (1, 'Playlist 2'),
       (1, 'Playlist 3');