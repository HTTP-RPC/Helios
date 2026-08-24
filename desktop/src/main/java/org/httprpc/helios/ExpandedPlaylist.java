package org.httprpc.helios;

import org.httprpc.kilo.sql.Column;
import org.httprpc.kilo.sql.Index;
import org.httprpc.kilo.sql.PrimaryKey;
import org.httprpc.kilo.sql.Table;

@Table("ExpandedPlaylist")
public interface ExpandedPlaylist {
    @Column("name")
    @PrimaryKey
    @Index
    String getName();

    @Column("artistCount")
    Integer getArtistCount();

    @Column("songCount")
    Integer getSongCount();
}
