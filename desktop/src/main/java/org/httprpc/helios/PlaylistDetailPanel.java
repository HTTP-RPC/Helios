package org.httprpc.helios;

import org.httprpc.sierra.BasicTableModel;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.UILoader;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.util.List;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Optionals.*;

public class PlaylistDetailPanel extends StackPanel {
    private static class PlaylistCellRenderer extends JLabel implements TableCellRenderer {
        PlaylistCellRenderer() {
            setOpaque(true);

            setBorder(new EmptyBorder(2, 2, 2, 2));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
            Object value,
            boolean selected, boolean hasFocus,
            int row, int column) {
            setText(map(value, Object::toString));

            Color background;
            Color foreground;
            if (selected) {
                background = table.getSelectionBackground();
                foreground = table.getSelectionForeground();
            } else {
                background = table.getBackground();
                foreground = table.getForeground();
            }

            setBackground(background);
            setForeground(foreground);

            return this;
        }
    }

    private @Outlet JLabel nameLabel = null;
    private @Outlet JButton playAllButton = null;

    private @Outlet JButton removeFromPlaylistButton = null;
    private @Outlet JButton editPlaylistNameButton = null;
    private @Outlet JButton deletePlaylistButton = null;

    private @Outlet JTable songTable = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(PlaylistDetailPanel.class.getName());

    public PlaylistDetailPanel(Playlist playlist, List<Song> songs) {
        add(UILoader.load(this, "PlaylistDetailPanel.xml", resourceBundle));

        nameLabel.setText(playlist.getName());

        playAllButton.addActionListener(event -> MainFrame.getInstance().playAll(songs));

        removeFromPlaylistButton.addActionListener(event -> removeFromPlaylist());
        removeFromPlaylistButton.setEnabled(false);

        editPlaylistNameButton.addActionListener(event -> editPlaylistName());

        deletePlaylistButton.addActionListener(event -> deletePlaylist());

        var songTableHeader = songTable.getTableHeader();

        songTableHeader.setReorderingAllowed(false);
        songTableHeader.setResizingAllowed(false);

        songTable.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        songTable.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

        songTable.setModel(new BasicTableModel<>(Song.class, songs,
            listOf("title", "artist", "album"),
            resourceBundle));

        songTable.setDefaultRenderer(Object.class, new PlaylistCellRenderer());

        songTable.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }

            var i = songTable.getSelectionModel().getMinSelectionIndex();

            removeFromPlaylistButton.setEnabled(i != -1);
        });

        setBorder(new EmptyBorder(8, 8, 8, 8));

        setScrollableTracksViewportWidth(true);
        setScrollableTracksViewportHeight(true);
    }

    private void removeFromPlaylist() {
        // TODO Confirm remove
    }

    private void editPlaylistName() {
        // TODO
    }

    private void deletePlaylist() {
        // TODO Confirm delete
    }
}
