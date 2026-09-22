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

import org.httprpc.sierra.NumberField;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.SuggestionPicker;
import org.httprpc.sierra.UILoader;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import java.text.NumberFormat;
import java.util.List;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Optionals.*;

public class EditAlbumDialog extends AbstractDialog {
    private String artist;
    private String album;
    private List<Song> songs;

    private String genre;
    private Integer year;

    private boolean compilation;

    private @Outlet JTextField artistTextField = null;
    private @Outlet JTextField albumTextField = null;

    private @Outlet SuggestionPicker genreSuggestionPicker = null;
    private @Outlet NumberField yearTextField = null;

    private @Outlet JCheckBox compilationCheckBox = null;

    private @Outlet JButton cancelButton = null;
    private @Outlet JButton okButton = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(EditAlbumDialog.class.getName());

    public EditAlbumDialog(MainFrame owner, String artist, String album, List<Song> songs,
        String genre, Integer year,
        boolean compilation) {
        super(owner);

        this.artist = artist;
        this.album = album;
        this.songs = songs;

        this.genre = genre;
        this.year = year;

        this.compilation = compilation;

        setTitle(resourceBundle.getString("windowTitle"));

        setContentPane(UILoader.load(this, "EditAlbumDialog.xml", resourceBundle));

        artistTextField.setEnabled(!compilation);

        var integerFormat = NumberFormat.getIntegerInstance();

        integerFormat.setGroupingUsed(false);

        yearTextField.setFormat(integerFormat);

        compilationCheckBox.addActionListener(event -> updateCompilation());

        cancelButton.addActionListener(event -> cancel());
        okButton.addActionListener(event -> save());

        rootPane.setDefaultButton(okButton);

        setResizable(false);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            load();
        }

        super.setVisible(visible);
    }

    private void updateCompilation() {
        if (compilationCheckBox.isSelected()) {
            artistTextField.setText(null);
            artistTextField.setEnabled(false);
        } else {
            artistTextField.setText(artist);
            artistTextField.setEnabled(true);
        }
    }

    @Override
    protected void cancel() {
        if (cancelButton.isEnabled()) {
            super.cancel();
        }
    }

    private void load() {
        artistTextField.setText(artist);
        albumTextField.setText(album);

        genreSuggestionPicker.setText(genre);
        genreSuggestionPicker.setSuggestions(MusicLibrary.getGenreSuggestions());

        yearTextField.setValue(year);

        compilationCheckBox.setSelected(compilation);
    }

    private void save() {
        var artist = artistTextField.getText().strip();

        var album = albumTextField.getText().strip();

        if (album.isEmpty()) {
            alertRequired("artist", albumTextField);
            return;
        }

        var genre = genreSuggestionPicker.getText().strip();

        var year = map(yearTextField.getValue(), Number::intValue);

        var compilation = compilationCheckBox.isSelected();

        if (compilation) {
            if (genre.isEmpty()) {
                alertRequired("genre", genreSuggestionPicker);
                return;
            }
        } else {
            if (artist.isEmpty()) {
                alertRequired("artist", artistTextField);
                return;
            }
        }

        getGlassPane().setVisible(true);

        rootPane.requestFocus();

        cancelButton.setEnabled(false);
        okButton.setEnabled(false);

        MainFrame.getTaskExecutor().execute(() -> {
            for (var previousSong : songs) {
                var song = new Song();

                song.setID(previousSong.getID());
                song.setArtist(compilation ? previousSong.getArtist() : artist);
                song.setAlbum(album);
                song.setTitle(previousSong.getTitle());
                song.setTime(previousSong.getTime());
                song.setGenre(genre);
                song.setYear(year);
                song.setTrackNumber(previousSong.getTrackNumber());
                song.setDiscNumber(previousSong.getDiscNumber());
                song.setCompilation(compilation);
                song.setType(previousSong.getType());

                MusicLibrary.updateSong(song, previousSong);
            }

            return null;
        }, (result, exception) -> {
            MainFrame.getInstance().loadAll();

            dispose();
        });
    }
}
