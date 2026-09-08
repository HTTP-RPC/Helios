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
import org.jaudiotagger.tag.TagException;
import org.sqlite.SQLiteErrorCode;

import javax.imageio.ImageIO;
import javax.swing.UIManager;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Predicate;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;
import static org.httprpc.kilo.util.Optionals.*;

public class Library {
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

        static final URI apiBaseURI = URI.create("https://itunes.apple.com/");

        static Result getArtist(String name) throws IOException {
            var webServiceProxy = new WebServiceProxy("GET", apiBaseURI.resolve("search"));

            webServiceProxy.setArguments(mapOf(
                entry("term", name),
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

            return firstOf(filter(results, result -> {
                var collectionName = map(result.getCollectionName(), String::toLowerCase);

                return collectionName != null && (collectionName.startsWith(name) || name.startsWith(collectionName));
            }));
        }

        static BufferedImage getArtwork(String artworkURL100) throws IOException {
            var uri = URI.create(artworkURL100).resolve(String.format("%dx%dbb.jpg", 720, 720));

            var webServiceProxy = new WebServiceProxy("GET", uri);

            webServiceProxy.setResponseHandler((inputStream, contentType) -> ImageIO.read(inputStream));

            return (BufferedImage)webServiceProxy.invoke();
        }
    }

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(Library.class.getName());

    private static final Path rootDirectory = Path.of(System.getProperty("user.home"), ".helios");
    private static final Path dbFile = rootDirectory.resolve("music.db");

    private static final Predicate<Path> dsStoreFilter = path -> !path.getFileName().toString().equals(".DS_Store");

    private static final Comparator<Song> genreComparator = Comparator.comparing(Song::getSortableAlbum)
        .thenComparing(song -> song.isCompilation() ? "" : song.getSortableArtist())
        .thenComparing(song -> coalesce(song.getTrackNumber(), () -> 0));

    private static final Comparator<Song> playlistComparator = Comparator.comparing(Song::getSortableArtist)
        .thenComparing(Song::getSortableTitle)
        .thenComparing(Song::getSortableAlbum);

    private static List<String> articles;
    static {
        articles = listOf(mapAll(iterableOf(resourceBundle.getString("articles").split(",")), article -> article.strip().toLowerCase()));
    }

    private Library() {
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

    public static Map<String, List<Song>> getAlbums(Artist artist) {
        var queryBuilder = QueryBuilder.select(Song.class).filterByForeignKey(Artist.class, "artist");

        try (var connection = openConnection();
            var statement = queryBuilder.prepare(connection);
            var results = queryBuilder.executeQuery(statement, mapOf(
                entry("artist", artist.getName())
            ))) {
            return groupBy(sortBy(mapAll(results, BeanAdapter.toType(Song.class)), Song::getSortableAlbum), Song::getAlbum);
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static List<Song> getSongs(Genre genre) {
        var queryBuilder = QueryBuilder.select(Song.class).filterByForeignKey(Genre.class, "genre");

        try (var connection = openConnection();
            var statement = queryBuilder.prepare(connection);
            var results = queryBuilder.executeQuery(statement, mapOf(
                entry("genre", genre.getName())
            ))) {
            return sortBy(mapAll(results, BeanAdapter.toType(Song.class)), genreComparator);
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

        try (var connection = Library.openConnection();
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

        try (var connection = Library.openConnection();
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

            var trackCount = map(tag.getFirst(FieldKey.TRACK_TOTAL), String::strip);

            if (trackCount != null) {
                try {
                    song.setTrackCount(Integer.parseInt(trackCount));
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

            var discCount = map(tag.getFirst(FieldKey.DISC_TOTAL), String::strip);

            if (discCount != null) {
                try {
                    song.setDiscCount(Integer.parseInt(discCount));
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

            var type = audioFile.getExt();

            song.setType(type);

            var queryBuilder = QueryBuilder.insert(Song.class).onConflictDoUpdate();

            try (var connection = Library.openConnection();
                var statement = queryBuilder.prepare(connection)) {
                queryBuilder.executeUpdate(statement, new BeanAdapter(song));
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }

            var contentPath = Library.getContentPath(song);

            Files.createDirectories(contentPath.getParent());
            Files.copy(path, contentPath, StandardCopyOption.REPLACE_EXISTING);

            var artworkPath = Library.getArtworkPath(artist, album);

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
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static void updateSong(Song song, Song previousSong) {
        var queryBuilder = QueryBuilder.update(Song.class).filterByPrimaryKey("id");

        try (var connection = Library.openConnection();
            var statement = queryBuilder.prepare(connection)) {
            queryBuilder.executeUpdate(statement, new BeanAdapter(song));
        } catch (SQLException exception) {
            if (SQLiteErrorCode.getErrorCode(exception.getErrorCode()) == SQLiteErrorCode.SQLITE_CONSTRAINT) {
                UIManager.getLookAndFeel().provideErrorFeedback(null);

                return;
            }

            throw new RuntimeException(exception);
        }

        var contentPath = Library.getContentPath(song);
        var previousContentPath = Library.getContentPath(previousSong);

        if (!contentPath.equals(previousContentPath)) {
            try {
                var albumContentPath = contentPath.getParent();

                Files.createDirectories(albumContentPath);

                var temporaryContentPath = albumContentPath.resolve(String.format("%s.tmp", Library.escape(song.getTitle())));

                Files.copy(previousContentPath, temporaryContentPath, StandardCopyOption.REPLACE_EXISTING);

                Library.deleteSong(previousContentPath);

                Files.move(temporaryContentPath, contentPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }

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
            tag.setField(FieldKey.ARTIST, song.getArtist());
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

            var trackCount = song.getTrackCount();

            if (trackCount != null) {
                tag.setField(FieldKey.TRACK_TOTAL, map(trackCount, Object::toString));
            } else {
                tag.deleteField(FieldKey.TRACK_TOTAL);
            }

            var discNumber = song.getDiscNumber();

            if (discNumber != null) {
                tag.setField(FieldKey.DISC_NO, map(discNumber, Object::toString));
            } else {
                tag.deleteField(FieldKey.DISC_NO);
            }

            var discCount = song.getDiscCount();

            if (discCount != null) {
                tag.setField(FieldKey.DISC_TOTAL, map(discCount, Object::toString));
            } else {
                tag.deleteField(FieldKey.DISC_TOTAL);
            }

            tag.setField(FieldKey.IS_COMPILATION, String.valueOf(song.isCompilation() ? 1 : 0));
        } catch (FieldDataInvalidException exception) {
            throw new RuntimeException(exception);
        }

        try {
            AudioFileIO.write(audioFile);
        } catch (CannotWriteException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static void deleteSong(Song song) {
        var queryBuilder = QueryBuilder.delete(Song.class).filterByPrimaryKey("id");

        try (var connection = Library.openConnection();
            var statement = queryBuilder.prepare(connection)) {
            queryBuilder.executeUpdate(statement, mapOf(
                entry("id", song.getID())
            ));
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }

        deleteSong(Library.getContentPath(song));
    }

    public static void addToPlaylist(Playlist playlist, Song song) {
        var queryBuilder = QueryBuilder.insert(PlaylistSong.class);

        try (var connection = Library.openConnection();
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

        try (var connection = Library.openConnection();
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

    public static boolean addPlaylist(Playlist playlist) {
        var queryBuilder = QueryBuilder.insert(Playlist.class);

        try (var connection = Library.openConnection();
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

        try (var connection = Library.openConnection();
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

        try (var connection = Library.openConnection();
            var statement = queryBuilder.prepare(connection)) {
            queryBuilder.executeUpdate(statement, mapOf(
                entry("id", playlist.getID())
            ));
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(String.format("jdbc:sqlite:%s?foreign_keys=true", dbFile.toAbsolutePath()));
    }

    public static Path getAlbumPath(String artist, String album) {
        return rootDirectory.resolve("music").resolve(escape(artist)).resolve(escape(album));
    }

    public static Path getArtworkPath(String artist, String album) {
        return getAlbumPath(artist, album).resolve("artwork.jpg");
    }

    public static Path getContentPath(Song song) {
        var fileName = String.format("%s.%s", song.getTitle(), song.getType());

        return getAlbumPath(song.getArtist(), song.getAlbum()).resolve("content").resolve(escape(fileName));
    }

    // TODO Make this private?
    public static String escape(String component) {
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

    public static void deleteArtist(Path artistPath) {
        try {
            deleteAll(artistPath);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static void deleteAlbum(Path albumPath) {
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

    public static void deleteSong(Path contentPath) {
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

    public static String getSortableValue(String value) {
        var sortableValue = value.toLowerCase().strip();

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

    public static void getAlbumArtwork() throws IOException {
        var queryBuilder = QueryBuilder.select(ArtistAlbum.class).ordered(true);

        List<ArtistAlbum> artistAlbums;
        try (var connection = openConnection();
            var statement = queryBuilder.prepare(connection);
            var results = queryBuilder.executeQuery(statement)) {
            artistAlbums = listOf(mapAll(results, BeanAdapter.toType(ArtistAlbum.class)));
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

            var artistID = map(ArtworkAPI.getArtist(artist.toLowerCase()), ArtworkAPI.Result::getArtistID);

            if (artistID != null) {
                var artworkURL100 = map(ArtworkAPI.getCollection(artistID, album.toLowerCase()), ArtworkAPI.Result::getArtworkURL100);

                if (artworkURL100 != null) {
                    var artwork = ArtworkAPI.getArtwork(artworkURL100);

                    if (artwork != null) {
                        try (var outputStream = Files.newOutputStream(artworkPath,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING)) {
                            ImageIO.write(artwork, "jpeg", outputStream);
                        }
                    }
                }
            }
        }
    }
}
