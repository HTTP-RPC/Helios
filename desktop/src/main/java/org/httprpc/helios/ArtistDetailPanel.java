package org.httprpc.helios;

import javax.swing.JLabel;
import java.util.ResourceBundle;

public class ArtistDetailPanel extends CollectionDetailPanel {
    private Artist artist;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(ArtistDetailPanel.class.getName());

    public ArtistDetailPanel(Artist artist) {
        this.artist = artist;
    }

    @Override
    public void load() {
        var artistLabel = new JLabel(artist.getName());

        artistLabel.putClientProperty("FlatLaf.styleClass", "h1");

        add(artistLabel);

        // TODO
    }
}
