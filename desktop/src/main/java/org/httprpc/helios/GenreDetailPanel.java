// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.httprpc.sierra.BasicTableModel;
import org.httprpc.sierra.MenuButton;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.UILoader;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;
import java.awt.KeyboardFocusManager;
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
    private @Outlet JButton playButton = null;

    private @Outlet MenuButton addToPlaylistButton = null;
    private @Outlet JButton editSongButton = null;
    private @Outlet JButton deleteSongButton = null;

    private @Outlet JTable songTable = null;

    private static final FlatSVGIcon playlistIcon;
    static {
        playlistIcon = new FlatSVGIcon(GenreDetailPanel.class.getResource("icons/music_note_24dp.svg")).derive(18, 18);

        playlistIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground")));
    }

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(GenreDetailPanel.class.getName());

    public GenreDetailPanel(Genre genre, List<Song> songs) {
        add(UILoader.load(this, "GenreDetailPanel.xml", resourceBundle));

        nameLabel.setText(genre.getName());

        playButton.addActionListener(event -> {
            var i = songTable.getSelectedRow();

            MainFrame.getInstance().playAll(i == -1 ? songs : songs.subList(i, songs.size()));
        });

        addToPlaylistButton.setEnabled(false);

        editSongButton.addActionListener(event -> editSong());
        editSongButton.setEnabled(false);

        deleteSongButton.addActionListener(event -> deleteSong());
        deleteSongButton.setEnabled(false);

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

            if (songTable.getSelectedRow() != -1) {
                enableButtons();
            } else {
                disableButtons();
            }
        });

        setBorder(new EmptyBorder(8, 8, 8, 8));

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

        MusicLibrary.addToPlaylist(playlist, song);

        MainFrame.getInstance().loadPlaylists();
    }

    @SuppressWarnings("unchecked")
    private void editSong() {
        var song = ((BasicTableModel<Song>)songTable.getModel()).getRow(songTable.getSelectedRow());

        var mainFrame = MainFrame.getInstance();

        var editSongDialog = new EditSongDialog(mainFrame, song);

        editSongDialog.pack();
        editSongDialog.setLocationRelativeTo(mainFrame);

        editSongDialog.setVisible(true);
    }

    @SuppressWarnings("unchecked")
    private void deleteSong() {
        var song = ((BasicTableModel<Song>)songTable.getModel()).getRow(songTable.getSelectedRow());

        var option = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
            String.format(resourceBundle.getString("confirmDeleteMessageFormat"), song.getTitle()),
            resourceBundle.getString("deleteSong"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (option == JOptionPane.YES_OPTION) {
            var mainFrame = MainFrame.getInstance();

            var glassPane = mainFrame.getGlassPane();

            glassPane.setVisible(true);

            MainFrame.getTaskExecutor().execute(() -> {
                MusicLibrary.deleteSong(song);

                return null;
            }, (result, exception) -> {
                glassPane.setVisible(false);

                mainFrame.refresh();
            });
        }
    }

    private void enableButtons() {
        addToPlaylistButton.setEnabled(addToPlaylistButton.getComponentPopupMenu().getComponentCount() > 0);
        editSongButton.setEnabled(true);
        deleteSongButton.setEnabled(true);
    }

    private void disableButtons() {
        addToPlaylistButton.setEnabled(false);
        editSongButton.setEnabled(false);
        deleteSongButton.setEnabled(false);
    }

    @SuppressWarnings("unchecked")
    public void scrollToSong(Song song) {
        var songID = song.getID();

        var songTableModel = (BasicTableModel<Song>)songTable.getModel();

        var n = songTableModel.getRowCount();

        for (var i = 0; i < n; i++) {
            if (songID.equals(songTableModel.getRow(i).getID())) {
                songTable.getSelectionModel().setSelectionInterval(i, i);
                songTable.scrollRectToVisible(songTable.getCellRect(i, 1, true));

                songTable.requestFocus();

                break;
            }
        }
    }
}
