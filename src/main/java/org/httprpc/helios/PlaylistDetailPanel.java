/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.httprpc.helios;

import org.httprpc.sierra.BasicTableModel;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.UILoader;

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
import java.awt.Container;
import java.awt.FocusTraversalPolicy;
import java.awt.KeyboardFocusManager;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;
import static org.httprpc.kilo.util.Optionals.*;

public class PlaylistDetailPanel extends StackPanel {
    private static class PlaylistCellRenderer extends JLabel implements TableCellRenderer {
        PlaylistCellRenderer() {
            setText("A");
            setOpaque(true);

            setBorder(new EmptyBorder(4, 8, 4, 8));
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
    private @Outlet JButton playButton = null;

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

        playButton.addActionListener(event -> {
            var selectedRows = songTable.getSelectedRows();

            if (selectedRows.length == 0) {
                MainFrame.getInstance().playAll(songs);
            } else if (selectedRows.length == 1) {
                MainFrame.getInstance().playAll(songs.subList(selectedRows[0], songs.size()));
            } else {
                MainFrame.getInstance().playAll(listOf(mapAll(iterableOf(selectedRows), songs::get)));
            }
        });

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
            listOf("artist", "title", "album"),
            resourceBundle));

        var playlistCellRenderer = new PlaylistCellRenderer();

        songTable.setDefaultRenderer(Object.class, playlistCellRenderer);
        songTable.setRowHeight(playlistCellRenderer.getPreferredSize().height);

        songTable.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }

            removeFromPlaylistButton.setEnabled(songTable.getSelectedRow() != -1);
        });

        setScrollableTracksViewportWidth(true);
        setScrollableTracksViewportHeight(true);

        setFocusCycleRoot(true);
        setFocusTraversalPolicyProvider(true);

        setFocusTraversalPolicy(new FocusTraversalPolicy() {
            @Override
            public Component getComponentAfter(Container container, Component component) {
                return null;
            }

            @Override
            public Component getComponentBefore(Container container, Component component) {
                return null;
            }

            @Override
            public Component getFirstComponent(Container container) {
                return null;
            }

            @Override
            public Component getLastComponent(Container container) {
                return null;
            }

            @Override
            public Component getDefaultComponent(Container container) {
                return null;
            }
        });

        if (playlist.getID() == null) {
            SwingUtilities.invokeLater(this::editPlaylistName);
        }
    }

    private void removeFromPlaylist() {
        var option = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
            resourceBundle.getString("confirmRemoveSongsMessage"),
            resourceBundle.getString("removeFromPlaylist"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (option == JOptionPane.YES_OPTION) {
            MusicLibrary.removeFromPlaylist(playlist, listOf(mapAll(iterableOf(songTable.getSelectedRows()), songs::get)));

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
            playlist.setName(name);

            if ((playlist.getID() == null) ? MusicLibrary.addPlaylist(playlist) : MusicLibrary.updatePlaylist(playlist)) {
                MainFrame.getInstance().loadPlaylists();

                endPlaylistNameEdit();
            } else {
                nameTextField.selectAll();

                UIManager.getLookAndFeel().provideErrorFeedback(nameTextField);
            }
        }
    }

    private void endPlaylistNameEdit() {
        nameTextField.setFocusable(false);
        nameTextField.setEditable(false);
    }

    private void deletePlaylist() {
        var option = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
            resourceBundle.getString("confirmDeleteMessage"),
            resourceBundle.getString("deletePlaylist"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (option == JOptionPane.YES_OPTION) {
            MusicLibrary.deletePlaylist(playlist);

            MainFrame.getInstance().loadPlaylists();
        }
    }
}
