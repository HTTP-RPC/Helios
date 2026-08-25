package org.httprpc.helios;

import org.httprpc.kilo.sql.Column;
import org.httprpc.kilo.sql.ForeignKey;
import org.httprpc.kilo.sql.Table;

@Table("PlaylistSong")
public record PlaylistSong(
    @Column("playlistID")
    @ForeignKey(Playlist.class)
    Integer playlistID,

    @Column("songID")
    @ForeignKey(Song.class)
    Integer songID
) {
}
