package org.httprpc.helios;

import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.sql.QueryBuilder;

import javax.swing.JLabel;
import java.sql.SQLException;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;

public class ArtistDetailPanel extends CollectionDetailPanel {
    private Artist artist;

    public ArtistDetailPanel(Artist artist) {
        this.artist = artist;
    }

    @Override
    public void load() {
        var artistLabel = new JLabel(artist.getName());

        artistLabel.putClientProperty("FlatLaf.styleClass", "h1");

        add(artistLabel);

        var queryBuilder = QueryBuilder.select(Song.class).filterByIndexEqualTo("artist").ordered(true);

        try (var connection = MainFrame.openConnection();
            var statement = queryBuilder.prepare(connection);
            var results = queryBuilder.executeQuery(statement, mapOf(
                entry("artist", artist.getName())
            ))) {
            var albums = groupBy(mapAll(results, BeanAdapter.toType(Song.class)), Song::getAlbum);

            for (var entry : albums.entrySet()) {
                var name = entry.getKey();
                var songs = entry.getValue();

                add(new AlbumDetailPanel(artist, name, songs));
            }
        } catch (SQLException exception) {
            // TODO
        }
    }
}
