// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.httprpc.kilo.sql.QueryBuilder;
import org.httprpc.sierra.BasicTableModel;
import org.httprpc.sierra.MenuButton;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.UILoader;
import org.sqlite.SQLiteErrorCode;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Optionals.*;

public class GenreDetailPanel extends StackPanel {
    private static class GenreCellRenderer extends JLabel implements TableCellRenderer {
        GenreCellRenderer() {
            setText("A");
            setOpaque(true);

            setBorder(new EmptyBorder(4, 4, 4, 4));
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

    private @Outlet MenuButton addToPlaylistButton = null;
    private @Outlet JButton editSongButton = null;

    private @Outlet JTable songTable = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(GenreDetailPanel.class.getName());

    private static final FlatSVGIcon playlistIcon;
    static {
        playlistIcon = new FlatSVGIcon(GenreDetailPanel.class.getResource("icons/music_note_24dp.svg")).derive(18, 18);

        playlistIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground")));
    }

    public GenreDetailPanel(Genre genre, List<Song> songs) {
        add(UILoader.load(this, "GenreDetailPanel.xml", resourceBundle));

        nameLabel.setText(genre.getName());

        playAllButton.addActionListener(event -> MainFrame.getInstance().playAll(songs));

        addToPlaylistButton.setEnabled(false);

        editSongButton.addActionListener(event -> editSong());
        editSongButton.setEnabled(false);

        var songTableHeader = songTable.getTableHeader();

        songTableHeader.setReorderingAllowed(false);
        songTableHeader.setResizingAllowed(false);

        songTable.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        songTable.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

        songTable.setModel(new BasicTableModel<>(Song.class, songs,
            listOf("album", "title", "artist"),
            resourceBundle));

        var genreCellRenderer = new GenreCellRenderer();

        songTable.setDefaultRenderer(Object.class, genreCellRenderer);
        songTable.setRowHeight(genreCellRenderer.getPreferredSize().height);

        songTable.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }

            if (songTable.getSelectionModel().getMinSelectionIndex() != -1) {
                enableButtons();
            } else {
                disableButtons();
            }
        });

        setBorder(new EmptyBorder(8, 8, 8, 8));

        setScrollableTracksViewportWidth(true);
        setScrollableTracksViewportHeight(true);

        SwingUtilities.invokeLater(() -> {
            var playlists = MainFrame.getInstance().getPlaylists();

            if (!playlists.isEmpty()) {
                for (var playlist : playlists) {
                    var menuItem = new JMenuItem(playlist.getName(), playlistIcon);

                    menuItem.addActionListener(event -> addToPlaylist(playlist));

                    addToPlaylistButton.add(menuItem);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void addToPlaylist(Playlist playlist) {
        var song = ((BasicTableModel<Song>)songTable.getModel()).getRow(songTable.getSelectedRow());

        var queryBuilder = QueryBuilder.insert(PlaylistSong.class);

        try (var connection = MainFrame.openConnection();
            var statement = queryBuilder.prepare(connection)) {
            queryBuilder.executeUpdate(statement, mapOf(
                entry("playlistID", playlist.getID()),
                entry("songID", song.getID())
            ));
        } catch (SQLException exception) {
            if (SQLiteErrorCode.getErrorCode(exception.getErrorCode()) != SQLiteErrorCode.SQLITE_CONSTRAINT) {
                throw new RuntimeException(exception);
            }
        }

        MainFrame.getInstance().loadPlaylists();
    }

    @SuppressWarnings("unchecked")
    private void editSong() {
        var song = ((BasicTableModel<Song>)songTable.getModel()).getRow(songTable.getSelectedRow());

        var editSongDialog = new EditSongDialog(MainFrame.getInstance(), song);

        editSongDialog.pack();
        editSongDialog.setLocationRelativeTo(editSongDialog.getOwner());

        editSongDialog.setVisible(true);
    }

    private void enableButtons() {
        addToPlaylistButton.setEnabled(addToPlaylistButton.getComponentPopupMenu().getComponentCount() > 0);
        editSongButton.setEnabled(true);
    }

    private void disableButtons() {
        addToPlaylistButton.setEnabled(false);
        editSongButton.setEnabled(false);
    }
}
