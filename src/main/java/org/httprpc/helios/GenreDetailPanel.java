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
import com.formdev.flatlaf.util.SystemFileChooser;
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
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.SequencedMap;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;
import static org.httprpc.kilo.util.Optionals.*;

public class GenreDetailPanel extends StackPanel implements LibraryDetail {
    private static class ArtistAlbumCellRenderer extends ColumnPanel implements ListCellRenderer<ArtistAlbum> {
        ImagePane artworkImagePane;

        JLabel missingArtworkLabel;

        JLabel albumLabel;
        JLabel artistLabel;

        static final int IMAGE_SIZE = 90;

        static final BufferedImage emptyImage = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);

        ArtistAlbumCellRenderer() {
            setOpaque(true);

            add(new RowPanel(), rowPanel -> {
                rowPanel.add(new StackPanel(), stackPanel -> {
                    stackPanel.add(new ImagePane(), imagePane -> {
                        imagePane.setPreferredSize(new Dimension(IMAGE_SIZE, IMAGE_SIZE));
                        imagePane.setScaleMode(ImagePane.ScaleMode.FILL_WIDTH);
                        imagePane.setBorder(UILoader.createRoundedLineBorder(UIManager.getColor("Component.borderColor"),
                            new BasicStroke(1,
                                BasicStroke.CAP_ROUND,
                                BasicStroke.JOIN_ROUND), 4));

                        artworkImagePane = imagePane;
                    });

                    stackPanel.add(new JLabel(), label -> {
                        var icon = new FlatSVGIcon(GenreDetailPanel.class.getResource("icons/photo_24dp.svg"));

                        label.setIcon(icon);

                        label.setHorizontalAlignment(SwingConstants.CENTER);
                        label.setVerticalAlignment(SwingConstants.CENTER);

                        missingArtworkLabel = label;
                    });
                });

                rowPanel.add(new Spacer(), 1.0);
            });

            add(new Spacer(4));

            add(new JLabel(), label -> albumLabel = label);
            add(new JLabel(), label -> {
                label.putClientProperty("FlatLaf.styleClass", "small");

                artistLabel = label;
            });

            setBorder(new EmptyBorder(4, 8, 4, 8));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ArtistAlbum> list,
            ArtistAlbum artistAlbum, int index,
            boolean selected, boolean cellHasFocus) {
            var artist = artistAlbum.getArtist();
            var album = artistAlbum.getAlbum();

            artworkImagePane.setImage(artistAlbum.getArtwork());

            if (artworkImagePane.getImage() == null && list.isValid()) {
                artistAlbum.setArtwork(emptyImage);

                MainFrame.getTaskExecutor().execute(() -> {
                    try (var inputStream = Files.newInputStream(MusicLibrary.getArtworkPath(artist, album, artist.isEmpty()))) {
                        return ImageIO.read(inputStream);
                    } catch (IOException exception) {
                        return null;
                    }
                }, (artwork, exception) -> {
                    if (artwork != null) {
                        artistAlbum.setArtwork(MusicLibrary.downscale(artwork, IMAGE_SIZE * 2));

                        if (index >= list.getFirstVisibleIndex() && index <= list.getLastVisibleIndex()) {
                            list.repaint();
                        }
                    }
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

            var missingArtworkIcon = (FlatSVGIcon)missingArtworkLabel.getIcon();

            missingArtworkIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> foreground));

            albumLabel.setForeground(foreground);
            artistLabel.setForeground(foreground);

            artistLabel.setEnabled(selected);

            return this;
        }
    }

    private static class SongCellRenderer extends ColumnPanel implements ListCellRenderer<Song> {
        JLabel titleLabel;
        JLabel nowPlayingLabel;
        JLabel timeLabel;
        JLabel artistLabel;

        SongCellRenderer() {
            setOpaque(true);

            add(new RowPanel(), rowPanel -> {
                rowPanel.setSpacing(4);

                rowPanel.add(new JLabel(), label -> titleLabel = label, 1.0);
                rowPanel.add(new JLabel(), label -> {
                    var icon = new FlatSVGIcon(GenreDetailPanel.class.getResource("icons/sensors_24dp.svg")).derive(18, 18);

                    icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Label.foreground")));

                    label.setIcon(icon);
                    label.setEnabled(false);

                    nowPlayingLabel = label;
                });
                rowPanel.add(new JLabel(), label -> {
                    label.setText(String.format(resourceBundle.getString("timeFormat"), 60, 0));
                    label.setPreferredSize(label.getPreferredSize());
                    label.setEnabled(false);

                    timeLabel = label;
                });
            });

            add(new JLabel(), label -> {
                label.putClientProperty("FlatLaf.styleClass", "small");

                artistLabel = label;
            });

            setBorder(new EmptyBorder(4, 8, 4, 8));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Song> list,
            Song song, int index,
            boolean selected, boolean cellHasFocus) {
            titleLabel.setText(song.getTitle());

            var currentSong = MainFrame.getInstance().getCurrentSong();

            nowPlayingLabel.setVisible(currentSong != null && song.getID().equals(currentSong.getID()));

            var duration = Duration.ofSeconds(song.getTime());

            timeLabel.setText(String.format(resourceBundle.getString("timeFormat"),
                duration.toMinutesPart(),
                duration.toSecondsPart()));

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
            timeLabel.setForeground(foreground);
            artistLabel.setForeground(foreground);

            nowPlayingLabel.setEnabled(selected);
            timeLabel.setEnabled(selected);
            artistLabel.setEnabled(selected);

            return this;
        }
    }

