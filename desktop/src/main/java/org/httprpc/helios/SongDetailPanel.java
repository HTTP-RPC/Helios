// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.httprpc.sierra.MenuButton;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.UILoader;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.util.List;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.*;

public class SongDetailPanel extends StackPanel {
    private Song song;

    private @Outlet JLabel titleLabel = null;

    private @Outlet JButton playSongButton = null;

    private @Outlet MenuButton addToPlaylistButton = null;
    private @Outlet JButton editSongButton = null;
    private @Outlet JButton deleteSongButton = null;

    private @Outlet JLabel timeLabel  = null;

    private List<ExpandedPlaylist> playlists = listOf();

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(SongDetailPanel.class.getName());

    private static final FlatSVGIcon playlistIcon;
    static {
        playlistIcon = new FlatSVGIcon(SongDetailPanel.class.getResource("icons/music_note_24dp.svg")).derive(18, 18);

        playlistIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground")));
    }

    public SongDetailPanel(Song song) {
        this.song = song;

        var content = UILoader.load(this, "SongDetailPanel.xml", resourceBundle);

        add(content);

        titleLabel.setText(song.getTitle());

        playSongButton.addActionListener(event -> playSong());
        playSongButton.setVisible(false);

        addToPlaylistButton.setVisible(false);

        editSongButton.addActionListener(event -> editSong());
        editSongButton.setVisible(false);

        deleteSongButton.addActionListener(event -> deleteSong());
        deleteSongButton.setVisible(false);

        timeLabel.setText(String.format(resourceBundle.getString("timeFormat"), 60, 0));
        timeLabel.setPreferredSize(timeLabel.getPreferredSize());

        var duration = Duration.ofSeconds(song.getTime());

        timeLabel.setText(String.format(resourceBundle.getString("timeFormat"),
            duration.toMinutesPart(),
            duration.toSecondsPart()));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                showButtons();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                var rectangle = new Rectangle(0, 0, getWidth(), getHeight());

                if (!rectangle.contains(event.getPoint())) {
                    hideButtons();
                }
            }
        });

        playSongButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                hideButtons();
            }
        });

        addToPlaylistButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                if (!addToPlaylistButton.getComponentPopupMenu().isVisible()) {
                    hideButtons();
                }
            }
        });

        addToPlaylistButton.getComponentPopupMenu().addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent event) {
                // No-op
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent event) {
                hideButtons();
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent event) {
                // No-op
            }
        });

        editSongButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                hideButtons();
            }
        });

        deleteSongButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                hideButtons();
            }
        });

        if (song.getTrackNumber() == null) {
            titleLabel.setEnabled(false);
            timeLabel.setEnabled(false);
        }

        setBackground(UIManager.getColor("Component.borderColor"));

        SwingUtilities.invokeLater(() -> playlists = MainFrame.getInstance().getPlaylists());
    }

    private void playSong() {
        MainFrame.getInstance().playAll(listOf(song));
    }

    private void addToPlaylist(Playlist playlist) {
        MusicLibrary.addToPlaylist(playlist, song);

        MainFrame.getInstance().loadPlaylists();
    }

    private void editSong() {
        var editSongDialog = new EditSongDialog(MainFrame.getInstance(), song);

        editSongDialog.pack();
        editSongDialog.setLocationRelativeTo(editSongDialog.getOwner());

        editSongDialog.setVisible(true);
    }

    private void deleteSong() {
        var result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
            String.format(resourceBundle.getString("confirmDeleteMessageFormat"), song.getTitle()),
            resourceBundle.getString("deleteSong"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            MusicLibrary.deleteSong(song);

            var mainFrame = MainFrame.getInstance();

            mainFrame.loadArtists();
            mainFrame.loadGenres();
            mainFrame.loadPlaylists();
        }
    }

    private void showButtons() {
        setOpaque(true);

        playSongButton.setVisible(true);

        if (!playlists.isEmpty()) {
            for (var playlist : playlists) {
                var menuItem = new JMenuItem(playlist.getName(), playlistIcon);

                menuItem.addActionListener(event -> addToPlaylist(playlist));

                addToPlaylistButton.add(menuItem);
            }

            addToPlaylistButton.setEnabled(true);
        } else {
            addToPlaylistButton.setEnabled(false);
        }

        addToPlaylistButton.setVisible(true);

        editSongButton.setVisible(true);
        deleteSongButton.setVisible(true);
    }

    private void hideButtons() {
        setOpaque(false);

        playSongButton.setVisible(false);

        addToPlaylistButton.removeAll();

        addToPlaylistButton.setVisible(false);

        editSongButton.setVisible(false);
        deleteSongButton.setVisible(false);
    }
}
