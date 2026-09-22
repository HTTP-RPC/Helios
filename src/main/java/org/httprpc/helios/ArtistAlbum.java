/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.httprpc.helios;

import org.httprpc.kilo.sql.Column;
import org.httprpc.kilo.sql.Table;

import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;

import static org.httprpc.kilo.util.Optionals.*;

@Table("ArtistAlbum")
public class ArtistAlbum {
    private String artist;
    private String sortableArtist;

    private String album;
    private String sortableAlbum;

    private SoftReference<BufferedImage> artwork = new SoftReference<>(null);

    @Column("artist")
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
        return artwork.get();
    }

    public void setArtwork(BufferedImage artwork) {
        this.artwork = new SoftReference<>(artwork);
    }

    @Override
    public String toString() {
        return artist.isEmpty() ? sortableAlbum : sortableArtist;
    }
}
