// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.kilo.Name;
import org.httprpc.kilo.sql.Column;
import org.httprpc.kilo.sql.ForeignKey;
import org.httprpc.kilo.sql.Identifier;
import org.httprpc.kilo.sql.Index;
import org.httprpc.kilo.sql.PrimaryKey;
import org.httprpc.kilo.sql.Table;

@Table("Song")
public class Song {
    private Integer id;
    private String artist;
    private String album;
    private String title;
    private Integer time;

    private String genre;
    private Integer year;

    private Integer trackNumber;
    private Integer trackCount;

    private Integer discNumber;
    private Integer discCount;

    private Boolean compilation;
    private Boolean classical;

    private String composer;

    private String type;

    @Name("id")
    @Column("id")
    @PrimaryKey
    public Integer getID() {
        return id;
    }

    public void setID(Integer id) {
        this.id = id;
    }

    @Column("artist")
    @ForeignKey(Artist.class)
    @Index(1)
    @Identifier(1)
    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    @Column("album")
    @Index(2)
    @Identifier(2)
    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    @Column("title")
    @Index(3)
    @Identifier(3)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Column("time")
    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }

    @Column("genre")
    @ForeignKey(Genre.class)
    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    @Column("year")
    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    @Column("trackNumber")
    public Integer getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(Integer trackNumber) {
        this.trackNumber = trackNumber;
    }

    @Column("trackCount")
    public Integer getTrackCount() {
        return trackCount;
    }

    public void setTrackCount(Integer trackCount) {
        this.trackCount = trackCount;
    }

    @Column("discNumber")
    public Integer getDiscNumber() {
        return discNumber;
    }

    public void setDiscNumber(Integer discNumber) {
        this.discNumber = discNumber;
    }

    @Column("discCount")
    public Integer getDiscCount() {
        return discCount;
    }

    public void setDiscCount(Integer discCount) {
        this.discCount = discCount;
    }

    @Column("compilation")
    public Boolean isCompilation() {
        return compilation;
    }

    public void setCompilation(Boolean compilation) {
        this.compilation = compilation;
    }

    @Column("classical")
    public Boolean isClassical() {
        return classical;
    }

    public void setClassical(Boolean classical) {
        this.classical = classical;
    }

    @Column("composer")
    public String getComposer() {
        return composer;
    }

    public void setComposer(String composer) {
        this.composer = composer;
    }

    @Column("type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