    private SequencedMap<String, List<Song>> albums;

    private @Outlet JLabel nameLabel = null;
    private @Outlet JButton playSelectedButton = null;

    private @Outlet MenuButton addToPlaylistButton = null;

    private @Outlet MenuButton editSelectedButton = null;
    private @Outlet JMenuItem editAlbumMenuItem = null;
    private @Outlet JMenuItem editArtworkMenuItem = null;

    private @Outlet MenuButton deleteSelectedButton = null;
    private @Outlet JMenuItem deleteAlbumMenuItem = null;
    private @Outlet JMenuItem deleteArtworkMenuItem = null;

    private @Outlet JList<ArtistAlbum> artistAlbumList = null;
    private @Outlet JList<Song> songList = null;

    private static final FlatSVGIcon playlistIcon;
    static {
        playlistIcon = new FlatSVGIcon(GenreDetailPanel.class.getResource("icons/queue_music_24dp.svg")).derive(18, 18);

        playlistIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground")));
    }

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(GenreDetailPanel.class.getName());

    private static Comparator<ArtistAlbum> artistComparator = (albumArtist1, albumArtist2) -> {
        var artist1 = albumArtist1.getSortableArtist();
        var artist2 = albumArtist2.getSortableArtist();

        if (artist1.isEmpty() && artist2.isEmpty()) {
            return 0;
        } else if (artist1.isEmpty()) {
            return 1;
        } else if (artist2.isEmpty()) {
            return -1;
        } else {
            return artist1.compareTo(artist2);
        }
    };

    private static Comparator<ArtistAlbum> artistAlbumComparator = artistComparator.thenComparing(ArtistAlbum::getSortableAlbum);

    public GenreDetailPanel(Genre genre, SequencedMap<String, List<Song>> albums) {
        this.albums = albums;

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

        add(UILoader.load(this, "GenreDetailPanel.xml", resourceBundle));

        nameLabel.setText(genre.getName());

        playSelectedButton.addActionListener(event -> {
            var artistAlbum = artistAlbumList.getSelectedValue();

            List<Song> songs;
            if (artistAlbum == null) {
                songs = listOf(flatten(mapAll(artistAlbums, ArtistAlbum::getAlbum), albums::get));
            } else {
                songs = albums.get(artistAlbum.getAlbum());

                var songIndex = songList.getSelectedIndex();

                if (songIndex != -1) {
                    songs = songs.subList(songIndex, songs.size());
                }
            }

            MainFrame.getInstance().playAll(songs);
        });

        addToPlaylistButton.setEnabled(false);

        editSelectedButton.addActionListener(event -> {
            if (songList.getSelectedValue() != null) {
                editSong();
            }
        });

        editAlbumMenuItem.addActionListener(event -> editAlbum());
        editArtworkMenuItem.addActionListener(event -> editArtwork());

        deleteSelectedButton.addActionListener(event -> {
            if (songList.getSelectedValue() != null) {
                deleteSong();
            }
        });

        deleteAlbumMenuItem.addActionListener(event -> deleteAlbum());
        deleteArtworkMenuItem.addActionListener(event -> deleteArtwork());

        artistAlbumList.setCellRenderer(new ArtistAlbumCellRenderer());
        artistAlbumList.setModel(new BasicListModel<>(artistAlbums));

        artistAlbumList.addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }

            var artistAlbum = artistAlbumList.getSelectedValue();

            List<Song> songs;
            if (artistAlbum != null) {
                songs = albums.get(artistAlbum.getAlbum());
            } else {
                songs = listOf();
            }

            songList.setModel(new BasicListModel<>(songs));

