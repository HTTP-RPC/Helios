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

create index idx_song_artist_album on Song(artist, album);

create view Artist as select distinct artist as name from Song;
create view Album as select distinct artist, album as name from Song;

create view ExpandedArtist as select name,
    (select count(*) from Album where artist = Artist.name) as albumCount,
    (select count(*) from Song where artist = Artist.name) as songCount
from Artist;
