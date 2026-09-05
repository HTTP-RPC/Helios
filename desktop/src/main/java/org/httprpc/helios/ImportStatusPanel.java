// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.sql.QueryBuilder;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.TaskExecutor;
import org.httprpc.sierra.UILoader;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.sqlite.SQLiteErrorCode;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.RootPaneContainer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Optionals.*;

public class ImportStatusPanel extends StackPanel {
    private @Outlet JProgressBar progressBar = null;

    private @Outlet JButton cancelButton = null;

    private List<Path> paths = null;

    private int index = -1;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(ImportStatusPanel.class.getName());

    private static final TaskExecutor taskExecutor = new TaskExecutor(Executors.newSingleThreadExecutor(runnable -> {
        var thread = new Thread(runnable);

        thread.setDaemon(true);

        return thread;
    }));

    public ImportStatusPanel() {
        add(UILoader.load(this, "ImportStatusPanel.xml", resourceBundle));

        cancelButton.addActionListener(event -> cancel());
    }

    public void addAll(List<Path> paths) {
        this.paths = paths;

        index = -1;

        progressBar.setMinimum(0);
        progressBar.setMaximum(paths.size());

        ((RootPaneContainer)getTopLevelAncestor()).getGlassPane().setVisible(true);

        setVisible(paths.size() > 1);

        addNext();
    }

    private void addNext() {
        index++;

        progressBar.setValue(index);

        if (index < paths.size()) {
            taskExecutor.execute(() -> {
                add(paths.get(index));

                return null;
            }, (result, exception) -> {
                addNext();
            });
        } else {
            close();
        }
    }

    private void add(Path path) {
        try {
            AudioFile audioFile;
            try {
                audioFile = AudioFileIO.read(path.toFile());
            } catch (CannotReadException | TagException | InvalidAudioFrameException | ReadOnlyFileException exception) {
                throw new IOException(exception);
            }

            var tag = audioFile.getTag();
            var audioHeader = audioFile.getAudioHeader();

            var artist = coalesce(tag.getFirst(FieldKey.ALBUM_ARTIST), () -> "");

            if (artist.isEmpty()) {
                artist = coalesce(tag.getFirst(FieldKey.ARTIST), () -> "");
            }

            var album = coalesce(tag.getFirst(FieldKey.ALBUM), () -> "");
            var title = coalesce(tag.getFirst(FieldKey.TITLE), () -> "");

            if (artist.isEmpty() || album.isEmpty() || title.isEmpty()) {
                throw new IOException("Missing required fields.");
            }

            var time = audioHeader.getTrackLength();

            var song = BeanAdapter.coerce(mapOf(), Song.class);

            song.setArtist(artist);
            song.setAlbum(album);
            song.setTitle(title);
            song.setTime(time);

            song.setGenre(tag.getFirst(FieldKey.GENRE));

            var year = tag.getFirst(FieldKey.YEAR);

            try {
                song.setYear(Integer.parseInt(year));
            } catch (Exception exception) {
                // No-op
            }

            if (year != null && song.getYear() == null) {
                try {
                    var instant = Instant.parse(year);
                    var localDateTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);

                    song.setYear(localDateTime.getYear());
                } catch (Exception exception) {
                    // No-op
                }
            }

            try {
                song.setTrackNumber(Integer.parseInt(tag.getFirst(FieldKey.TRACK)));
            } catch (Exception exception) {
                // No-op
            }

            try {
                song.setTrackCount(Integer.parseInt(tag.getFirst(FieldKey.TRACK_TOTAL)));
            } catch (Exception exception) {
                // No-op
            }

            try {
                song.setDiscNumber(Integer.parseInt(tag.getFirst(FieldKey.DISC_NO)));
            } catch (Exception exception) {
                // No-op
            }

            try {
                song.setDiscCount(Integer.parseInt(tag.getFirst(FieldKey.DISC_TOTAL)));
            } catch (Exception exception) {
                // No-op
            }

            var type = audioFile.getExt();

            song.setType(type);

            var queryBuilder = QueryBuilder.insert(Song.class);

            try (var connection = MainFrame.openConnection();
                var statement = queryBuilder.prepare(connection)) {
                queryBuilder.executeUpdate(statement, new BeanAdapter(song));
            } catch (SQLException exception) {
                if (SQLiteErrorCode.getErrorCode(exception.getErrorCode()) != SQLiteErrorCode.SQLITE_CONSTRAINT) {
                    throw new RuntimeException(exception);
                }
            }

            var contentPath = MainFrame.getContentPath(song);

            Files.createDirectories(contentPath.getParent());
            Files.copy(path, contentPath, StandardCopyOption.REPLACE_EXISTING);

            var albumArtworkPath = MainFrame.getArtworkPath(artist, album);

            if (!Files.exists(albumArtworkPath, LinkOption.NOFOLLOW_LINKS)) {
                var artwork = tag.getFirstArtwork();

                if (artwork != null) {
                    try (var inputStream = new ByteArrayInputStream(artwork.getBinaryData());
                        var outputStream = Files.newOutputStream(albumArtworkPath,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING)) {
                        ImageIO.write(ImageIO.read(inputStream), "jpeg", outputStream);
                    } catch (IOException exception) {
                        Files.deleteIfExists(albumArtworkPath);
                    }
                }
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private void cancel() {
        index = paths.size();

        cancelButton.setEnabled(false);
    }

    private void close() {
        ((RootPaneContainer)getTopLevelAncestor()).getGlassPane().setVisible(false);

        setVisible(false);

        MainFrame.getInstance().loadArtists();
    }
}
