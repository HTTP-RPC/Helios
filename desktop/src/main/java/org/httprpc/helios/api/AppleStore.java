// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios.api;

import org.httprpc.kilo.Name;
import org.httprpc.kilo.WebServiceProxy;
import org.httprpc.kilo.beans.BeanAdapter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;
import static org.httprpc.kilo.util.Optionals.*;

public class AppleStore {
    private interface Response {
        List<Result> getResults();
    }

    private interface Result {
        @Name("artistId")
        Integer getArtistID();

        String getCollectionName();

        @Name("artworkUrl100")
        String getArtworkURL100();
    }

    private static final URI apiBaseURI = URI.create("https://itunes.apple.com/");

    public static BufferedImage getAlbumArtwork(String artist, String album) throws IOException {
        var artistID = map(getArtist(artist.toLowerCase()), Result::getArtistID);

        if (artistID != null) {
            var artworkURL100 = map(getCollection(artistID, album.toLowerCase()), Result::getArtworkURL100);

            if (artworkURL100 != null) {
                return getArtwork(artworkURL100);
            }
        }

        return null;
    }

    private static Result getArtist(String name) throws IOException {
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

    private static Result getCollection(Integer artistID, String name) throws IOException {
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

    private static BufferedImage getArtwork(String artworkURL100) throws IOException {
        var uri = URI.create(artworkURL100).resolve(String.format("%dx%dbb.jpg", 720, 720));

        var webServiceProxy = new WebServiceProxy("GET", uri);

        webServiceProxy.setResponseHandler((inputStream, contentType) -> ImageIO.read(inputStream));

        return (BufferedImage)webServiceProxy.invoke();
    }
}
