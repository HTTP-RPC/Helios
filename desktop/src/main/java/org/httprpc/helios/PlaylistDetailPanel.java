package org.httprpc.helios;

import javax.swing.JLabel;
import java.util.ResourceBundle;

public class PlaylistDetailPanel extends CollectionDetailPanel {
    private Playlist playlist;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(PlaylistDetailPanel.class.getName());

    public PlaylistDetailPanel(Playlist playlist) {
        this.playlist = playlist;
    }

    @Override
    public void load() {
        var playlistLabel = new JLabel(playlist.getName());

        playlistLabel.putClientProperty("FlatLaf.styleClass", "h1");

        add(playlistLabel);

        // TODO
    }
}
