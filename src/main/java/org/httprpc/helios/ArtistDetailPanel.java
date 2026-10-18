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

import org.httprpc.sierra.ColumnPanel;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.UILoader;

import javax.swing.JButton;
import javax.swing.JLabel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.SequencedMap;
import java.util.TreeSet;
import java.util.prefs.Preferences;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;
import static org.httprpc.kilo.util.Optionals.*;

public class ArtistDetailPanel extends CollectionDetailPanel {
    private @Outlet JLabel nameLabel = null;
    private @Outlet JButton playAllButton = null;

    private @Outlet ColumnPanel albumListPanel = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(ArtistDetailPanel.class.getName());

    private static final Comparator<ArtistAlbum> nameComparator = Comparator.comparing(ArtistAlbum::getArtist);

    private static final Comparator<ArtistAlbum> yearComparator = (artistAlbum1, artistAlbum2) -> {
        var years1 = artistAlbum1.getYears();
        var year1 = years1.isEmpty() ? 0 : years1.getLast();

        var years2 = artistAlbum2.getYears();
        var year2 = years2.isEmpty() ? 0 : years2.getLast();

        return Integer.compare(year1, year2);
    };

    public ArtistDetailPanel(Artist artist, SequencedMap<String, List<Song>> albums) {
        add(UILoader.load(this, "ArtistDetailPanel.xml", resourceBundle));

        nameLabel.setText(artist.getName());

        var artistAlbums = new ArrayList<ArtistAlbum>(albums.size());

        for (var entry : albums.entrySet()) {
            var name = entry.getKey();
            var songs = entry.getValue();

            var genres = new LinkedHashSet<String>();
            var years = new TreeSet<Integer>();

            var n = songs.size();

            for (var i = 0; i < n; i++) {
                var song = songs.get(i);

                perform(song.getGenre(), genres::add);
                perform(song.getYear(), years::add);
            }

            var artistAlbum = new ArtistAlbum();

            artistAlbum.setArtist(artist.getName());
            artistAlbum.setAlbum(name);

            artistAlbum.setGenres(genres);
            artistAlbum.setYears(years);

            artistAlbums.add(artistAlbum);
        }

        var preferences = Preferences.userRoot().node(MainFrame.class.getName());

        var albumSortOrder = map(preferences.get(MainFrame.ALBUM_SORT_ORDER_KEY, null), AlbumSortOrder::valueOf);

        artistAlbums.sort(switch (albumSortOrder) {
            case NAME -> nameComparator;
            case YEAR_ASCENDING -> yearComparator.thenComparing(nameComparator);
            case YEAR_DESCENDING -> yearComparator.reversed().thenComparing(nameComparator);
        });

        playAllButton.addActionListener(event -> {
            var artistSongs = listOf(flatten(mapAll(artistAlbums, artistAlbum -> albums.get(artistAlbum.getAlbum())), songs -> songs));

            MainFrame.getInstance().playAll(artistSongs, 0);
        });

        for (var artistAlbum : artistAlbums) {
            albumListPanel.add(new AlbumDetailPanel(artistAlbum, albums.get(artistAlbum.getAlbum())));
        }

        setScrollableTracksViewportWidth(true);
    }

    @Override
    public void showCurrentSong(Song song) {
        var n = albumListPanel.getComponentCount();

        for (var i = 0; i < n; i++) {
            if (albumListPanel.getComponent(i) instanceof AlbumDetailPanel albumDetailPanel) {
                albumDetailPanel.showCurrentSong(song);
            }
        }
    }

    public void scrollToSong(Song song) {
        var album = song.getAlbum();

        var n = albumListPanel.getComponentCount();

        for (var i = 0; i < n; i++) {
            var albumDetailPanel = (AlbumDetailPanel)albumListPanel.getComponent(i);

            if (albumDetailPanel.getArtistAlbum().getAlbum().equals(album)) {
                albumDetailPanel.scrollToSong(song);

                break;
            }
        }
    }

    public void deactivate() {
        var n = albumListPanel.getComponentCount();

        for (var i = 0; i < n; i++) {
            if (albumListPanel.getComponent(i) instanceof AlbumDetailPanel albumDetailPanel) {
                albumDetailPanel.deactivate();
            }
        }
    }
}
