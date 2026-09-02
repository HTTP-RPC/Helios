package org.httprpc.helios;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.sql.QueryBuilder;
import org.httprpc.sierra.BasicTableModel;
import org.httprpc.sierra.RowPanel;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import java.awt.KeyboardFocusManager;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;

public class PlaylistDetailPanel extends CollectionDetailPanel {
    private Playlist playlist;

    private JLabel nameLabel = new JLabel();

    private JButton playAllButton = new JButton();

    private List<Song> songs = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(PlaylistDetailPanel.class.getName());

    public PlaylistDetailPanel(Playlist playlist) {
        this.playlist = playlist;

        setScrollableTracksViewportHeight(true);
    }

    @Override
    public void load() {
        var playlistNamePanel = new RowPanel();

        playlistNamePanel.setSpacing(4);
        playlistNamePanel.setAlignToBaseline(true);

        nameLabel.setText(playlist.getName());

        nameLabel.putClientProperty("FlatLaf.styleClass", "h1");

        playlistNamePanel.add(nameLabel);

        var playIcon = new FlatSVGIcon(MainFrame.class.getResource("icons/play_arrow_24dp.svg")).derive(20, 20);

        playIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground")));

        playAllButton.setIcon(playIcon);
        playAllButton.setFocusable(false);
        playAllButton.setToolTipText(resourceBundle.getString("playAll"));
        playAllButton.putClientProperty("FlatLaf.style", "buttonType: borderless");

        playAllButton.addActionListener(event -> MainFrame.getInstance().playAll(songs));

        playlistNamePanel.add(playAllButton);

        // TODO Add buttons

        add(playlistNamePanel);

        var queryBuilder = QueryBuilder.select(Song.class)
            .join(PlaylistSong.class, Song.class)
            .filterByForeignKey(PlaylistSong.class, Playlist.class, "playlistID")
            .ordered(true);

        try (var connection = MainFrame.openConnection();
            var statement = queryBuilder.prepare(connection);
            var results = queryBuilder.executeQuery(statement, mapOf(
                entry("playlistID", playlist.getID())
            ))) {
            songs = sortBy(mapAll(results, BeanAdapter.toType(Song.class)), Song::getTitle);

            var songTable = new JTable();

            var songTableHeader = songTable.getTableHeader();

            songTableHeader.setReorderingAllowed(false);
            songTableHeader.setResizingAllowed(false);

            songTable.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
            songTable.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

            songTable.setModel(new BasicTableModel<>(Song.class, songs,
                listOf("title", "artist", "album"),
                resourceBundle));

            add(new JScrollPane(songTable), 1.0);
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }
}
