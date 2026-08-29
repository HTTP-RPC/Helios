package org.httprpc.helios;

import org.httprpc.sierra.ColumnPanel;

import java.util.ResourceBundle;

public class PlaylistDetailPanel extends ColumnPanel {
    private Playlist playlist;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(PlaylistDetailPanel.class.getName());

    public PlaylistDetailPanel(Playlist playlist) {
        this.playlist = playlist;
    }

    // TODO
}
