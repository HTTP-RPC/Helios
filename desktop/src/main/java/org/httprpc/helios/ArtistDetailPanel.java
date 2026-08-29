package org.httprpc.helios;

import org.httprpc.sierra.ColumnPanel;

import java.util.ResourceBundle;

public class ArtistDetailPanel extends ColumnPanel {
    private Artist artist;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(ArtistDetailPanel.class.getName());

    public ArtistDetailPanel(Artist artist) {
        this.artist = artist;
    }

    // TODO
}
