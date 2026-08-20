package org.httprpc.helios;

import org.httprpc.kilo.sql.Column;
import org.httprpc.kilo.sql.ForeignKey;
import org.httprpc.kilo.sql.Index;
import org.httprpc.kilo.sql.Table;

@Table("Song")
public interface Song {
    @Column("artist")
    @ForeignKey(ExpandedArtist.class)
    @Index
    String getArtist();

    @Column("album")
    @Index
    String getAlbum();

    @Column("title")
    @Index
    String getTitle();

    @Column("time")
    Integer getTime();

    @Column("genre")
    String getGenre();

    @Column("year")
    Integer getYear();

    @Column("trackNumber")
    Integer getTrackNumber();

    @Column("trackCount")
    Integer getTrackCount();

    @Column("discNumber")
    Integer getDiscNumber();

    @Column("discCount")
    Integer getDiscCount();
}
