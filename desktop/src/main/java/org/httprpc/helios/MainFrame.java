package org.httprpc.helios;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.io.TextDecoder;
import org.httprpc.kilo.sql.QueryBuilder;
import org.httprpc.sierra.BasicListModel;
import org.httprpc.sierra.ColumnPanel;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.TaskExecutor;
import org.httprpc.sierra.UILoader;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;

public class MainFrame extends JFrame implements Runnable {
    private abstract static class CollectionCellRenderer<T> extends ColumnPanel implements ListCellRenderer<T> {
        JLabel nameLabel = new JLabel();
        JLabel countLabel = new JLabel();

        CollectionCellRenderer() {
            setOpaque(true);

            setBorder(new EmptyBorder(4, 8, 4, 8));

            add(nameLabel);
            add(countLabel);

            countLabel.putClientProperty("FlatLaf.styleClass", "mini");
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends T> list,
            T value, int index,
            boolean selected, boolean cellHasFocus) {
            nameLabel.setText(getName(value));
            countLabel.setText(getCountText(value));

            Color background;
            Color foreground;
            if (selected) {
                background = list.getSelectionBackground();
                foreground = list.getSelectionForeground();
            } else {
                background = list.getBackground();
                foreground = list.getForeground();
            }

            setBackground(background);

            nameLabel.setForeground(foreground);
            countLabel.setForeground(foreground);

            return this;
        }

        protected abstract String getName(T value);
        protected abstract String getCountText(T value);
    }

    private static class ArtistCellRenderer extends CollectionCellRenderer<ExpandedArtist> {
        @Override
        protected String getName(ExpandedArtist value) {
            return value.getName();
        }

        @Override
        protected String getCountText(ExpandedArtist value) {
            var albumCount = value.getAlbumCount();
            var songCount = value.getSongCount();

            return String.format(resourceBundle.getString("countFormat"),
                String.format(resourceBundle.getString(albumCount == 1 ? "singleAlbumFormat" : "multipleAlbumFormat"), albumCount),
                String.format(resourceBundle.getString(songCount == 1 ? "singleSongFormat" : "multipleSongFormat"), songCount));
        }
    }

    private static class PlaylistCellRenderer extends CollectionCellRenderer<ExpandedPlaylist> {
        @Override
        protected String getName(ExpandedPlaylist value) {
            return value.getName();
        }

        @Override
        protected String getCountText(ExpandedPlaylist value) {
            var artistCount = value.getArtistCount();
            var songCount = value.getSongCount();

            return String.format(resourceBundle.getString("countFormat"),
                String.format(resourceBundle.getString(artistCount == 1 ? "singleArtistFormat" : "multipleArtistFormat"), artistCount),
                String.format(resourceBundle.getString(songCount == 1 ? "singleSongFormat" : "multipleSongFormat"), songCount));
        }
    }

    private @Outlet JButton playButton = null;

    private @Outlet JButton previousButton = null;
    private @Outlet JButton nextButton = null;

    private @Outlet JToggleButton shuffleButton = null;
    private @Outlet JToggleButton repeatButton = null;

    private @Outlet JButton showQueueButton = null;

    private @Outlet JLabel elapsedTimeLabel = null;
    private @Outlet JSlider positionSlider = null;
    private @Outlet JLabel remainingTimeLabel = null;

    private @Outlet JList<ExpandedArtist> artistList = null;
    private @Outlet JList<ExpandedPlaylist> playlistList = null;

    private @Outlet JScrollPane albumScrollPane = null;

    private FlatSVGIcon playIcon = new FlatSVGIcon(MainFrame.class.getResource("icons/play_arrow_24dp.svg"));
    private FlatSVGIcon pauseIcon = new FlatSVGIcon(MainFrame.class.getResource("icons/pause_24dp.svg"));

    private static final String DARK_MODE_KEY = "darkMode";
    private static final String LOCATION_X_KEY = "locationX";
    private static final String LOCATION_Y_KEY = "locationY";
    private static final String SIZE_WIDTH_KEY = "sizeWidth";
    private static final String SIZE_HEIGHT_KEY = "sizeHeight";

