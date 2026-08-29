package org.httprpc.helios;

import org.httprpc.kilo.Name;
import org.httprpc.kilo.Required;
import org.httprpc.kilo.sql.Column;
import org.httprpc.kilo.sql.ForeignKey;
import org.httprpc.kilo.sql.Index;
import org.httprpc.kilo.sql.PrimaryKey;
import org.httprpc.kilo.sql.Table;

@Table("Song")
public interface Song {
    @Name("id")
    @Column("id")
    @PrimaryKey
    Integer getID();

    @Column("artist")
    @ForeignKey(Artist.class)
    @Index(1)
    @Required
    String getArtist();
    void setArtist(String artist);

    @Column("album")
    @Index(2)
    @Required
    String getAlbum();
    void setAlbum(String album);

    @Column("title")
    @Required
    String getTitle();
    void setTitle(String title);

    @Column("time")
    @Required
    Integer getTime();
    void setTime(Integer time);

    @Column("genre")
    String getGenre();
    void setGenre(String genre);

    @Column("year")
    Integer getYear();
    void setYear(Integer year);

    @Column("trackNumber")
    @Index(3)
    Integer getTrackNumber();
    void setTrackNumber(Integer trackNumber);

    @Column("trackCount")
    Integer getTrackCount();
    void setTrackCount(Integer trackCount);

    @Column("discNumber")
    Integer getDiscNumber();
    void setDiscNumber(Integer discNumber);

    @Column("discCount")
    Integer getDiscCount();
    void setDiscCount(Integer discCount);
}
