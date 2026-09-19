// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.kilo.sql.Column;
import org.httprpc.kilo.sql.Index;
import org.httprpc.kilo.sql.Table;

import java.awt.image.BufferedImage;

import static org.httprpc.kilo.util.Optionals.*;

@Table("ArtistAlbum")
public class ArtistAlbum {
    private String artist;
    private String sortableArtist;

    private String album;
    private String sortableAlbum;

    private BufferedImage artwork;

    @Column("artist")
    @Index(1)
    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;

        sortableArtist = map(artist, MusicLibrary::getSortableValue);
    }

    public String getSortableArtist() {
        return sortableArtist;
    }

    @Column("album")
    @Index(2)
    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;

        sortableAlbum = map(album, MusicLibrary::getSortableValue);
    }

    public String getSortableAlbum() {
        return sortableAlbum;
    }

    public BufferedImage getArtwork() {
        return artwork;
    }

    public void setArtwork(BufferedImage artwork) {
        this.artwork = artwork;
    }
}
