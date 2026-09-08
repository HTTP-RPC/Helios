// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.sql.QueryBuilder;
import org.httprpc.sierra.NumberField;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.SuggestionPicker;
import org.httprpc.sierra.UILoader;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.sqlite.SQLiteErrorCode;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;
import static org.httprpc.kilo.util.Optionals.*;

public class EditSongDialog extends ModalDialog {
    private Song song;

    private @Outlet JTextField artistTextField = null;
    private @Outlet JTextField albumTextField = null;
    private @Outlet JTextField titleTextField = null;

    private @Outlet SuggestionPicker genreSuggestionPicker = null;
    private @Outlet NumberField yearTextField = null;

    private @Outlet NumberField trackNumberTextField = null;
    private @Outlet NumberField trackCountTextField = null;

    private @Outlet NumberField discNumberTextField = null;
    private @Outlet NumberField discCountTextField = null;

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
        trackCountTextField.setFormat(integerFormat);

        discNumberTextField.setFormat(integerFormat);
        discCountTextField.setFormat(integerFormat);

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
        genreSuggestionPicker.setSuggestions(getGenreSuggestions());

        yearTextField.setValue(song.getYear());

        trackNumberTextField.setValue(song.getTrackNumber());
        trackCountTextField.setValue(song.getTrackCount());

        discNumberTextField.setValue(song.getDiscNumber());
        discCountTextField.setValue(song.getDiscCount());

        compilationCheckBox.setSelected(song.isCompilation());
    }

    private List<String> getGenreSuggestions() {
        var queryBuilder = new QueryBuilder();

        queryBuilder.append("select distinct genre from Song where genre is not null");

        try (var connection = MainFrame.openConnection();
            var statement = queryBuilder.prepare(connection);
            var results = queryBuilder.executeQuery(statement)) {
            return listOf(mapAll(results, result -> (String)result.get("genre")));
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
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
        var trackCount = map(trackCountTextField.getValue(), Number::intValue);

        var discNumber = map(discNumberTextField.getValue(), Number::intValue);
        var discCount = map(discCountTextField.getValue(), Number::intValue);

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
        song.setTrackCount(trackCount);

        song.setDiscNumber(discNumber);
        song.setDiscCount(discCount);

        song.setCompilation(compilation);

        song.setType(this.song.getType());

        var queryBuilder = QueryBuilder.update(Song.class).filterByPrimaryKey("id");

        try (var connection = MainFrame.openConnection();
            var statement = queryBuilder.prepare(connection)) {
            queryBuilder.executeUpdate(statement, new BeanAdapter(song));
        } catch (SQLException exception) {
            if (SQLiteErrorCode.getErrorCode(exception.getErrorCode()) == SQLiteErrorCode.SQLITE_CONSTRAINT) {
                UIManager.getLookAndFeel().provideErrorFeedback(null);

                return;
            }

            throw new RuntimeException(exception);
        }

        var contentPath = MainFrame.getContentPath(song);
        var previousContentPath = MainFrame.getContentPath(this.song);

        if (!contentPath.equals(previousContentPath)) {
            try {
                var albumContentPath = contentPath.getParent();

                Files.createDirectories(albumContentPath);

                var temporaryContentPath = albumContentPath.resolve(String.format("%s.tmp", MainFrame.escape(song.getTitle())));

                Files.copy(previousContentPath, temporaryContentPath, StandardCopyOption.REPLACE_EXISTING);

                MainFrame.deleteSong(previousContentPath);

                Files.move(temporaryContentPath, contentPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }

        AudioFile audioFile;
        try {
            try {
                audioFile = AudioFileIO.read(contentPath.toFile());
            } catch (CannotReadException | TagException | InvalidAudioFrameException | ReadOnlyFileException exception) {
                throw new IOException(exception);
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        var tag = audioFile.getTag();

        try {
            tag.setField(FieldKey.ARTIST, artist);
            tag.setField(FieldKey.ALBUM, album);
            tag.setField(FieldKey.TITLE, title);

            if (!genre.isEmpty()) {
                tag.setField(FieldKey.GENRE, genre);
            } else {
                tag.deleteField(FieldKey.GENRE);
            }

            if (year != null) {
                tag.setField(FieldKey.YEAR, map(year, Object::toString));
            } else {
                tag.deleteField(FieldKey.YEAR);
            }

            if (trackNumber != null) {
                tag.setField(FieldKey.TRACK, map(trackNumber, Object::toString));
            } else {
                tag.deleteField(FieldKey.TRACK);
            }

            if (trackCount != null) {
                tag.setField(FieldKey.TRACK_TOTAL, map(trackCount, Object::toString));
            } else {
                tag.deleteField(FieldKey.TRACK_TOTAL);
            }

            if (discNumber != null) {
                tag.setField(FieldKey.DISC_NO, map(discNumber, Object::toString));
            } else {
                tag.deleteField(FieldKey.DISC_NO);
            }

            if (discCount != null) {
                tag.setField(FieldKey.DISC_TOTAL, map(discCount, Object::toString));
            } else {
                tag.deleteField(FieldKey.DISC_TOTAL);
            }

            tag.setField(FieldKey.IS_COMPILATION, String.valueOf(compilation ? 1 : 0));
        } catch (FieldDataInvalidException exception) {
            throw new RuntimeException(exception);
        }

        try {
            AudioFileIO.write(audioFile);
        } catch (CannotWriteException exception) {
            throw new RuntimeException(exception);
        }

        var mainFrame = MainFrame.getInstance();

        mainFrame.loadArtists();
        mainFrame.loadGenres();
        mainFrame.loadPlaylists();

        dispose();
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
