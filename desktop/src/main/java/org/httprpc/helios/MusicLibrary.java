// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.kilo.Name;
import org.httprpc.kilo.WebServiceProxy;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.io.TextDecoder;
import org.httprpc.kilo.sql.QueryBuilder;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.images.StandardArtwork;
import org.sqlite.SQLiteErrorCode;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.SequencedMap;
import java.util.function.Predicate;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;
import static org.httprpc.kilo.util.Optionals.*;

public class MusicLibrary {
    private static class ArtworkAPI {
        interface Response {
            List<Result> getResults();
        }

        interface Result {
            @Name("artistId")
            Integer getArtistID();

            String getCollectionName();

            @Name("artworkUrl100")
            String getArtworkURL100();
        }

        static final int REQUEST_DELAY = 3500;

        static final URI apiBaseURI = URI.create("https://itunes.apple.com/");

        static Result getArtist(String name) throws IOException {
            var webServiceProxy = new WebServiceProxy("GET", apiBaseURI.resolve("search"));

            webServiceProxy.setArguments(mapOf(
                entry("term", name.toLowerCase()),
                entry("entity", "musicArtist"),
                entry("country", Locale.getDefault().getCountry().toLowerCase()),
                entry("limit", 1)
            ));

            var results = BeanAdapter.coerce(webServiceProxy.invoke(), Response.class).getResults();

            return firstOf(results);
        }

        static Result getCollection(Integer artistID, String name) throws IOException {
            var webServiceProxy = new WebServiceProxy("GET", apiBaseURI.resolve("lookup"));

            webServiceProxy.setArguments(mapOf(
                entry("id", artistID),
                entry("entity", "album"),
                entry("country", Locale.getDefault().getCountry().toLowerCase()),
                entry("limit", 200)
            ));

            var results = BeanAdapter.coerce(webServiceProxy.invoke(), Response.class).getResults();

            var lowerCaseName = name.toLowerCase();

            return firstOf(filter(results, result -> {
                var collectionName = map(result.getCollectionName(), String::toLowerCase);

                if (collectionName == null) {
                    return false;
                }

                return collectionName.startsWith(lowerCaseName) || lowerCaseName.startsWith(collectionName);
            }));
        }

        static BufferedImage getArtwork(String artworkURL100) throws IOException {
            var uri = URI.create(artworkURL100).resolve(String.format("%dx%dbb.jpg", 720, 720));

            var webServiceProxy = new WebServiceProxy("GET", uri);

            webServiceProxy.setResponseHandler((inputStream, contentType) -> ImageIO.read(inputStream));

            return (BufferedImage)webServiceProxy.invoke();
        }
    }

    public static final String MP3_EXTENSION = ".mp3";
    public static final String M4A_EXTENSION = ".m4a";

    public static final String JPG_EXTENSION = ".jpg";
    public static final String JPEG_EXTENSION = ".jpeg";

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(MusicLibrary.class.getName());

    private static final Path rootDirectory = Path.of(System.getProperty("user.home"), ".helios");
    private static final Path dbFile = rootDirectory.resolve("music.db");

    private static final Predicate<Path> dsStoreFilter = path -> !path.getFileName().toString().equals(".DS_Store");

    private static final Comparator<Song> albumComparator = Comparator.comparing(Song::getSortableAlbum)
        .thenComparing(song -> coalesce(song.getDiscNumber(), () -> 0))
        .thenComparing(song -> coalesce(song.getTrackNumber(), () -> 0));

    private static final Comparator<Song> playlistComparator = Comparator.comparing(Song::getSortableArtist)
        .thenComparing(Song::getSortableTitle)
        .thenComparing(Song::getSortableAlbum);

    private static final Comparator<ArtistAlbum> albumArtworkComparator = Comparator.comparing(ArtistAlbum::getSortableArtist)
        .thenComparing(ArtistAlbum::getSortableAlbum);

    private static List<String> articles;
    static {
        articles = listOf(mapAll(iterableOf(resourceBundle.getString("articles").split(",")), article -> article.strip().toLowerCase()));
    }

