package org.httprpc.helios;

import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.sql.QueryBuilder;
import org.httprpc.sierra.NumberField;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.SuggestionPicker;
import org.httprpc.sierra.UILoader;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
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
        genreSuggestionPicker.setSuggestions(getSuggestions());

        yearTextField.setValue(song.getYear());

        trackNumberTextField.setValue(song.getTrackNumber());
        trackCountTextField.setValue(song.getTrackCount());

        discNumberTextField.setValue(song.getDiscNumber());
        discCountTextField.setValue(song.getDiscCount());
    }

    private List<String> getSuggestions() {
        var queryBuilder = new QueryBuilder();

        queryBuilder.append("select distinct genre from Song");

        try (var connection = MainFrame.openConnection();
            var statement = queryBuilder.prepare(connection);
            var results = queryBuilder.executeQuery(statement)) {
            return listOf(mapAll(results, result -> (String)result.get("genre")));
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    private void save() {
        var artist = artistTextField.getText();

        if (artist.isEmpty()) {
            alertRequired("artist", artistTextField);
            return;
        }

        var album = albumTextField.getText();

        if (album.isEmpty()) {
            alertRequired("album", albumTextField);
            return;
        }

        var title = titleTextField.getText();

        if (title.isEmpty()) {
            alertRequired("title", titleTextField);
            return;
        }

        var genre = genreSuggestionPicker.getText();

        var year = map(yearTextField.getValue(), Number::intValue);

        var trackNumber = map(trackNumberTextField.getValue(), Number::intValue);
        var trackCount = map(trackCountTextField.getValue(), Number::intValue);

        var discNumber = map(discNumberTextField.getValue(), Number::intValue);
        var discCount = map(discCountTextField.getValue(), Number::intValue);

        var song = BeanAdapter.coerce(mapOf(), Song.class);

        song.setID(this.song.getID());

        song.setArtist(artist);
        song.setAlbum(album);
        song.setTitle(title);
        song.setTime(this.song.getTime());

        song.setGenre(genre);
        song.setYear(year);

        song.setTrackNumber(trackNumber);
        song.setTrackCount(trackCount);

        song.setDiscNumber(discNumber);
        song.setDiscCount(discCount);

        var queryBuilder = QueryBuilder.update(Song.class).filterByPrimaryKey("id");

        try (var connection = MainFrame.openConnection();
            var statement = queryBuilder.prepare(connection)) {
            queryBuilder.executeUpdate(statement, new BeanAdapter(song));
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }

        var mainFrame = MainFrame.getInstance();

        mainFrame.loadArtists();
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
