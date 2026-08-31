package org.httprpc.helios;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.sql.QueryBuilder;
import org.httprpc.sierra.RowPanel;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.UIManager;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;
import static org.httprpc.kilo.util.Optionals.*;

public class ArtistDetailPanel extends CollectionDetailPanel {
    private Artist artist;

    private JLabel nameLabel = new JLabel();

    private JButton playAllButton = new JButton();

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(ArtistDetailPanel.class.getName());

    public ArtistDetailPanel(Artist artist) {
        this.artist = artist;
    }

    @Override
    public void load() {
        var artistNamePanel = new RowPanel();

        artistNamePanel.setSpacing(4);
        artistNamePanel.setAlignToBaseline(true);

        nameLabel.setText(artist.getName());

        nameLabel.putClientProperty("FlatLaf.styleClass", "h1");

        artistNamePanel.add(nameLabel);

        var playIcon = new FlatSVGIcon(MainFrame.class.getResource("icons/play_arrow_24dp.svg")).derive(20, 20);

        playIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground")));

        playAllButton.setIcon(playIcon);
        playAllButton.setFocusable(false);
        playAllButton.setToolTipText(resourceBundle.getString("playAll"));
        playAllButton.putClientProperty("FlatLaf.style", "buttonType: borderless");

        artistNamePanel.add(playAllButton);

        add(artistNamePanel);

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

                songs.sort(Comparator.comparing(song -> coalesce(song.getTrackNumber(), () -> 0)));

                add(new AlbumDetailPanel(artist, name, songs));
            }
        } catch (SQLException exception) {
            // TODO
        }
    }
}
