package org.httprpc.helios;

import org.httprpc.sierra.ColumnPanel;
import org.httprpc.sierra.ImagePane;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.RowPanel;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.UILoader;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.util.List;
import java.util.ResourceBundle;

public class AlbumDetailPanel extends StackPanel {
    private String name;

    private @Outlet RowPanel albumNamePanel = null;

    private @Outlet JLabel nameLabel = null;
    private @Outlet JButton playAlbumButton = null;

    private @Outlet ImagePane imagePane = null;
    private @Outlet ColumnPanel songListPanel = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(AlbumDetailPanel.class.getName());

    public AlbumDetailPanel(Artist artist, String name, List<Song> songs) {
        this.name = name;

        add(UILoader.load(this, "AlbumDetailPanel.xml", resourceBundle));

        nameLabel.setText(name);

        imagePane.setImage(MainFrame.getAlbumArtwork(artist, name));

        songListPanel.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")),
            new EmptyBorder(2, 0, 0, 0)
        ));

        for (var song : songs) {
            songListPanel.add(new SongDetailPanel(song));
        }
    }

    public boolean matches(String name) {
        return this.name.equals(name);
    }
}
