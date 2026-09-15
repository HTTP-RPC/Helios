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
import java.text.NumberFormat;
import java.util.List;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Optionals.*;

public class EditAlbumDialog extends AbstractDialog {
    private String name;
    private List<Song> songs;

    private String genre;
    private Integer year;

    private boolean compilation;

    private @Outlet JTextField nameTextField = null;
    private @Outlet SuggestionPicker genreSuggestionPicker = null;
    private @Outlet NumberField yearTextField = null;

    private @Outlet JCheckBox compilationCheckBox = null;

    private @Outlet JButton cancelButton = null;
    private @Outlet JButton okButton = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(EditAlbumDialog.class.getName());

    public EditAlbumDialog(MainFrame owner, String name, List<Song> songs,
        String genre, Integer year,
        boolean compilation) {
        super(owner);

        this.name = name;
        this.songs = songs;

        this.genre = genre;
        this.year = year;

        this.compilation = compilation;

        setTitle(resourceBundle.getString("windowTitle"));

        setContentPane(UILoader.load(this, "EditAlbumDialog.xml", resourceBundle));

        var integerFormat = NumberFormat.getIntegerInstance();

        integerFormat.setGroupingUsed(false);

        yearTextField.setFormat(integerFormat);

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
        nameTextField.setText(name);

        genreSuggestionPicker.setText(genre);
        genreSuggestionPicker.setSuggestions(MusicLibrary.getGenreSuggestions());

        yearTextField.setValue(year);

        compilationCheckBox.setSelected(compilation);
    }

    private void save() {
        var name = nameTextField.getText().strip();

        if (name.isEmpty()) {
            alertRequired("name", nameTextField);
            return;
        }

        var genre = genreSuggestionPicker.getText().strip();

        var year = map(yearTextField.getValue(), Number::intValue);

        var compilation = compilationCheckBox.isSelected();

        if (compilation && genre.isEmpty()) {
            alertRequired("genre", genreSuggestionPicker);
            return;
        }

        for (var previousSong : songs) {
            var song = new Song();

            song.setID(previousSong.getID());
            song.setArtist(previousSong.getArtist());
            song.setAlbum(name);
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
