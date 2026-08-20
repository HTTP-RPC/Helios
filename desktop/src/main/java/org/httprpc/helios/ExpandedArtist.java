package org.httprpc.helios;

import org.httprpc.kilo.sql.Column;
import org.httprpc.kilo.sql.Index;
import org.httprpc.kilo.sql.Table;

@Table("ExpandedArtist")
public interface ExpandedArtist {
    @Column("name")
    @Index
    String getName();

    @Column("albumCount")
    Integer getAlbumCount();

    @Column("songCount")
    Integer getSongCount();
}
