// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios.api;

import org.httprpc.helios.MainFrame;
import org.httprpc.kilo.Name;
import org.httprpc.kilo.WebServiceProxy;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.xml.ElementAdapter;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;
import static org.httprpc.kilo.util.Optionals.*;

public class MusicBrainz {
    private interface Metadata {
        @Name("artist-list")
        ArtistList getArtistList();

        @Name("release-list")
        ReleaseList getReleaseList();
    }

    private interface ArtistList {
        @Name("artist*")
        List<Artist> getArtists();
    }

    private interface Artist {
        @Name("@id")
        String getID();

        @Name("@ns2:score")
        Integer getScore();
    }

    private interface ReleaseList {
        @Name("release*")
        List<Release> getReleases();
    }

    private interface Release {
        @Name("@id")
        String getID();

        String getTitle();
        String getCountry();
    }

    private static final URI apiBaseURI = URI.create("https://musicbrainz.org/ws/2/");
    private static final URI artworkBaseURI = URI.create("https://coverartarchive.org/");

    public static BufferedImage getAlbumArtwork(String artist, String album) throws IOException {
        var artistID = map(getArtist(artist), Artist::getID);

        if (artistID != null) {
            System.out.println(String.format("...got artist ID %s", artistID));

            pause();

            var releaseID = map(getRelease(artistID, album), Release::getID);

            if (releaseID != null) {
                System.out.println(String.format("...got release ID %s", releaseID));

                pause();

                return getArtwork(releaseID);
            }
        }

        return null;
    }

    private static Artist getArtist(String name) throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", apiBaseURI.resolve("artist"));

        webServiceProxy.setArguments(mapOf(
            entry("query", name)
        ));

        webServiceProxy.setHeaders(mapOf(
            entry("User-Agent", MainFrame.getUserAgent())
        ));

        webServiceProxy.setResponseHandler((inputStream, contentType) -> {
            var documentBuilder = ElementAdapter.newDocumentBuilder();

            try {
                return new ElementAdapter(documentBuilder.parse(inputStream).getDocumentElement());
            } catch (SAXException exception) {
                throw new IOException(exception);
            }
        });

        var metadata = BeanAdapter.coerce(webServiceProxy.invoke(), Metadata.class);

        var artists = sortBy(metadata.getArtistList().getArtists(), Comparator.comparing(Artist::getScore).reversed());

        return firstOf(filter(artists, whereEqualTo(Artist::getScore, 100)));
    }

    private static Release getRelease(String artistID, String title) throws IOException {
        var country = Locale.getDefault().getCountry();

        var webServiceProxy = new WebServiceProxy("GET", apiBaseURI.resolve("release"));

        webServiceProxy.setArguments(mapOf(
            entry("artist", artistID),
            entry("limit", 250)
        ));

        webServiceProxy.setHeaders(mapOf(
            entry("User-Agent", MainFrame.getUserAgent())
        ));

        webServiceProxy.setResponseHandler((inputStream, contentType) -> {
            var documentBuilder = ElementAdapter.newDocumentBuilder();

            try {
                return new ElementAdapter(documentBuilder.parse(inputStream).getDocumentElement());
            } catch (SAXException exception) {
                throw new IOException(exception);
            }
        });

        var metadata = BeanAdapter.coerce(webServiceProxy.invoke(), Metadata.class);

        var releases = metadata.getReleaseList().getReleases();

        return firstOf(filter(releases, release -> title.equals(release.getTitle()) && country.equalsIgnoreCase(release.getCountry())));
    }

    private static BufferedImage getArtwork(String releaseID) throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", artworkBaseURI.resolve(String.format("release/%s/front", releaseID)));

        webServiceProxy.setHeaders(mapOf(
            entry("User-Agent", MainFrame.getUserAgent())
        ));

        webServiceProxy.setResponseHandler((inputStream, contentType) -> ImageIO.read(inputStream));

        return (BufferedImage)webServiceProxy.invoke();
    }

    private static void pause() {
        try {
            Thread.sleep(2500);
        } catch (InterruptedException exception) {
            throw new RuntimeException(exception);
        }
    }
}
