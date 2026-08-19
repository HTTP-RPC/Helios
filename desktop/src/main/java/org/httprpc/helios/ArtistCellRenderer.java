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

        var countText = String.format(resourceBundle.getString("countFormat"),
            value.getAlbumCount(),
            value.getSongCount());

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
