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

import org.httprpc.sierra.ColumnPanel;
import org.httprpc.sierra.ImagePane;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.UILoader;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Optionals.*;

public class AlbumDetailPanel extends StackPanel {
    private Artist artist;
    private String name;
    private List<Song> songs;

    private String genre = null;
    private Integer year = null;

    private boolean compilation = false;

    private @Outlet JLabel nameLabel = null;
    private @Outlet JButton playAlbumButton = null;

    private @Outlet JButton editAlbumButton = null;
    private @Outlet JButton deleteAlbumButton = null;

    private @Outlet StackPanel artworkPanel = null;

    private @Outlet ColumnPanel artworkButtonPanel = null;

    private @Outlet JButton editArtworkButton = null;
    private @Outlet JButton deleteArtworkButton = null;

    private @Outlet ImagePane artworkImagePane = null;

    private @Outlet JLabel genreLabel = null;
    private @Outlet JLabel yearLabel = null;

    private @Outlet ColumnPanel songListPanel = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(AlbumDetailPanel.class.getName());

    public AlbumDetailPanel(Artist artist, String name, List<Song> songs) {
        this.artist = artist;
        this.name = name;
        this.songs = songs;

        add(UILoader.load(this, "AlbumDetailPanel.xml", resourceBundle));

        nameLabel.setText(name);

        playAlbumButton.addActionListener(event -> MainFrame.getInstance().playAll(songs));

        editAlbumButton.addActionListener(event -> editAlbum());
        deleteAlbumButton.addActionListener(event -> deleteAlbum());

        editArtworkButton.addActionListener(event -> editArtwork());
        editArtworkButton.setVisible(false);

        deleteArtworkButton.addActionListener(event -> deleteArtwork());
        deleteArtworkButton.setVisible(false);

        artworkPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                showArtworkButtons();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                var rectangle = new Rectangle(0, 0, artworkPanel.getWidth(), artworkPanel.getHeight());

                if (!rectangle.contains(event.getPoint())) {
                    hideArtworkButtons();
                }
            }
        });

        editArtworkButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                hideArtworkButtons();
            }
        });

        deleteArtworkButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                hideArtworkButtons();
            }
        });

        songListPanel.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")),
            new EmptyBorder(2, 0, 0, 0)
        ));

        Integer lastDiscNumber = null;

        for (var song : songs) {
            genre = coalesce(genre, song::getGenre);
            year = coalesce(year, song::getYear);

            var discNumber = song.getDiscNumber();

            if (discNumber != null) {
                if (lastDiscNumber != null && discNumber > lastDiscNumber) {
                    songListPanel.add(new JSeparator());
                }

                lastDiscNumber = discNumber;
            }

            compilation |= song.isCompilation();

            songListPanel.add(new SongDetailPanel(song));
        }

        genreLabel.setText(genre);
        yearLabel.setText(map(year, String::valueOf));

        editAlbumButton.setEnabled(!compilation);
        deleteAlbumButton.setEnabled(!compilation);

        var artworkPath = MusicLibrary.getArtworkPath(artist.getName(), name, compilation);

        if (Files.exists(artworkPath)) {
            MainFrame.getTaskExecutor().execute(() -> {
                try (var inputStream = Files.newInputStream(artworkPath)) {
                    return ImageIO.read(inputStream);
                } catch (IOException exception) {
                    return null;
                }
            }, (artwork, exception) -> {
                if (artwork != null) {
                    var width = artworkImagePane.getPreferredSize().width * 2;

                    artworkImagePane.setImage(MusicLibrary.downscale(artwork, width));
                }
            });
        } else {
            deleteArtworkButton.setEnabled(false);
        }
    }

    private void editAlbum() {
        var mainFrame = MainFrame.getInstance();

        var editAlbumDialog = new EditAlbumDialog(mainFrame, name, songs, genre, year, compilation);

        editAlbumDialog.pack();
        editAlbumDialog.setLocationRelativeTo(mainFrame);

        editAlbumDialog.setVisible(true);
    }

    private void deleteAlbum() {
        var option = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
            String.format(resourceBundle.getString("confirmDeleteMessageFormat"), name),
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
        var fileChooser = new JFileChooser();

        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (file.isDirectory()) {
                    return true;
                } else {
                    var path = file.getPath();

                    return path.endsWith(MusicLibrary.JPG_EXTENSION)
                        || path.endsWith(MusicLibrary.JPEG_EXTENSION);
                }
            }

            @Override
            public String getDescription() {
                return resourceBundle.getString("imageFileFilterDescription");
            }
        });

        var result = fileChooser.showOpenDialog(getTopLevelAncestor());

        if (result == JFileChooser.APPROVE_OPTION) {
            updateArtwork(fileChooser.getSelectedFile().toPath());
        }
    }

    private void deleteArtwork() {
        var option = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
            String.format(resourceBundle.getString("confirmDeleteArtworkMessageFormat"), name),
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

        artworkImagePane.setImage(artwork);

        var glassPane = MainFrame.getInstance().getGlassPane();

        glassPane.setVisible(true);

        MainFrame.getTaskExecutor().execute(() -> {
            if (artwork != null) {
                MusicLibrary.updateAlbumArtwork(artist.getName(), name, false, artwork);
            } else {
                MusicLibrary.deleteAlbumArtwork(artist.getName(), name, false);
            }

            return null;
        }, (result, exception) -> glassPane.setVisible(false));
    }

    private void showArtworkButtons() {
        if (compilation) {
            return;
        }

        artworkButtonPanel.setOpaque(true);

        editArtworkButton.setVisible(true);
        deleteArtworkButton.setVisible(true);
    }

    private void hideArtworkButtons() {
        artworkButtonPanel.setOpaque(false);

        editArtworkButton.setVisible(false);
        deleteArtworkButton.setVisible(false);
    }

    public boolean matches(String name) {
        return this.name.equals(name);
    }

    public void showCurrentSong(Song song) {
        var n = songListPanel.getComponentCount();

        for (var i = 0; i < n; i++) {
            if (songListPanel.getComponent(i) instanceof SongDetailPanel songDetailPanel) {
                songDetailPanel.showCurrentSong(song);
            }
        }
    }
}
