// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.sierra.ColumnPanel;
import org.httprpc.sierra.ImagePane;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.RowPanel;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.TaskExecutor;
import org.httprpc.sierra.UILoader;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.images.StandardArtwork;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;

public class AlbumDetailPanel extends StackPanel {
    private Artist artist;
    private String name;
    private List<Song> songs;

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

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(AlbumDetailPanel.class.getName());

    private static final TaskExecutor taskExecutor = new TaskExecutor(Executors.newCachedThreadPool(runnable -> {
        var thread = new Thread(runnable);

        thread.setDaemon(true);

        return thread;
    }));

    public AlbumDetailPanel(Artist artist, String name, List<Song> songs) {
        this.artist = artist;
        this.name = name;
        this.songs = songs;

        add(UILoader.load(this, "AlbumDetailPanel.xml", resourceBundle));

        nameLabel.setText(name);

        playAlbumButton.addActionListener(event -> MainFrame.getInstance().playAll(songs));

        editArtworkButton.addActionListener(event -> editArtwork());
        editArtworkButton.setVisible(false);

        deleteArtworkButton.addActionListener(event -> deleteArtwork());
        deleteArtworkButton.setVisible(false);

        deleteArtworkButton.setEnabled(Files.exists(MainFrame.getArtworkPath(artist.getName(), name)));

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
            try (var inputStream = Files.newInputStream(MainFrame.getArtworkPath(artist.getName(), name))) {
                return ImageIO.read(inputStream);
            } catch (IOException exception) {
                return null;
            }
        }, (artwork, exception) -> artworkImagePane.setImage(artwork));

        for (var song : songs) {
            var genre = song.getGenre();

            if (genre != null) {
                genreLabel.setText(genre);
                break;
            }
        }

        for (var song : songs) {
            var year = song.getYear();

            if (year != null) {
                yearLabel.setText(String.valueOf(year));
                break;
            }
        }

        songListPanel.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")),
            new EmptyBorder(2, 0, 0, 0)
        ));

        for (var song : songs) {
            songListPanel.add(new SongDetailPanel(song));
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

                    return path.endsWith(".jpg") || path.endsWith(".jpeg");
                }
            }

            @Override
            public String getDescription() {
                return resourceBundle.getString("imageFileFilterDescription");
            }
        });

        var result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            var path = fileChooser.getSelectedFile().toPath();

            BufferedImage artwork;
            try (var inputStream = Files.newInputStream(path)) {
                artwork = ImageIO.read(inputStream);
            } catch (IOException exception) {
                artwork = null;
            }

            updateArtwork(artwork);
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

    private void updateArtwork(BufferedImage artwork) {
        artworkImagePane.setImage(artwork);

        byte[] binaryData;
        if (artwork != null) {
            var artworkPath = MainFrame.getArtworkPath(artist.getName(), name);

            try (var outputStream = Files.newOutputStream(artworkPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
                ImageIO.write(artwork, "jpeg", outputStream);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }

            try (var outputStream = new ByteArrayOutputStream()) {
                ImageIO.write(artwork, "jpeg", outputStream);

                binaryData = outputStream.toByteArray();
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        } else {
            try {
                Files.deleteIfExists(MainFrame.getArtworkPath(artist.getName(), name));
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }

            binaryData = null;
        }

        for (var song : songs) {
            var contentPath = MainFrame.getContentPath(song);

            try {
                AudioFile audioFile;
                try {
                    audioFile = AudioFileIO.read(contentPath.toFile());
                } catch (CannotReadException | TagException | InvalidAudioFrameException | ReadOnlyFileException exception) {
                    throw new IOException(exception);
                }

                var tag = audioFile.getTag();

                var artworkField = new StandardArtwork();

                artworkField.setBinaryData(binaryData);

                try {
                    tag.setField(artworkField);
                } catch (FieldDataInvalidException exception) {
                    throw new IOException(exception);
                }

                try {
                    AudioFileIO.write(audioFile);
                } catch (CannotWriteException exception) {
                    throw new RuntimeException(exception);
                }
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    private void showButtons() {
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
