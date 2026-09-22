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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.Duration;
import java.util.List;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Optionals.*;

public class SongDetailPanel extends StackPanel {
    private Song song;

    private @Outlet JLabel trackNumberLabel = null;
    private @Outlet JLabel nowPlayingLabel = null;

    private @Outlet JLabel titleLabel = null;

    private @Outlet JButton playSongButton = null;

    private @Outlet MenuButton addToPlaylistButton = null;
    private @Outlet JButton editSongButton = null;
    private @Outlet JButton deleteSongButton = null;

    private @Outlet JLabel timeLabel  = null;

    private List<ExpandedPlaylist> playlists = listOf();

    private static final FlatSVGIcon playlistIcon;
    static {
        playlistIcon = new FlatSVGIcon(SongDetailPanel.class.getResource("icons/queue_music_24dp.svg")).derive(18, 18);

        playlistIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground")));
    }

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(SongDetailPanel.class.getName());

    public SongDetailPanel(Song song) {
        this.song = song;

        var content = UILoader.load(this, "SongDetailPanel.xml", resourceBundle);

        add(content);

        trackNumberLabel.setText("000");
        trackNumberLabel.setPreferredSize(trackNumberLabel.getPreferredSize());

        trackNumberLabel.setText(map(song.getTrackNumber(), String::valueOf));

        titleLabel.setText(song.getTitle());

        titleLabel.setEnabled(!song.isCompilation());

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

        showCurrentSong(MainFrame.getInstance().getCurrentSong());

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
        var mainFrame = MainFrame.getInstance();

        var editSongDialog = new EditSongDialog(mainFrame, song);

        editSongDialog.pack();
        editSongDialog.setLocationRelativeTo(mainFrame);

        editSongDialog.setVisible(true);
    }

    private void deleteSong() {
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

                mainFrame.loadAll();
            });
        }
    }

    @Override
    public void paintComponent(Graphics graphics) {
        paintComponent((Graphics2D)graphics);
    }

    private void paintComponent(Graphics2D graphics) {
        if (isOpaque()) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            graphics.setColor(UIManager.getColor("Component.borderColor"));

            graphics.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
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

    public void showCurrentSong(Song song) {
        var current = song != null && song.getID().equals(this.song.getID());

        trackNumberLabel.setVisible(!current);
        nowPlayingLabel.setVisible(current);
    }
}
