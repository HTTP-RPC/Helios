package org.httprpc.helios;

import org.httprpc.sierra.ColumnPanel;
import org.httprpc.sierra.ImagePane;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.RowPanel;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.UILoader;

import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.time.Duration;
import java.util.List;
import java.util.ResourceBundle;

public class AlbumDetailPanel extends StackPanel {
    private @Outlet JLabel nameLabel = null;

    private @Outlet ImagePane imagePane = null;
    private @Outlet ColumnPanel songListPanel = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(AlbumDetailPanel.class.getName());

    public AlbumDetailPanel(Artist artist, String name, List<Song> songs) {
        add(UILoader.load(this, "AlbumDetailPanel.xml"));

        nameLabel.setText(name);

        imagePane.setImage(MainFrame.getAlbumArtwork(artist, name));

        songListPanel.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")),
            new EmptyBorder(2, 0, 0, 0)
        ));

        for (var song : songs) {
            var songPanel = new RowPanel();

            songPanel.setAlignToBaseline(true);

            songPanel.add(new JLabel(song.getTitle()), 1.0);

            var duration = Duration.ofSeconds(song.getTime());

            songPanel.add(new JLabel(String.format(resourceBundle.getString("timeFormat"),
                duration.toMinutesPart(),
                duration.toSecondsPart())));

            songPanel.setBorder(new EmptyBorder(4, 0, 4, 0));

            songListPanel.add(songPanel);
        }
    }
}
