// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.sql.QueryBuilder;
import org.httprpc.sierra.BasicTableModel;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.UILoader;
import org.sqlite.SQLiteErrorCode;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Optionals.*;

public class PlaylistDetailPanel extends StackPanel {
    private static class PlaylistCellRenderer extends JLabel implements TableCellRenderer {
        PlaylistCellRenderer() {
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

    private Playlist playlist;
    private List<Song> songs;

    private @Outlet JTextField nameTextField = null;
    private @Outlet JButton playAllButton = null;

    private @Outlet JButton removeFromPlaylistButton = null;
    private @Outlet JButton editNameButton = null;
    private @Outlet JButton deletePlaylistButton = null;

    private @Outlet JTable songTable = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(PlaylistDetailPanel.class.getName());

    public PlaylistDetailPanel(Playlist playlist, List<Song> songs) {
        this.playlist = playlist;
        this.songs = songs;

        add(UILoader.load(this, "PlaylistDetailPanel.xml", resourceBundle));

        nameTextField.setText(playlist.getName());
        nameTextField.setBorder(null);

        nameTextField.addActionListener(event -> updatePlaylistName());

        nameTextField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent event) {
                nameTextField.getParent().revalidate();
            }

            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    revertPlaylistNameChange();
                }
            }

            @Override
            public void keyReleased(KeyEvent event) {
                // No-op
            }
        });

        nameTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                revertPlaylistNameChange();
            }
        });

        playAllButton.addActionListener(event -> MainFrame.getInstance().playAll(songs));

        removeFromPlaylistButton.addActionListener(event -> removeFromPlaylist());
        removeFromPlaylistButton.setEnabled(false);

        var playlistID = playlist.getID();

        editNameButton.addActionListener(event -> editPlaylistName());
        editNameButton.setEnabled(playlistID != null);

        deletePlaylistButton.addActionListener(event -> deletePlaylist());
        deletePlaylistButton.setEnabled(playlistID != null);

        var songTableHeader = songTable.getTableHeader();

        songTableHeader.setReorderingAllowed(false);
        songTableHeader.setResizingAllowed(false);

        songTable.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        songTable.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

        songTable.setModel(new BasicTableModel<>(Song.class, songs,
            listOf("title", "artist", "album"),
            resourceBundle));

        var playlistCellRenderer = new PlaylistCellRenderer();

        songTable.setDefaultRenderer(Object.class, playlistCellRenderer);
        songTable.setRowHeight(playlistCellRenderer.getPreferredSize().height);

        songTable.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }

            removeFromPlaylistButton.setEnabled(songTable.getSelectionModel().getMinSelectionIndex() != -1);
        });

        setBorder(new EmptyBorder(8, 8, 8, 8));

        setScrollableTracksViewportWidth(true);
        setScrollableTracksViewportHeight(true);

        if (playlist.getID() == null) {
            SwingUtilities.invokeLater(this::editPlaylistName);
        }
    }

    private void removeFromPlaylist() {
        var result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
            resourceBundle.getString("confirmRemoveSongsMessage"),
            resourceBundle.getString("removeFromPlaylist"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            var queryBuilder = QueryBuilder.delete(PlaylistSong.class)
                .filterByForeignKey(Playlist.class, "playlistID")
                .filterByForeignKey(Song.class, "songID");

            try (var connection = MainFrame.openConnection();
                var statement = queryBuilder.prepare(connection)) {
                var playlistID = playlist.getID();

                var selectedRows = songTable.getSelectedRows();

                for (var i = 0; i < selectedRows.length; i++) {
                    var song = songs.get(selectedRows[i]);

                    queryBuilder.addBatch(statement, mapOf(
                        entry("playlistID", playlistID),
                        entry("songID", song.getID())
                    ));
                }

                statement.executeBatch();
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }

            MainFrame.getInstance().loadPlaylists();
        }
    }

    private void editPlaylistName() {
        nameTextField.setFocusable(true);
        nameTextField.setEditable(true);

        nameTextField.selectAll();

        nameTextField.requestFocus();
    }

    private void revertPlaylistNameChange() {
        if (playlist.getID() == null) {
            MainFrame.getInstance().loadPlaylists();
        } else {
            nameTextField.setText(playlist.getName());

            nameTextField.getParent().revalidate();
        }

        endPlaylistNameEdit();
    }

    private void updatePlaylistName() {
        var name = nameTextField.getText().strip();

        if (name.isEmpty()) {
            revertPlaylistNameChange();
        } else {
            QueryBuilder queryBuilder;
            if (playlist.getID() == null) {
                queryBuilder = QueryBuilder.insert(Playlist.class);
            } else {
                queryBuilder = QueryBuilder.update(Playlist.class).filterByPrimaryKey("id");
            }

            playlist.setName(name);

            try (var connection = MainFrame.openConnection();
                var statement = queryBuilder.prepare(connection)) {
                queryBuilder.executeUpdate(statement, new BeanAdapter(playlist));

                MainFrame.getInstance().loadPlaylists();

                endPlaylistNameEdit();
            } catch (SQLException exception) {
                if (SQLiteErrorCode.getErrorCode(exception.getErrorCode()) == SQLiteErrorCode.SQLITE_CONSTRAINT) {
                    nameTextField.selectAll();

                    UIManager.getLookAndFeel().provideErrorFeedback(nameTextField);

                    return;
                }

                throw new RuntimeException(exception);
            }
        }
    }

    private void endPlaylistNameEdit() {
        nameTextField.setFocusable(false);
        nameTextField.setEditable(false);
    }

    private void deletePlaylist() {
        var result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
            resourceBundle.getString("confirmDeleteMessage"),
            resourceBundle.getString("deletePlaylist"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            var queryBuilder = QueryBuilder.delete(Playlist.class).filterByPrimaryKey("id");

            try (var connection = MainFrame.openConnection();
                var statement = queryBuilder.prepare(connection)) {
                queryBuilder.executeUpdate(statement, mapOf(
                    entry("id", playlist.getID())
                ));
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }

            MainFrame.getInstance().loadPlaylists();
        }
    }
}
