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
import javax.swing.UIManager;
import java.awt.Insets;
import java.text.NumberFormat;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Optionals.*;

public class EditSongDialog extends AbstractDialog {
    private List<Song> songs;
    private int songIndex;

    private @Outlet JTextField artistTextField = null;
    private @Outlet JTextField albumTextField = null;
    private @Outlet JTextField titleTextField = null;

    private @Outlet SuggestionPicker genreSuggestionPicker = null;
    private @Outlet NumberField yearTextField = null;

    private @Outlet NumberField trackNumberTextField = null;
    private @Outlet NumberField discNumberTextField = null;

    private @Outlet JCheckBox compilationCheckBox = null;

    private @Outlet JButton previousButton = null;
    private @Outlet JButton nextButton = null;

    private @Outlet JButton cancelButton = null;
    private @Outlet JButton okButton = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(EditSongDialog.class.getName());

    public EditSongDialog(MainFrame owner, List<Song> songs, int songIndex) {
        super(owner);

        this.songs = songs;
        this.songIndex = songIndex;

        setTitle(resourceBundle.getString("windowTitle"));

        setContentPane(UILoader.load(this, "EditSongDialog.xml", resourceBundle));

        var integerFormat = NumberFormat.getIntegerInstance();

        integerFormat.setGroupingUsed(false);

        yearTextField.setFormat(integerFormat);

        trackNumberTextField.setFormat(integerFormat);
        discNumberTextField.setFormat(integerFormat);

        previousButton.setMargin(new Insets(0, 0, 0, 0));
        previousButton.addActionListener(event -> movePrevious());

        nextButton.setMargin(new Insets(0, 0, 0, 0));
        nextButton.addActionListener(event -> moveNext());

        cancelButton.addActionListener(event -> cancel());
        okButton.addActionListener(event -> save(this::dispose));

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

    @Override
    protected void cancel() {
        if (cancelButton.isEnabled()) {
            super.cancel();
        }
    }

    private void load() {
        var song = songs.get(songIndex);

        artistTextField.setText(song.getArtist());
        albumTextField.setText(song.getAlbum());
        titleTextField.setText(song.getTitle());

        genreSuggestionPicker.setText(song.getGenre());
        genreSuggestionPicker.setSuggestions(MusicLibrary.getGenreSuggestions());

        yearTextField.setValue(song.getYear());

        trackNumberTextField.setValue(song.getTrackNumber());
        discNumberTextField.setValue(song.getDiscNumber());

        compilationCheckBox.setSelected(song.isCompilation());

        updateControls();
    }

    private void save(Runnable callback) {
        var previousSong = songs.get(songIndex);

        var update = false;

        var artist = artistTextField.getText().strip();

        if (artist.isEmpty()) {
            alertRequired("artist", artistTextField);
            return;
        }

        update |= !artist.equals(previousSong.getArtist());

        var album = albumTextField.getText().strip();

        if (album.isEmpty()) {
            alertRequired("album", albumTextField);
            return;
        }

        update |= !album.equals(previousSong.getAlbum());

        var title = titleTextField.getText().strip();

        if (title.isEmpty()) {
            alertRequired("title", titleTextField);
            return;
        }

        update |= !title.equals(previousSong.getTitle());

        var genre = genreSuggestionPicker.getText().strip();

        update |= !genre.equals(previousSong.getGenre());

        var year = map(yearTextField.getValue(), Number::intValue);

        update |= !Objects.equals(year, previousSong.getYear());

        var trackNumber = map(trackNumberTextField.getValue(), Number::intValue);

        update |= !Objects.equals(trackNumber, previousSong.getTrackNumber());

        var discNumber = map(discNumberTextField.getValue(), Number::intValue);

        update |= !Objects.equals(discNumber, previousSong.getDiscNumber());

        var compilation = compilationCheckBox.isSelected();

        if (compilation && genre.isEmpty()) {
            alertRequired("genre", genreSuggestionPicker);
            return;
        }

        update |= compilation != previousSong.isCompilation();

        if (!update) {
            callback.run();
            return;
        }

        var song = new Song();

        song.setID(previousSong.getID());

        song.setArtist(artist);
        song.setAlbum(album);
        song.setTitle(title);

        song.setTime(previousSong.getTime());

        if (!genre.isEmpty()) {
            song.setGenre(genre);
        }

        song.setYear(year);

        song.setTrackNumber(trackNumber);
        song.setDiscNumber(discNumber);

        song.setCompilation(compilation);

        song.setType(previousSong.getType());

        var glassPane = getGlassPane();

        glassPane.setVisible(true);

        rootPane.requestFocus();

        previousButton.setEnabled(false);
        nextButton.setEnabled(false);

        cancelButton.setEnabled(false);
        okButton.setEnabled(false);

        MainFrame.getTaskExecutor().execute(() -> MusicLibrary.updateSong(song, previousSong), (result, exception) -> {
            if (coalesce(result, () -> false)) {
                MainFrame.getInstance().loadAll();

                songs.set(songIndex, song);

                glassPane.setVisible(false);

                cancelButton.setEnabled(true);
                okButton.setEnabled(true);

                callback.run();
            } else {
                UIManager.getLookAndFeel().provideErrorFeedback(artistTextField);
            }
        });
    }

    private void movePrevious() {
        save(() -> {
            songIndex--;

            load();
        });
    }

    private void moveNext() {
        save(() -> {
            songIndex++;

            load();
        });
    }

    private void updateControls() {
        previousButton.setEnabled(songIndex > 0);
        nextButton.setEnabled(songIndex < songs.size() - 1);
    }
}
