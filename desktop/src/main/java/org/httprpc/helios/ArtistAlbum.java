// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.kilo.sql.Column;
import org.httprpc.kilo.sql.Table;

import static org.httprpc.kilo.util.Optionals.*;

@Table("ArtistAlbum")
public class ArtistAlbum {
    private String artist;
    private String sortableArtist;

    private String album;
    private String sortableAlbum;

    @Column("artist")
    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;

        sortableArtist = map(artist, Library::getSortableValue);
    }

    public String getSortableArtist() {
        return sortableArtist;
    }

    @Column("album")
    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;

        sortableAlbum = map(album, Library::getSortableValue);
    }

    public String getSortableAlbum() {
        return sortableAlbum;
    }
}
