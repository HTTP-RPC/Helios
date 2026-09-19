// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.httprpc.sierra.BasicListModel;
import org.httprpc.sierra.ColumnPanel;
import org.httprpc.sierra.ImagePane;
import org.httprpc.sierra.MenuButton;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.RowPanel;
import org.httprpc.sierra.Spacer;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.UILoader;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Optionals.*;

public class GenreDetailPanel extends StackPanel {
    private static class ArtistAlbumCellRenderer extends ColumnPanel implements ListCellRenderer<ArtistAlbum> {
        ImagePane artworkImagePane = new ImagePane();

        JLabel albumLabel = new JLabel();
        JLabel artistLabel = new JLabel();

        ArtistAlbumCellRenderer() {
            setOpaque(true);

            setBorder(new EmptyBorder(4, 8, 4, 8));

            var artworkPanel = new RowPanel();

            artworkImagePane.setPreferredSize(new Dimension(90, 90));
            artworkImagePane.setScaleMode(ImagePane.ScaleMode.FILL_WIDTH);
            artworkImagePane.setBorder(UILoader.createRoundedLineBorder(UIManager.getColor("Component.borderColor"),
                new BasicStroke(1,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND), 4));

            artworkPanel.add(artworkImagePane);

            artworkPanel.add(new Spacer(), 1.0);

            add(artworkPanel);

            add(new Spacer(4));

            add(albumLabel);
            add(artistLabel);

            artistLabel.putClientProperty("FlatLaf.styleClass", "small");
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ArtistAlbum> list,
            ArtistAlbum artistAlbum, int index,
            boolean selected, boolean cellHasFocus) {
            var artist = artistAlbum.getArtist();
            var album = artistAlbum.getAlbum();

            artworkImagePane.setImage(artistAlbum.getArtwork());

            if (artworkImagePane.getImage() == null) {
                MainFrame.getTaskExecutor().execute(() -> {
                    try (var inputStream = Files.newInputStream(MusicLibrary.getArtworkPath(artist, album))) {
                        return ImageIO.read(inputStream);
                    } catch (IOException exception) {
                        return null;
                    }
                }, (artwork, exception) -> {
                    artistAlbum.setArtwork(artwork);

                    list.repaint();
                });
            }

            albumLabel.setText(album);
            artistLabel.setText(artist.isEmpty() ? resourceBundle.getString("compilation") : artist);

            Color background;
            Color foreground;
            if (selected) {
                background = list.getSelectionBackground();
                foreground = list.getSelectionForeground();
            } else {
                background = list.getBackground();
                foreground = list.getForeground();
            }

            setBackground(background);

            albumLabel.setForeground(foreground);
            artistLabel.setEnabled(selected);

            return this;
        }
    }

    private static class SongCellRenderer extends ColumnPanel implements ListCellRenderer<Song> {
        JLabel titleLabel = new JLabel();
        JLabel artistLabel = new JLabel();

        SongCellRenderer() {
            setOpaque(true);

            setBorder(new EmptyBorder(4, 8, 4, 8));

            add(titleLabel);
            add(artistLabel);

            artistLabel.putClientProperty("FlatLaf.styleClass", "small");
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Song> list,
            Song song, int index,
            boolean selected, boolean cellHasFocus) {
            titleLabel.setText(song.getTitle());
            artistLabel.setText(song.getArtist());

            Color background;
            Color foreground;
            if (selected) {
                background = list.getSelectionBackground();
                foreground = list.getSelectionForeground();
            } else {
                background = list.getBackground();
                foreground = list.getForeground();
            }

            setBackground(background);

            titleLabel.setForeground(foreground);
            artistLabel.setForeground(foreground);

            artistLabel.setEnabled(selected);

            return this;
        }
    }

    private @Outlet JLabel nameLabel = null;
    private @Outlet JButton playButton = null;

    private @Outlet MenuButton addToPlaylistButton = null;

    private @Outlet MenuButton editButton = null;
    private @Outlet JMenuItem editAlbumMenuItem = null;
    private @Outlet JMenuItem editArtworkMenuItem = null;
    private @Outlet JMenuItem editSongMenuItem = null;

    private @Outlet MenuButton deleteButton = null;
    private @Outlet JMenuItem deleteAlbumMenuItem = null;
    private @Outlet JMenuItem deleteArtworkMenuItem = null;
    private @Outlet JMenuItem deleteSongMenuItem = null;

    private @Outlet JList<ArtistAlbum> artistAlbumList = null;
    private @Outlet JList<Song> songList = null;

    private static final FlatSVGIcon playlistIcon;
    static {
        playlistIcon = new FlatSVGIcon(GenreDetailPanel.class.getResource("icons/queue_music_24dp.svg")).derive(18, 18);

        playlistIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground")));
    }

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(GenreDetailPanel.class.getName());

    private static Comparator<ArtistAlbum> artistAlbumComparator = Comparator.comparing(ArtistAlbum::getSortableArtist)
        .thenComparing(ArtistAlbum::getSortableAlbum);

    public GenreDetailPanel(Genre genre, Map<String, List<Song>> albums) {
        add(UILoader.load(this, "GenreDetailPanel.xml", resourceBundle));

        nameLabel.setText(genre.getName());

        playButton.addActionListener(event -> {
            // TODO
        });

        addToPlaylistButton.setEnabled(false);

        editAlbumMenuItem.addActionListener(event -> editAlbum());
        editArtworkMenuItem.addActionListener(event -> editArtwork());
        editSongMenuItem.addActionListener(event -> editSong());

        deleteAlbumMenuItem.addActionListener(event -> deleteAlbum());
        deleteArtworkMenuItem.addActionListener(event -> deleteArtwork());
        deleteSongMenuItem.addActionListener(event -> deleteSong());

        artistAlbumList.setCellRenderer(new ArtistAlbumCellRenderer());

        var artistAlbums = new ArrayList<ArtistAlbum>();

        for (var entry : albums.entrySet()) {
            var name = entry.getKey();
            var songs = entry.getValue();

            String artist = null;

            boolean compilation = false;

            for (var song : songs) {
                artist = coalesce(artist, song::getArtist);

                compilation |= song.isCompilation();
            }

            var artistAlbum = new ArtistAlbum();

            artistAlbum.setArtist(compilation ? "" : artist);
            artistAlbum.setAlbum(name);

            artistAlbums.add(artistAlbum);
        }

        artistAlbums.sort(artistAlbumComparator);

        artistAlbumList.setModel(new BasicListModel<>(artistAlbums));

        updateControls();

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

    private void addToPlaylist(Playlist playlist) {
        // TODO
    }

    private void editAlbum() {
        // TODO
    }

    private void editArtwork() {
        // TODO
    }

    private void editSong() {
        // TODO
    }

    private void deleteAlbum() {
        // TODO
    }

    private void deleteArtwork() {
        // TODO
    }

    private void deleteSong() {
        Song song = songList.getSelectedValue();

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

    private void updateControls() {
        // TODO Enable/disable menu buttons

        // TODO Enable/disable album/artwork buttons
    }

    public void scrollToSong(Song song) {
        // TODO Select artist album, then scroll to song
    }
}
