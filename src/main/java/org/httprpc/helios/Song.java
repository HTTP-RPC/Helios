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

import org.httprpc.kilo.Name;
import org.httprpc.kilo.sql.Column;
import org.httprpc.kilo.sql.ForeignKey;
import org.httprpc.kilo.sql.Identifier;
import org.httprpc.kilo.sql.Index;
import org.httprpc.kilo.sql.PrimaryKey;
import org.httprpc.kilo.sql.Table;

import static org.httprpc.kilo.util.Optionals.*;

@Table("Song")
public class Song {
    private Integer id;

    private String artist;
    private String sortableArtist;
    private String album;
    private String sortableAlbum;
    private String title;
    private String sortableTitle;

    private Integer time;

    private String genre;
    private Integer year;

    private Integer trackNumber;
    private Integer discNumber;

    private Boolean compilation;

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
    @Identifier(1)
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
    @Identifier(2)
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

    @Column("title")
    @Identifier(3)
    @Index
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;

        sortableTitle = map(title, MusicLibrary::getSortableValue);
    }

    public String getSortableTitle() {
        return sortableTitle;
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

    @Column("discNumber")
    public Integer getDiscNumber() {
        return discNumber;
    }

    public void setDiscNumber(Integer discNumber) {
        this.discNumber = discNumber;
    }

    @Column("compilation")
    public Boolean isCompilation() {
        return compilation;
    }

    public void setCompilation(Boolean compilation) {
        this.compilation = compilation;
    }

    @Column("type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
