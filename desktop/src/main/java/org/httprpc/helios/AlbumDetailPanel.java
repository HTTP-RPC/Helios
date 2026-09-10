// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.sierra.ColumnPanel;
import org.httprpc.sierra.ImagePane;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.RowPanel;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.TaskExecutor;
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
import java.util.concurrent.Executors;

import static org.httprpc.kilo.util.Optionals.*;

public class AlbumDetailPanel extends StackPanel {
    private Artist artist;
    private String name;

    private @Outlet JLabel nameLabel = null;
    private @Outlet JButton playAlbumButton = null;

    private @Outlet StackPanel artworkPanel = null;

    private @Outlet RowPanel artworkButtonPanel = null;

    private @Outlet JButton editArtworkButton = null;
    private @Outlet JButton deleteArtworkButton = null;

    private @Outlet ImagePane artworkImagePane = null;

    private @Outlet JLabel genreLabel = null;
    private @Outlet JLabel yearLabel = null;

    private @Outlet ColumnPanel songListPanel = null;

    private boolean compilation = false;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(AlbumDetailPanel.class.getName());

    private static final TaskExecutor taskExecutor = new TaskExecutor(Executors.newCachedThreadPool(runnable -> {
        var thread = new Thread(runnable);

        thread.setDaemon(true);

        return thread;
    }));

    public AlbumDetailPanel(Artist artist, String name, List<Song> songs) {
        this.artist = artist;
        this.name = name;

        add(UILoader.load(this, "AlbumDetailPanel.xml", resourceBundle));

        nameLabel.setText(name);

        playAlbumButton.addActionListener(event -> MainFrame.getInstance().playAll(songs));

        editArtworkButton.addActionListener(event -> editArtwork());
        editArtworkButton.setVisible(false);

        deleteArtworkButton.addActionListener(event -> deleteArtwork());
        deleteArtworkButton.setVisible(false);

        deleteArtworkButton.setEnabled(Files.exists(MusicLibrary.getArtworkPath(artist.getName(), name)));

        artworkPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                showButtons();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                var rectangle = new Rectangle(0, 0, artworkPanel.getWidth(), artworkPanel.getHeight());

                if (!rectangle.contains(event.getPoint())) {
                    hideButtons();
                }
            }
        });

        editArtworkButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                hideButtons();
            }
        });

        deleteArtworkButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                hideButtons();
            }
        });

        taskExecutor.execute(() -> {
            try (var inputStream = Files.newInputStream(MusicLibrary.getArtworkPath(artist.getName(), name))) {
                return ImageIO.read(inputStream);
            } catch (IOException exception) {
                return null;
            }
        }, (artwork, exception) -> artworkImagePane.setImage(artwork));

        songListPanel.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")),
            new EmptyBorder(2, 0, 0, 0)
        ));

        String genre = null;
        Integer year = null;

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

                    return path.endsWith(".jpg") || path.endsWith(".jpeg");
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
        var result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
            resourceBundle.getString("confirmDeleteArtwork"),
            resourceBundle.getString("deleteArtwork"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
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

        editArtworkButton.setEnabled(false);
        deleteArtworkButton.setEnabled(false);

        taskExecutor.execute(() -> {
            if (artwork != null) {
                MusicLibrary.updateAlbumArtwork(artist.getName(), name, artwork);
            } else {
                MusicLibrary.deleteAlbumArtwork(artist.getName(), name);
            }

            return null;
        }, (result, exception) -> {
            editArtworkButton.setEnabled(true);
            deleteArtworkButton.setEnabled(artwork != null);
        });
    }

    private void showButtons() {
        if (compilation) {
            return;
        }

        artworkButtonPanel.setOpaque(true);

        editArtworkButton.setVisible(true);
        deleteArtworkButton.setVisible(true);
    }

    private void hideButtons() {
        artworkButtonPanel.setOpaque(false);

        editArtworkButton.setVisible(false);
        deleteArtworkButton.setVisible(false);
    }

    public boolean matches(String name) {
        return this.name.equals(name);
    }
}
