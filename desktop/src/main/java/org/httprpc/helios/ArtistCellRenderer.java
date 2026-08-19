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

public class ArtistCellRenderer extends ColumnPanel implements ListCellRenderer<ExpandedArtist> {
    private JComponent component;

    private @Outlet JLabel nameLabel = null;
    private @Outlet JLabel countLabel = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(ArtistCellRenderer.class.getName());

    public ArtistCellRenderer() {
        component = UILoader.load(this, "ArtistCellRenderer.xml");
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ExpandedArtist> list,
        ExpandedArtist value, int index,
        boolean selected, boolean cellHasFocus) {
        nameLabel.setText(value.getName());

        var albumCount = value.getAlbumCount();
        var songCount = value.getSongCount();

        var countText = String.format(resourceBundle.getString("countFormat"),
            String.format(resourceBundle.getString(albumCount == 1 ? "singleAlbum" : "multipleAlbums"), albumCount),
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
