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

public class PlaylistCellRenderer extends ColumnPanel implements ListCellRenderer<ExpandedPlaylist> {
    private JComponent component;

    private @Outlet JLabel nameLabel = null;
    private @Outlet JLabel countLabel = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(PlaylistCellRenderer.class.getName());

    public PlaylistCellRenderer() {
        component = UILoader.load(this, "PlaylistCellRenderer.xml");
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ExpandedPlaylist> list,
        ExpandedPlaylist value, int index,
        boolean selected, boolean cellHasFocus) {
        nameLabel.setText(value.getName());

        var artistCount = value.getArtistCount();
        var songCount = value.getSongCount();

        var countText = String.format(resourceBundle.getString("countFormat"),
            String.format(resourceBundle.getString(artistCount == 1 ? "singleArtist" : "multipleArtists"), artistCount),
            String.format(resourceBundle.getString(songCount == 1 ? "singleSong" : "multipleSongs"), songCount));

        countLabel.setText(countText);

        Color background;
        Color foreground;
        if (selected) {
            background = list.getSelectionBackground();
            foreground = list.getSelectionForeground();
        } else {
            background = list.getBackground();
            foreground = list.getForeground();
        }

        component.setBackground(background);

        nameLabel.setForeground(foreground);
        countLabel.setForeground(foreground);

        return component;
    }
}
