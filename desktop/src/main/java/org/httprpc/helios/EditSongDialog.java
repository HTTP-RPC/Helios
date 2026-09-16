// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.sierra.NumberField;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.SuggestionPicker;
import org.httprpc.sierra.UILoader;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.text.NumberFormat;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Optionals.*;

public class EditSongDialog extends AbstractDialog {
    private Song song;

    private @Outlet JTextField artistTextField = null;
    private @Outlet JTextField albumTextField = null;
    private @Outlet JTextField titleTextField = null;

    private @Outlet SuggestionPicker genreSuggestionPicker = null;
    private @Outlet NumberField yearTextField = null;

    private @Outlet NumberField trackNumberTextField = null;
    private @Outlet NumberField discNumberTextField = null;

    private @Outlet JCheckBox compilationCheckBox = null;

    private @Outlet JButton cancelButton = null;
    private @Outlet JButton okButton = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(EditSongDialog.class.getName());

    public EditSongDialog(MainFrame owner, Song song) {
        super(owner);

        this.song = song;

        setTitle(resourceBundle.getString("windowTitle"));

        setContentPane(UILoader.load(this, "EditSongDialog.xml", resourceBundle));

        var integerFormat = NumberFormat.getIntegerInstance();

        integerFormat.setGroupingUsed(false);

        yearTextField.setFormat(integerFormat);

        trackNumberTextField.setFormat(integerFormat);
        discNumberTextField.setFormat(integerFormat);

        cancelButton.addActionListener(event -> dispose());
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

    private void load() {
        artistTextField.setText(song.getArtist());
        albumTextField.setText(song.getAlbum());
        titleTextField.setText(song.getTitle());

        genreSuggestionPicker.setText(song.getGenre());
        genreSuggestionPicker.setSuggestions(MusicLibrary.getGenreSuggestions());

        yearTextField.setValue(song.getYear());

        trackNumberTextField.setValue(song.getTrackNumber());
        discNumberTextField.setValue(song.getDiscNumber());

        compilationCheckBox.setSelected(song.isCompilation());
    }

    private void save() {
        var artist = artistTextField.getText().strip();

        if (artist.isEmpty()) {
            alertRequired("artist", artistTextField);
            return;
        }

        var album = albumTextField.getText().strip();

        if (album.isEmpty()) {
            alertRequired("album", albumTextField);
            return;
        }

        var title = titleTextField.getText().strip();

        if (title.isEmpty()) {
            alertRequired("title", titleTextField);
            return;
        }

        var genre = genreSuggestionPicker.getText().strip();

        var year = map(yearTextField.getValue(), Number::intValue);

        var trackNumber = map(trackNumberTextField.getValue(), Number::intValue);
        var discNumber = map(discNumberTextField.getValue(), Number::intValue);

        var compilation = compilationCheckBox.isSelected();

        if (compilation && genre.isEmpty()) {
            alertRequired("genre", genreSuggestionPicker);
            return;
        }

        var song = new Song();

        song.setID(this.song.getID());

        song.setArtist(artist);
        song.setAlbum(album);
        song.setTitle(title);

        song.setTime(this.song.getTime());

        if (!genre.isEmpty()) {
            song.setGenre(genre);
        }

        song.setYear(year);

        song.setTrackNumber(trackNumber);
        song.setDiscNumber(discNumber);

        song.setCompilation(compilation);

        song.setType(this.song.getType());

        if (MusicLibrary.updateSong(song, this.song)) {
            var mainFrame = MainFrame.getInstance();

            mainFrame.loadArtists();
            mainFrame.loadGenres();
            mainFrame.loadPlaylists();

            dispose();
        } else {
            UIManager.getLookAndFeel().provideErrorFeedback(artistTextField);
        }
    }

    private void alertRequired(String key, JComponent component) {
        var message = String.format(resourceBundle.getString("requiredFieldFormat"),
            ResourceBundle.getBundle(getClass().getName()).getString(key));

        JOptionPane.showMessageDialog(this, message,
            resourceBundle.getString("error"),
            JOptionPane.ERROR_MESSAGE);

        component.requestFocus();
    }
}
