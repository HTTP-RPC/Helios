create table Song (
    artist varchar(128) not null,
    album varchar(128) not null,
    title varchar(256) not null,
    time int not null,
    genre varchar(64),
    year int,
    trackNumber int,
    trackCount int,
    discNumber int,
    discCount int
);
