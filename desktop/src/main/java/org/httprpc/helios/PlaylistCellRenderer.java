package org.httprpc.helios;

import org.httprpc.sierra.ColumnPanel;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.UILoader;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.text.ChoiceFormat;
import java.util.ResourceBundle;

public class PlaylistCellRenderer extends CollectionCellRenderer<ExpandedPlaylist> {
    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(PlaylistCellRenderer.class.getName());

    @Override
    protected String getName(ExpandedPlaylist value) {
        return value.getName();
    }

    @Override
    protected String getCountText(ExpandedPlaylist value) {
        var artistCount = value.getArtistCount();
        var songCount = value.getSongCount();

        return String.format(resourceBundle.getString("countFormat"),
            String.format(resourceBundle.getString(artistCount == 1 ? "singleArtist" : "multipleArtists"), artistCount),
            String.format(resourceBundle.getString(songCount == 1 ? "singleSong" : "multipleSongs"), songCount));
    }
}