    private MusicLibrary() {
    }

    public static void initialize() throws Exception {
        Files.createDirectories(rootDirectory);

        if (!Files.exists(dbFile)) {
            String sql;
            try (var inputStream = MainFrame.class.getResourceAsStream("/db.sql")) {
                var textDecoder = new TextDecoder();

                sql = textDecoder.read(inputStream);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }

            try (var connection = openConnection();
                var statement = connection.createStatement()) {
                statement.executeUpdate(sql);
            }
        }
    }

    public static String getSortableValue(String value) {
        var sortableValue = toLowerCase(value);

        for (var article : articles) {
            var n = article.length();

            if (sortableValue.startsWith(article)
                && n < value.length()
                && Character.isWhitespace(value.charAt(n))) {
                return sortableValue.substring(n).strip();
            }
        }

        return sortableValue;
    }

    private static String toLowerCase(String value) {
        var n = value.length();

        var sortableValueBuilder = new StringBuilder(n);

        for (var i = 0; i < n; i++) {
            var c = value.charAt(i);

            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c)) {
                sortableValueBuilder.append(Character.toLowerCase(c));
            }
        }

        return sortableValueBuilder.toString();
    }

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(String.format("jdbc:sqlite:%s?foreign_keys=true", dbFile.toAbsolutePath()));
    }

    public static List<ExpandedArtist> getArtists() {
        var queryBuilder = QueryBuilder.select(ExpandedArtist.class);

        try (var connection = openConnection();
            var statement = queryBuilder.prepare(connection);
            var results = queryBuilder.executeQuery(statement)) {
            return sortBy(mapAll(results, BeanAdapter.toType(ExpandedArtist.class)), Artist::getSortableName);
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static List<ExpandedGenre> getGenres() {
        var queryBuilder = QueryBuilder.select(ExpandedGenre.class);

        try (var connection = openConnection();
            var statement = queryBuilder.prepare(connection);
            var results = queryBuilder.executeQuery(statement)) {
            return sortBy(mapAll(results, BeanAdapter.toType(ExpandedGenre.class)), Genre::getSortableName);
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static List<ExpandedPlaylist> getPlaylists() {
        var queryBuilder = QueryBuilder.select(ExpandedPlaylist.class);

        try (var connection = openConnection();
            var statement = queryBuilder.prepare(connection);
            var results = queryBuilder.executeQuery(statement)) {
            return sortBy(mapAll(results, BeanAdapter.toType(ExpandedPlaylist.class)), Playlist::getSortableName);
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static SequencedMap<String, List<Song>> getAlbums(Artist artist) {
        var queryBuilder = QueryBuilder.select(Song.class).filterByForeignKey(Artist.class, "artist");

        try (var connection = openConnection();
            var statement = queryBuilder.prepare(connection);
            var results = queryBuilder.executeQuery(statement, mapOf(
                entry("artist", artist.getName())
            ))) {
            return groupBy(sortBy(mapAll(results, BeanAdapter.toType(Song.class)), albumComparator), Song::getAlbum);
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static SequencedMap<String, List<Song>> getAlbums(Genre genre) {
        var queryBuilder = QueryBuilder.select(Song.class).filterByForeignKey(Genre.class, "genre");

        try (var connection = openConnection();
            var statement = queryBuilder.prepare(connection);
            var results = queryBuilder.executeQuery(statement, mapOf(
                entry("genre", genre.getName())
            ))) {
            return groupBy(sortBy(mapAll(results, BeanAdapter.toType(Song.class)), albumComparator), Song::getAlbum);
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static List<Song> getSongs(Playlist playlist) {
        var queryBuilder = QueryBuilder.select(Song.class)
            .join(PlaylistSong.class, Song.class)
            .filterByForeignKey(PlaylistSong.class, Playlist.class, "playlistID");

        try (var connection = openConnection();
            var statement = queryBuilder.prepare(connection);
            var results = queryBuilder.executeQuery(statement, mapOf(
                entry("playlistID", playlist.getID())
            ))) {
            return sortBy(mapAll(results, BeanAdapter.toType(Song.class)), playlistComparator);
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static List<String> getGenreSuggestions() {
        var queryBuilder = new QueryBuilder();

        queryBuilder.append("select distinct genre from Song where genre is not null");

        try (var connection = openConnection();
            var statement = queryBuilder.prepare(connection);
            var results = queryBuilder.executeQuery(statement)) {
            return listOf(mapAll(results, result -> (String)result.get("genre")));
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static List<Song> findSongs(String text) {
        text = text.strip();

        if (text.isEmpty()) {
            return listOf();
        }

        var queryBuilder = QueryBuilder.select(Song.class).filterByIndexLike("title");

        try (var connection = openConnection();
            var statement = queryBuilder.prepare(connection);
            var results = queryBuilder.executeQuery(statement, mapOf(
                entry("title", String.format("%%%s%%", text))
            ))) {
            return sortBy(mapAll(results, BeanAdapter.toType(Song.class)), Comparator.comparing(Song::getTitle)
                .thenComparing(Song::getAlbum)
                .thenComparing(Song::getArtist));
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static void addSong(Path path) {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException();
        }

        if (path.startsWith(rootDirectory)) {
            return;
        }

        try {
            AudioFile audioFile;
            try {
                audioFile = AudioFileIO.read(path.toFile());
            } catch (CannotReadException | TagException | InvalidAudioFrameException | ReadOnlyFileException exception) {
                throw new IOException(exception);
            }

            var tag = audioFile.getTag();
            var audioHeader = audioFile.getAudioHeader();

            var artist = coalesce(tag.getFirst(FieldKey.ARTIST), () -> "").strip();
            var album = coalesce(tag.getFirst(FieldKey.ALBUM), () -> "").strip();
            var title = coalesce(tag.getFirst(FieldKey.TITLE), () -> "").strip();

            if (artist.isEmpty() || album.isEmpty() || title.isEmpty()) {
                throw new IOException("Missing required fields.");
            }

            var time = audioHeader.getTrackLength();

            var song = new Song();

            song.setArtist(artist);
            song.setAlbum(album);
            song.setTitle(title);
            song.setTime(time);

            var genre = coalesce(tag.getFirst(FieldKey.GENRE), () -> "").strip();

            if (!genre.isEmpty()) {
                song.setGenre(tag.getFirst(FieldKey.GENRE));
            }

            var year = map(tag.getFirst(FieldKey.YEAR), String::strip);

            if (year != null) {
                try {
                    song.setYear(Integer.parseInt(year));
                } catch (Exception exception) {
                    // No-op
                }

                if (song.getYear() == null) {
                    try {
                        var instant = Instant.parse(year);
                        var localDateTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);

                        song.setYear(localDateTime.getYear());
                    } catch (Exception exception) {
                        // No-op
                    }
                }
            }

            var trackNumber = map(tag.getFirst(FieldKey.TRACK), String::strip);

            if (trackNumber != null) {
                try {
                    song.setTrackNumber(Integer.parseInt(trackNumber));
                } catch (Exception exception) {
                    // No-op
                }
            }

            var discNumber = map(tag.getFirst(FieldKey.DISC_NO), String::strip);

            if (discNumber != null) {
                try {
                    song.setDiscNumber(Integer.parseInt(discNumber));
                } catch (Exception exception) {
                    // No-op
                }
            }

            var compilation = map(tag.getFirst(FieldKey.IS_COMPILATION), String::strip);

            if (compilation != null) {
                try {
                    song.setCompilation(Integer.parseInt(compilation) > 0);
                } catch (Exception exception) {
                    // No-op
                }
            }

            if (song.isCompilation() == null) {
                song.setCompilation(false);
            }

            if (song.isCompilation() && song.getGenre() == null) {
                return;
            }

            if (!song.isCompilation()) {
                var albumArtist = coalesce(tag.getFirst(FieldKey.ALBUM_ARTIST), () -> "").strip();

                if (!albumArtist.isEmpty()) {
                    song.setArtist(albumArtist);
                }
            }

            var type = audioFile.getExt();

            song.setType(type);

            var queryBuilder = QueryBuilder.insert(Song.class).onConflictDoUpdate();

            try (var connection = openConnection();
                var statement = queryBuilder.prepare(connection)) {
                queryBuilder.executeUpdate(statement, new BeanAdapter(song));
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }

            var contentPath = getContentPath(song);

            Files.createDirectories(contentPath.getParent());
            Files.copy(path, contentPath, StandardCopyOption.REPLACE_EXISTING);

            extractAlbumArtwork(song, tag);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static boolean updateSong(Song song, Song previousSong) {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException();
        }

        var queryBuilder = QueryBuilder.update(Song.class).filterByPrimaryKey("id");

        try (var connection = openConnection();
            var statement = queryBuilder.prepare(connection)) {
            queryBuilder.executeUpdate(statement, new BeanAdapter(song));
        } catch (SQLException exception) {
            if (SQLiteErrorCode.getErrorCode(exception.getErrorCode()) == SQLiteErrorCode.SQLITE_CONSTRAINT) {
                return false;
            }

            throw new RuntimeException(exception);
        }

        var contentPath = getContentPath(song);

        AudioFile audioFile;
        try {
            try {
                audioFile = AudioFileIO.read(contentPath.toFile());
            } catch (CannotReadException | TagException | InvalidAudioFrameException | ReadOnlyFileException exception) {
                throw new IOException(exception);
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        var tag = audioFile.getTag();

        try {
            var artist = song.getArtist();

            tag.setField(FieldKey.ARTIST, artist);
            tag.setField(FieldKey.ALBUM, song.getAlbum());
            tag.setField(FieldKey.TITLE, song.getTitle());

            var genre = song.getGenre();

            if (genre != null) {
                tag.setField(FieldKey.GENRE, genre);
            } else {
                tag.deleteField(FieldKey.GENRE);
            }

            var year = song.getYear();

            if (year != null) {
                tag.setField(FieldKey.YEAR, map(year, Object::toString));
            } else {
                tag.deleteField(FieldKey.YEAR);
            }

            var trackNumber = song.getTrackNumber();

            if (trackNumber != null) {
                tag.setField(FieldKey.TRACK, map(trackNumber, Object::toString));
            } else {
                tag.deleteField(FieldKey.TRACK);
            }

            var discNumber = song.getDiscNumber();

            if (discNumber != null) {
                tag.setField(FieldKey.DISC_NO, map(discNumber, Object::toString));
            } else {
                tag.deleteField(FieldKey.DISC_NO);
            }

            var compilation = song.isCompilation();

            tag.setField(FieldKey.IS_COMPILATION, String.valueOf(compilation ? 1 : 0));

            if (!compilation) {
                tag.setField(FieldKey.ALBUM_ARTIST, artist);
            }
        } catch (FieldDataInvalidException exception) {
            throw new RuntimeException(exception);
        }

        try {
            AudioFileIO.write(audioFile);
        } catch (CannotWriteException exception) {
            throw new RuntimeException(exception);
        }

        var previousContentPath = getContentPath(previousSong);

        if (!contentPath.equals(previousContentPath)) {
            try {
                var albumContentPath = contentPath.getParent();

                Files.createDirectories(albumContentPath);

                var temporaryContentPath = albumContentPath.resolve(String.format("%s.tmp", escape(song.getTitle())));

                Files.copy(previousContentPath, temporaryContentPath, StandardCopyOption.REPLACE_EXISTING);

                deleteSong(previousContentPath);

                Files.move(temporaryContentPath, contentPath, StandardCopyOption.REPLACE_EXISTING);

                extractAlbumArtwork(song, tag);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }

        return true;
    }

    private static void extractAlbumArtwork(Song song, Tag tag) throws IOException {
        if (song.isCompilation()) {
            return;
        }

        var artworkPath = getArtworkPath(song.getArtist(), song.getAlbum());

        if (!Files.exists(artworkPath, LinkOption.NOFOLLOW_LINKS)) {
            var artwork = tag.getFirstArtwork();

            if (artwork != null) {
                try (var inputStream = new ByteArrayInputStream(artwork.getBinaryData());
                    var outputStream = Files.newOutputStream(artworkPath,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING)) {
                    ImageIO.write(ImageIO.read(inputStream), "jpeg", outputStream);
                } catch (IOException exception) {
                    Files.deleteIfExists(artworkPath);
                }
            }
        }
    }

    public static void deleteSong(Song song) {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException();
        }

        var queryBuilder = QueryBuilder.delete(Song.class).filterByPrimaryKey("id");

        try (var connection = openConnection();
            var statement = queryBuilder.prepare(connection)) {
            queryBuilder.executeUpdate(statement, mapOf(
                entry("id", song.getID())
            ));
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }

        deleteSong(getContentPath(song));
    }

    public static boolean addPlaylist(Playlist playlist) {
        var queryBuilder = QueryBuilder.insert(Playlist.class);

        try (var connection = openConnection();
            var statement = queryBuilder.prepare(connection)) {
            queryBuilder.executeUpdate(statement, new BeanAdapter(playlist));
        } catch (SQLException exception) {
            if (SQLiteErrorCode.getErrorCode(exception.getErrorCode()) == SQLiteErrorCode.SQLITE_CONSTRAINT) {
                return false;
            }

            throw new RuntimeException(exception);
        }

        return true;
    }

    public static boolean updatePlaylist(Playlist playlist) {
        var queryBuilder = QueryBuilder.update(Playlist.class).filterByPrimaryKey("id");

        try (var connection = openConnection();
            var statement = queryBuilder.prepare(connection)) {
            queryBuilder.executeUpdate(statement, new BeanAdapter(playlist));
        } catch (SQLException exception) {
            if (SQLiteErrorCode.getErrorCode(exception.getErrorCode()) == SQLiteErrorCode.SQLITE_CONSTRAINT) {
                return false;
            }

            throw new RuntimeException(exception);
        }

        return true;
    }

    public static void deletePlaylist(Playlist playlist) {
        var queryBuilder = QueryBuilder.delete(Playlist.class).filterByPrimaryKey("id");

        try (var connection = openConnection();
            var statement = queryBuilder.prepare(connection)) {
            queryBuilder.executeUpdate(statement, mapOf(
                entry("id", playlist.getID())
            ));
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static void addToPlaylist(Playlist playlist, Song song) {
        var queryBuilder = QueryBuilder.insert(PlaylistSong.class);

        try (var connection = openConnection();
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
    }

    public static void removeFromPlaylist(Playlist playlist, List<Song> songs) {
        var queryBuilder = QueryBuilder.delete(PlaylistSong.class)
            .filterByForeignKey(Playlist.class, "playlistID")
            .filterByForeignKey(Song.class, "songID");

        try (var connection = openConnection();
            var statement = queryBuilder.prepare(connection)) {
            var playlistID = playlist.getID();

            for (var song : songs) {
                queryBuilder.addBatch(statement, mapOf(
                    entry("playlistID", playlistID),
                    entry("songID", song.getID())
                ));
            }

            statement.executeBatch();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static Path getContentPath(Song song) {
        var fileName = String.format("%s.%s", song.getTitle(), song.getType());

        return getContentPath(song.getArtist(), song.getAlbum()).resolve(escape(fileName));
    }

    public static Path getContentPath(String artist, String album) {
        return getPath(artist, album).resolve("content");
    }

    public static Path getArtworkPath(String artist, String album) {
        return getPath(artist, album).resolve("artwork.jpg");
    }

    private static Path getPath(String artist, String album) {
        return rootDirectory.resolve("music").resolve(escape(artist)).resolve(escape(album));
    }

    private static String escape(String component) {
        var n = component.length();

        var componentBuilder = new StringBuilder(n);

        for (var i = 0; i < n; i++) {
            var c = component.charAt(i);

            if (c == '\\' || c == '/' || c == ':') {
                c = '_';
            }

            componentBuilder.append(c);
        }

        return componentBuilder.toString();
    }

    private static void deleteSong(Path contentPath) {
        try {
            Files.deleteIfExists(contentPath);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        var albumContentPath = contentPath.getParent();

        try (var stream = Files.list(albumContentPath)) {
            if (isEmpty(filter(iterableOf(stream), dsStoreFilter))) {
                deleteAlbum(albumContentPath.getParent());
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void deleteAlbum(Path albumPath) {
        try {
            deleteAll(albumPath);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        var artistPath = albumPath.getParent();

        try (var stream = Files.list(artistPath)) {
            if (isEmpty(filter(iterableOf(stream), dsStoreFilter))) {
                deleteArtist(artistPath);
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void deleteArtist(Path artistPath) {
        try {
            deleteAll(artistPath);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void deleteAll(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }

        if (Files.isDirectory(root)) {
            try (var stream = Files.list(root)) {
                for (var path : iterableOf(stream)) {
                    deleteAll(path);
                }
            }
        }

        Files.delete(root);
    }

    public static void getAlbumArtwork() {
        var queryBuilder = QueryBuilder.select(ArtistAlbum.class);

        List<ArtistAlbum> artistAlbums;
        try (var connection = openConnection();
            var statement = queryBuilder.prepare(connection);
            var results = queryBuilder.executeQuery(statement)) {
            artistAlbums = sortBy(mapAll(results, BeanAdapter.toType(ArtistAlbum.class)), albumArtworkComparator);
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }

        for (var artistAlbum : artistAlbums) {
            var artist = artistAlbum.getArtist();
            var album = artistAlbum.getAlbum();

            var artworkPath = getArtworkPath(artist, album);

            if (Files.exists(artworkPath)) {
                continue;
            }

            System.out.println(String.format("Downloading artwork for %s / %s", artist, album));

            var t0 = System.currentTimeMillis();

            try {
                var artistID = map(ArtworkAPI.getArtist(artist), ArtworkAPI.Result::getArtistID);

                if (artistID != null) {
                    var artworkURL100 = map(ArtworkAPI.getCollection(artistID, album), ArtworkAPI.Result::getArtworkURL100);

                    if (artworkURL100 != null) {
                        var artwork = ArtworkAPI.getArtwork(artworkURL100);

                        if (artwork != null) {
                            updateAlbumArtwork(artist, album, artwork);
                        }
                    }
                }
            } catch (IOException exception) {
                System.out.println(exception.getMessage());
            }

            var t1 = System.currentTimeMillis();

            var delay = Math.max(ArtworkAPI.REQUEST_DELAY - (t1 - t0), 0);

            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException exception) {
                    throw new RuntimeException(exception);
                }
            }
        }
    }

    public static void updateAlbumArtwork(String artist, String album, BufferedImage artwork) {
        var artworkPath = getArtworkPath(artist, album);

        try (var outputStream = Files.newOutputStream(artworkPath,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING)) {
            ImageIO.write(artwork, "jpeg", outputStream);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        byte[] binaryData;
        try (var outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(artwork, "jpeg", outputStream);

            binaryData = outputStream.toByteArray();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        try (var contentPaths = Files.list(getContentPath(artist, album))) {
            for (var contentPath : iterableOf(contentPaths)) {
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
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static void deleteAlbumArtwork(String artist, String album) {
        try {
            Files.deleteIfExists(getArtworkPath(artist, album));
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        try (var contentPaths = Files.list(getContentPath(artist, album))) {
            for (var contentPath : iterableOf(contentPaths)) {
                try {
                    AudioFile audioFile;
                    try {
                        audioFile = AudioFileIO.read(contentPath.toFile());
                    } catch (CannotReadException | TagException | InvalidAudioFrameException | ReadOnlyFileException exception) {
                        throw new IOException(exception);
                    }

                    var tag = audioFile.getTag();

                    tag.deleteArtworkField();

                    try {
                        AudioFileIO.write(audioFile);
                    } catch (CannotWriteException exception) {
                        throw new RuntimeException(exception);
                    }
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
