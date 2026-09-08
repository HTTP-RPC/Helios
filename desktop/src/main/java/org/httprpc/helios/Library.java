// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.kilo.Name;
import org.httprpc.kilo.WebServiceProxy;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.io.TextDecoder;
import org.httprpc.kilo.sql.QueryBuilder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
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

    // TODO Make this private
    @Deprecated
    public static Connection openConnection() throws SQLException {
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

    // TODO Make this private
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
