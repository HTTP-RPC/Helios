package org.httprpc.helios;

import org.httprpc.kilo.sql.Column;
import org.httprpc.kilo.sql.Table;

@Table("ExpandedPlaylist")
public interface ExpandedPlaylist extends Playlist {
    @Column("artistCount")
    Integer getArtistCount();

    @Column("songCount")
    Integer getSongCount();
}
