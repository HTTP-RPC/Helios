package org.httprpc.helios;

import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.SuggestionPicker;
import org.httprpc.sierra.UILoader;

import javax.swing.JButton;
import javax.swing.JTextField;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Optionals.*;

public class EditSongDialog extends ModalDialog {
    private Song song;

    private @Outlet JTextField artistTextField = null;
    private @Outlet JTextField albumTextField = null;
    private @Outlet JTextField titleTextField = null;

    private @Outlet SuggestionPicker genreSuggestionPicker = null;
    private @Outlet JTextField yearTextField = null;

    private @Outlet JTextField trackNumberTextField = null;
    private @Outlet JTextField trackCountTextField = null;

    private @Outlet JTextField discNumberTextField = null;
    private @Outlet JTextField discCountTextField = null;

    private @Outlet JButton cancelButton = null;
    private @Outlet JButton okButton = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(EditSongDialog.class.getName());

    public EditSongDialog(MainFrame owner, Song song) {
        super(owner);

        this.song = song;

        setTitle(resourceBundle.getString("windowTitle"));

        setContentPane(UILoader.load(this, "EditSongDialog.xml", resourceBundle));

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

        // TODO Genre suggestions

        yearTextField.setText(map(song.getYear(), String::valueOf));

        trackNumberTextField.setText(map(song.getTrackNumber(), String::valueOf));
        trackCountTextField.setText(map(song.getTrackCount(), String::valueOf));

        discNumberTextField.setText(map(song.getDiscNumber(), String::valueOf));
        discCountTextField.setText(map(song.getDiscCount(), String::valueOf));
    }

    private void save() {
        // TODO

        dispose();
    }
}
