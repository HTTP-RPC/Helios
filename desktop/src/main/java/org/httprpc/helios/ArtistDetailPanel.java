package org.httprpc.helios;

import org.httprpc.sierra.ColumnPanel;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.UILoader;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Iterables.*;
import static org.httprpc.kilo.util.Optionals.*;

public class ArtistDetailPanel extends StackPanel {
    private @Outlet JLabel nameLabel = null;
    private @Outlet JButton playAllButton = null;

    private @Outlet ColumnPanel albumListPanel = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(ArtistDetailPanel.class.getName());

    public ArtistDetailPanel(Artist artist, Map<String, List<Song>> albums) {
        add(UILoader.load(this, "ArtistDetailPanel.xml", resourceBundle));

        nameLabel.setText(artist.getName());

        playAllButton.addActionListener(event -> MainFrame.getInstance().playAll(flatten(albums.entrySet(), Map.Entry::getValue)));

        for (var entry : albums.entrySet()) {
            var name = entry.getKey();
            var songs = entry.getValue();

            songs.sort(Comparator.comparing(song -> coalesce(song.getTrackNumber(), () -> 0)));

            var albumDetailPanel = new AlbumDetailPanel(artist, name, songs);

            albumListPanel.add(albumDetailPanel);
        }

        setBorder(new EmptyBorder(8, 8, 8, 8));

        setScrollableTracksViewportWidth(true);
    }

    public void scrollToSong(Song song) {
        var album = song.getAlbum();

        var n = albumListPanel.getComponentCount();

        for (var i = 0; i < n; i++) {
            var albumDetailPanel = (AlbumDetailPanel)albumListPanel.getComponent(i);

            if (albumDetailPanel.matches(album)) {
                scrollRectToVisible(SwingUtilities.convertRectangle(this, albumDetailPanel.getBounds(), this));

                break;
            }
        }
    }
}
