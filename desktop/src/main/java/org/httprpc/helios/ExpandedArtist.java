package org.httprpc.helios;

import org.httprpc.kilo.sql.Column;
import org.httprpc.kilo.sql.Table;

@Table("ExpandedArtist")
public interface ExpandedArtist {
    @Column("name")
    String getName();

    @Column("albumCount")
    Integer getAlbumCount();

    @Column("songCount")
    Integer getSongCount();
}