            updateControls();
        });

        artistAlbumList.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                songList.clearSelection();
            }
        });

        songList.setCellRenderer(new SongCellRenderer());

        songList.addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }

            updateControls();
        });

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

    @Override
    public void showCurrentSong(Song song) {
        songList.repaint();
    }

    private void addToPlaylist(Playlist playlist) {
        MusicLibrary.addToPlaylist(playlist, songList.getSelectedValue());

        MainFrame.getInstance().loadPlaylists();
    }

    private void editAlbum() {
        var artistAlbum = artistAlbumList.getSelectedValue();

        var artist = artistAlbum.getArtist();
        var album = artistAlbum.getAlbum();

        var songs = albums.get(album);

        String genre = null;
        Integer year = null;

        for (var song : songs) {
            genre = coalesce(genre, song::getGenre);
            year = coalesce(year, song::getYear);
        }

        var compilation = artistAlbum.getArtist().isEmpty();

        var mainFrame = MainFrame.getInstance();

        var editAlbumDialog = new EditAlbumDialog(mainFrame, artist, album, songs, genre, year, compilation);

        editAlbumDialog.pack();
        editAlbumDialog.setLocationRelativeTo(mainFrame);

        editAlbumDialog.setVisible(true);
    }

    private void deleteAlbum() {
        var artistAlbum = artistAlbumList.getSelectedValue();

        var album = artistAlbum.getAlbum();

        var songs = albums.get(album);

        var option = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
            String.format(resourceBundle.getString("confirmDeleteMessageFormat"), album),
            resourceBundle.getString("deleteAlbum"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (option == JOptionPane.YES_OPTION) {
            var mainFrame = MainFrame.getInstance();

            var glassPane = mainFrame.getGlassPane();

            glassPane.setVisible(true);

            MainFrame.getTaskExecutor().execute(() -> {
                for (var song : songs) {
                    MusicLibrary.deleteSong(song);
                }

                return null;
            }, (result, exception) -> {
                glassPane.setVisible(false);

                mainFrame.loadAll();
            });
        }
    }

    private void editArtwork() {
        var fileChooser = new SystemFileChooser();

        fileChooser.setFileSelectionMode(SystemFileChooser.FILES_ONLY);

        var filter = new SystemFileChooser.FileNameExtensionFilter(resourceBundle.getString("imageFileFilterDescription"),
            MusicLibrary.JPG_EXTENSION.substring(1),
            MusicLibrary.JPEG_EXTENSION.substring(1));

        fileChooser.addChoosableFileFilter(filter);

        var option = fileChooser.showOpenDialog(getTopLevelAncestor());

        if (option == SystemFileChooser.APPROVE_OPTION) {
            updateArtwork(fileChooser.getSelectedFile().toPath());
        }
    }

    private void deleteArtwork() {
        var artistAlbum = artistAlbumList.getSelectedValue();

        var option = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
            String.format(resourceBundle.getString("confirmDeleteArtworkMessageFormat"), artistAlbum.getAlbum()),
            resourceBundle.getString("deleteArtwork"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (option == JOptionPane.YES_OPTION) {
            updateArtwork(null);
        }
    }

    private void updateArtwork(Path path) {
        BufferedImage artwork;
        if (path != null) {
            try (var inputStream = Files.newInputStream(path)) {
                artwork = ImageIO.read(inputStream);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        } else {
            artwork = null;
        }

        var artistAlbum = artistAlbumList.getSelectedValue();

        artistAlbum.setArtwork(artwork);

        artistAlbumList.repaint();

        var glassPane = MainFrame.getInstance().getGlassPane();

        glassPane.setVisible(true);

        var artist = artistAlbum.getArtist();
        var album = artistAlbum.getAlbum();

        MainFrame.getTaskExecutor().execute(() -> {
            if (artwork != null) {
                MusicLibrary.updateAlbumArtwork(artist, album, artist.isEmpty(), artwork);
            } else {
                MusicLibrary.deleteAlbumArtwork(artist, album, artist.isEmpty());
            }

            return null;
        }, (result, exception) -> glassPane.setVisible(false));
    }

    private void editSong() {
        var song = songList.getSelectedValue();

        var mainFrame = MainFrame.getInstance();

        var editSongDialog = new EditSongDialog(mainFrame, song);

        editSongDialog.pack();
        editSongDialog.setLocationRelativeTo(mainFrame);

        editSongDialog.setVisible(true);
    }

    private void deleteSong() {
        var song = songList.getSelectedValue();

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
        if (artistAlbumList.getSelectedValue() != null) {
            if (songList.getSelectedValue() != null) {
                addToPlaylistButton.setEnabled(addToPlaylistButton.getComponentPopupMenu().getComponentCount() > 0);

                editSelectedButton.getComponentPopupMenu().setEnabled(false);
                deleteSelectedButton.getComponentPopupMenu().setEnabled(false);
            } else {
                addToPlaylistButton.setEnabled(false);

                editSelectedButton.getComponentPopupMenu().setEnabled(true);
                deleteSelectedButton.getComponentPopupMenu().setEnabled(true);
            }

            editSelectedButton.setEnabled(true);
            deleteSelectedButton.setEnabled(true);
        } else {
            addToPlaylistButton.setEnabled(false);

            editSelectedButton.setEnabled(false);
            deleteSelectedButton.setEnabled(false);
        }
    }

    public void clearSelection() {
        artistAlbumList.clearSelection();
        songList.clearSelection();
    }

    public void scrollToSong(Song song) {
        var artistAlbumModel = artistAlbumList.getModel();

        var n = artistAlbumModel.getSize();

        var album = song.getAlbum();

        for (var i = 0; i < n; i++) {
            if (artistAlbumModel.getElementAt(i).getAlbum().equals(album)) {
                selectAlbum(i);

                break;
            }
        }
    }

    private void selectAlbum(int index) {
        SwingUtilities.invokeLater(() -> {
            artistAlbumList.setSelectedIndex(index);
            artistAlbumList.ensureIndexIsVisible(index);
            artistAlbumList.requestFocus();
        });
    }
}