    private static final Path rootDirectory = Path.of(System.getProperty("user.home"), ".helios");
    private static final Path dbFile = rootDirectory.resolve("media.db");

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(MainFrame.class.getName());

    private static final Preferences preferences = Preferences.userRoot().node(MainFrame.class.getName());

    private static final TaskExecutor taskExecutor = new TaskExecutor(Executors.newSingleThreadExecutor(runnable -> {
        var thread = new Thread(runnable);

        thread.setDaemon(true);

        return thread;
    }));

    private MainFrame() {
        super(resourceBundle.getString("title"));

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        var playButtonColorFilter = new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground"));

        playIcon.setColorFilter(playButtonColorFilter);
        pauseIcon.setColorFilter(playButtonColorFilter);
    }

    @Override
    public void run() {
        setContentPane(UILoader.load(this, "MainFrame.xml", resourceBundle));

        // TODO
        playButton.setIcon(playIcon);
        playButton.setToolTipText(resourceBundle.getString("play"));

        var shuffleIcon = (FlatSVGIcon)shuffleButton.getIcon();

        shuffleIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> {
            if (shuffleButton.isSelected() || shuffleButton.getModel().isPressed()) {
                return UIManager.getColor("Slider.thumbColor");
            } else {
                return UILoader.getColor("Button.foreground");
            }
        }));

        var repeatIcon = (FlatSVGIcon)repeatButton.getIcon();

        repeatIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> {
            if (repeatButton.isSelected() || repeatButton.getModel().isPressed()) {
                return UIManager.getColor("Slider.thumbColor");
            } else {
                return UILoader.getColor("Button.foreground");
            }
        }));

        // TODO
        elapsedTimeLabel.setText("00:00");
        remainingTimeLabel.setText("-00:00");

        artistList.setCellRenderer(new ArtistCellRenderer());
        playlistList.setCellRenderer(new PlaylistCellRenderer());

        loadArtists();
        loadPlaylists();

        if (artistList.getModel().getSize() > 0) {
            artistList.setSelectedIndex(0);
        }

        pack();
        setMinimumSize(getSize());

        setLocationRelativeTo(null);

        setLocation(preferences.getInt(LOCATION_X_KEY, getX()), preferences.getInt(LOCATION_Y_KEY, getY()));
        setSize(preferences.getInt(SIZE_WIDTH_KEY, getWidth()), preferences.getInt(SIZE_HEIGHT_KEY, getHeight()));

        setVisible(true);

        artistList.requestFocus();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                var location = getLocation();

                preferences.putInt(LOCATION_X_KEY, location.x);
                preferences.putInt(LOCATION_Y_KEY, location.y);

                var size = getSize();

                preferences.putInt(SIZE_WIDTH_KEY, size.width);
                preferences.putInt(SIZE_HEIGHT_KEY, size.height);

                try {
                    preferences.flush();
                } catch (BackingStoreException exception) {
                    // No-op
                }
            }
        });
    }

    private void loadArtists() {
        var queryBuilder = QueryBuilder.select(ExpandedArtist.class).ordered(true);

        List<ExpandedArtist> artists;
        try (var connection = openConnection();
            var statement = queryBuilder.prepare(connection);
            var results = queryBuilder.executeQuery(statement)) {
            artists = listOf(mapAll(results, BeanAdapter.toType(ExpandedArtist.class)));
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }

        artistList.setModel(new BasicListModel<>(artists));
    }

    private void loadPlaylists() {
        var queryBuilder = QueryBuilder.select(ExpandedPlaylist.class).ordered(true);

        List<ExpandedPlaylist> playlists;
        try (var connection = openConnection();
            var statement = queryBuilder.prepare(connection);
            var results = queryBuilder.executeQuery(statement)) {
            playlists = listOf(mapAll(results, BeanAdapter.toType(ExpandedPlaylist.class)));
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }

        playlistList.setModel(new BasicListModel<>(playlists));
    }

    public static void main(String[] args) throws Exception {
        if (preferences.getBoolean(DARK_MODE_KEY, true)) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }

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

        SwingUtilities.invokeLater(new MainFrame());
    }

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(String.format("jdbc:sqlite:%s", dbFile.toAbsolutePath()));
    }
}
