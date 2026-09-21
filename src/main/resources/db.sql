drop table if exists Song;

create table Song (
    id integer primary key,
    artist text not null collate nocase,
    album text not null collate nocase,
    title text not null collate nocase,
    time integer not null,
    genre text collate nocase,
    year integer,
    trackNumber integer,
    discNumber integer,
    compilation boolean not null,
    type text not null check (type in ('mp3', 'm4a')),
    unique (artist, album, title)
);

create index idx_genre on Song(genre);
create index idx_compilation on Song(compilation);

drop table if exists Playlist;

create table Playlist (
    id integer primary key,
    name text not null collate nocase,
    unique (name)
);

drop table if exists PlaylistSong;

create table PlaylistSong (
    playlistID integer not null,
    songID integer not null,
    primary key (playlistID, songID),
    foreign key (playlistID) references Playlist (id) on delete cascade,
    foreign key (songID) references Song (id) on delete cascade
);

drop view if exists Artist;

create view Artist as select distinct artist as name from Song where compilation = false;

drop view if exists ArtistAlbum;

create view ArtistAlbum as select distinct artist, album from Song where compilation = false;

drop view if exists ExpandedArtist;

create view ExpandedArtist as select name,
    (select count(*) from ArtistAlbum where artist = Artist.name) as albumCount,
    (select count(*) from Song where artist = Artist.name) as songCount
from Artist;

drop view if exists Genre;

create view Genre as select distinct genre as name from Song
where genre is not null;

drop view if exists GenreArtist;

create view GenreArtist as select distinct genre, artist from Song
where genre is not null;

drop view if exists ExpandedGenre;

create view ExpandedGenre as select name,
    (select count(*) from GenreArtist where genre = Genre.name) as artistCount,
    (select count(*) from Song where genre = Genre.name) as songCount
from Genre;

drop view if exists ExpandedPlaylist;

create view ExpandedPlaylist as select id, name,
    (select count(*) from (
        select distinct artist from Song
        join PlaylistSong on songID = Song.id and playlistID = Playlist.id
    )) as artistCount,
    (select count(*) from PlaylistSong where playlistID = Playlist.id) as songCount
from Playlist;
