// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.httprpc.kilo.sql.QueryBuilder;
import org.httprpc.sierra.MenuButton;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.UILoader;
import org.sqlite.SQLiteErrorCode;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.Duration;
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

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(SongDetailPanel.class.getName());

    public SongDetailPanel(Song song) {
        this.song = song;

        var content = UILoader.load(this, "SongDetailPanel.xml", resourceBundle);

        add(content);

        titleLabel.setText(song.getTitle());

        playSongButton.addActionListener(event -> playSong());
        playSongButton.setVisible(false);

        addToPlaylistButton.setVisible(false);

        var playlists = MainFrame.getInstance().getPlaylists();

        if (!playlists.isEmpty()) {
            var popupMenu = addToPlaylistButton.getComponentPopupMenu();

            popupMenu.addPopupMenuListener(new PopupMenuListener() {
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

            var playlistIcon = new FlatSVGIcon(SongDetailPanel.class.getResource("icons/music_note_24dp.svg")).derive(18, 18);

            playlistIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground")));

            for (var playlist : playlists) {
                var menuItem = new JMenuItem(playlist.getName(), playlistIcon);

                menuItem.addActionListener(event -> addToPlaylist(playlist));

                addToPlaylistButton.add(menuItem);
            }
        } else {
            addToPlaylistButton.setEnabled(false);
        }

        editSongButton.addActionListener(event -> editSong());
        editSongButton.setVisible(false);

        deleteSongButton.addActionListener(event -> deleteSong());
        deleteSongButton.setVisible(false);

        var duration = Duration.ofSeconds(song.getTime());

        timeLabel.setText(String.format(resourceBundle.getString("timeFormat"),
            duration.toMinutesPart(),
            duration.toSecondsPart()));

        content.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                showButtons();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                if (content.getComponentAt(event.getX(), event.getY()) == null) {
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

        setBackground(UIManager.getColor("Component.borderColor"));
    }

    private void playSong() {
        MainFrame.getInstance().playAll(listOf(song));
    }

    private void addToPlaylist(Playlist playlist) {
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
            var queryBuilder = QueryBuilder.delete(Song.class).filterByPrimaryKey("id");

            try (var connection = MainFrame.openConnection();
                var statement = queryBuilder.prepare(connection)) {
                queryBuilder.executeUpdate(statement, mapOf(
                    entry("id", song.getID())
                ));
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }

            MainFrame.deleteSong(MainFrame.getContentPath(song));

            var mainFrame = MainFrame.getInstance();

            mainFrame.loadArtists();
            mainFrame.loadPlaylists();
        }
    }

    private void showButtons() {
        setOpaque(true);

        playSongButton.setVisible(true);

        addToPlaylistButton.setVisible(true);
        editSongButton.setVisible(true);
        deleteSongButton.setVisible(true);
    }

    private void hideButtons() {
        setOpaque(false);

        playSongButton.setVisible(false);

        addToPlaylistButton.setVisible(false);
        editSongButton.setVisible(false);
        deleteSongButton.setVisible(false);
    }
}
