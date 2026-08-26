package org.httprpc.helios;

import org.httprpc.sierra.ColumnPanel;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.util.ResourceBundle;

public class SongCellRenderer extends ColumnPanel implements ListCellRenderer<Song> {
    private JLabel titleLabel = new JLabel();
    private JLabel albumLabel = new JLabel();

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(SongCellRenderer.class.getName());

    public SongCellRenderer() {
        setOpaque(true);

        setBorder(new EmptyBorder(4, 4, 4, 4));

        add(titleLabel);
        add(albumLabel);

        albumLabel.putClientProperty("FlatLaf.styleClass", "small");
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Song> list,
        Song song, int index,
        boolean selected, boolean cellHasFocus) {
        titleLabel.setText(song.getTitle());
        albumLabel.setText(String.format(resourceBundle.getString("albumFormat"), song.getAlbum(), song.getArtist()));

        Color background;
        Color foreground;
        if (selected) {
            background = list.getSelectionBackground();
            foreground = list.getSelectionForeground();
        } else {
            background = list.getBackground();
            foreground = list.getForeground();
        }

        setBackground(background);

        titleLabel.setForeground(foreground);
        albumLabel.setForeground(foreground);

        return this;
    }
}
