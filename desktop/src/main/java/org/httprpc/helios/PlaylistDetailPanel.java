package org.httprpc.helios;

import org.httprpc.sierra.BasicTableModel;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.UILoader;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import java.awt.KeyboardFocusManager;
import java.util.List;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.*;

public class PlaylistDetailPanel extends StackPanel {
    private @Outlet JLabel nameLabel = null;
    private @Outlet JButton playAllButton = null;

    private @Outlet JTable songTable = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(PlaylistDetailPanel.class.getName());

    public PlaylistDetailPanel(Playlist playlist, List<Song> songs) {
        add(UILoader.load(this, "PlaylistDetailPanel.xml", resourceBundle));

        nameLabel.setText(playlist.getName());

        playAllButton.addActionListener(event -> MainFrame.getInstance().playAll(songs));

        var songTableHeader = songTable.getTableHeader();

        songTableHeader.setReorderingAllowed(false);
        songTableHeader.setResizingAllowed(false);

        songTable.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        songTable.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

        songTable.setModel(new BasicTableModel<>(Song.class, songs,
            listOf("title", "artist", "album"),
            resourceBundle));

        setBorder(new EmptyBorder(8, 8, 8, 8));

        setScrollableTracksViewportWidth(true);
        setScrollableTracksViewportHeight(true);
    }
}
