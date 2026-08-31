package org.httprpc.helios;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.io.TextDecoder;
import org.httprpc.kilo.sql.QueryBuilder;
import org.httprpc.sierra.BasicListModel;
import org.httprpc.sierra.ColumnPanel;
import org.httprpc.sierra.MenuButton;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.UILoader;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
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
            if (selected && cellHasFocus) {
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

    private @Outlet JButton playPauseButton = null;

    private @Outlet JButton previousButton = null;
    private @Outlet JButton nextButton = null;

    private @Outlet JToggleButton shuffleButton = null;
    private @Outlet JToggleButton repeatButton = null;

    private @Outlet JButton queueButton = null;

    private @Outlet MenuButton addButton = null;
    private @Outlet JMenuItem addSongsMenuItem = null;
    private @Outlet JMenuItem addPlaylistMenuItem = null;

    private @Outlet JButton searchButton = null;
    private @Outlet JButton settingsButton = null;

    private @Outlet JLabel elapsedTimeLabel = null;
    private @Outlet JSlider positionSlider = null;
    private @Outlet JLabel remainingTimeLabel = null;

    private @Outlet JList<ExpandedArtist> artistList = null;
    private @Outlet JList<ExpandedPlaylist> playlistList = null;

    private @Outlet JScrollPane collectionScrollPane = null;

    private boolean playing = false;

    private Song selectedSong = null;

    private FlatSVGIcon playIcon = new FlatSVGIcon(MainFrame.class.getResource("icons/play_arrow_24dp.svg"));
    private FlatSVGIcon pauseIcon = new FlatSVGIcon(MainFrame.class.getResource("icons/pause_24dp.svg"));

    private static final String PLAY_PAUSE_KEY = "playPause";
    private static final String PREVIOUS_KEY = "previous";
    private static final String NEXT_KEY = "next";
    private static final String SHUFFLE_KEY = "shuffle";
    private static final String REPEAT_KEY = "repeat";
    private static final String QUEUE_KEY = "queue";
    private static final String ADD_KEY = "add";
    private static final String SEARCH_KEY = "search";
    private static final String SETTINGS_KEY = "settings";

    private static final String LOCATION_X_KEY = "locationX";
    private static final String LOCATION_Y_KEY = "locationY";
    private static final String SIZE_WIDTH_KEY = "sizeWidth";
    private static final String SIZE_HEIGHT_KEY = "sizeHeight";

    private static final Path rootDirectory = Path.of(System.getProperty("user.home"), ".helios");
    private static final Path dbFile = rootDirectory.resolve("media.db");

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(MainFrame.class.getName());

    private static final Preferences preferences = Preferences.userRoot().node(MainFrame.class.getName());

    private MainFrame() {
        super(resourceBundle.getString("title"));

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        var playButtonColorFilter = new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground"));

        playIcon.setColorFilter(playButtonColorFilter);
        pauseIcon.setColorFilter(playButtonColorFilter);

        var inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        var actionMap = rootPane.getActionMap();

        var shortcutModifier = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false), PLAY_PAUSE_KEY);
        actionMap.put(PLAY_PAUSE_KEY, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                playPauseButton.doClick();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, shortcutModifier, false), PREVIOUS_KEY);
        actionMap.put(PREVIOUS_KEY, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                previousButton.doClick();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, shortcutModifier, false), NEXT_KEY);
        actionMap.put(NEXT_KEY, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                nextButton.doClick();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutModifier, false), SHUFFLE_KEY);
        actionMap.put(SHUFFLE_KEY, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                shuffleButton.doClick();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, shortcutModifier, false), REPEAT_KEY);
        actionMap.put(REPEAT_KEY, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                repeatButton.doClick();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, shortcutModifier, false), QUEUE_KEY);
        actionMap.put(QUEUE_KEY, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                queueButton.doClick();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, shortcutModifier, false), ADD_KEY);
        actionMap.put(ADD_KEY, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                addButton.doClick();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, shortcutModifier, false), SEARCH_KEY);
        actionMap.put(SEARCH_KEY, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                searchButton.doClick();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, shortcutModifier, false), SETTINGS_KEY);
        actionMap.put(SETTINGS_KEY, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                settingsButton.doClick();
            }
        });
    }

    @Override
    public void run() {
        setContentPane(UILoader.load(this, "MainFrame.xml", resourceBundle));

        playPauseButton.addActionListener(event -> {
            if (!playing) {
                play();
            } else {
                pause();
            }
        });

        pause();

        previousButton.addActionListener(event -> movePrevious());
        nextButton.addActionListener(event -> moveNext());

        var shuffleIcon = (FlatSVGIcon)shuffleButton.getIcon();

        shuffleIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> {
            if (shuffleButton.isSelected() || shuffleButton.getModel().isPressed()) {
                return UIManager.getColor("Slider.thumbColor");
            } else {
                return UILoader.getColor("Button.foreground");
            }
        }));

        shuffleButton.addChangeListener(event -> toggleShuffle());

        var repeatIcon = (FlatSVGIcon)repeatButton.getIcon();

        repeatIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> {
            if (repeatButton.isSelected() || repeatButton.getModel().isPressed()) {
                return UIManager.getColor("Slider.thumbColor");
            } else {
                return UILoader.getColor("Button.foreground");
            }
        }));

        repeatButton.addChangeListener(event -> toggleRepeat());

        queueButton.addActionListener(event -> showQueue());

        addSongsMenuItem.addActionListener(event -> addSongs());
        addPlaylistMenuItem.addActionListener(event -> addPlaylist());

        // TODO
        elapsedTimeLabel.setText("00:00");
        remainingTimeLabel.setText("-00:00");

        searchButton.addActionListener(event -> showSearchDialog());

        settingsButton.addActionListener(event -> showSettingsDialog());

        artistList.setCellRenderer(new ArtistCellRenderer());

        artistList.addListSelectionListener(event -> {
            if (event.getValueIsAdjusting() || !artistList.isFocusOwner()) {
                return;
            }

            showSelectedCollection();
        });

        artistList.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                showSelectedCollection();
            }
        });

        playlistList.setCellRenderer(new PlaylistCellRenderer());

        playlistList.addListSelectionListener(event -> {
            if (event.getValueIsAdjusting() || !playlistList.isFocusOwner()) {
                return;
            }

            showSelectedCollection();
        });

        playlistList.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                showSelectedCollection();
            }
        });

        loadArtists();
        loadPlaylists();

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

        if (!artists.isEmpty()) {
            artistList.setSelectedIndex(0);
        }
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

        if (!playlists.isEmpty()) {
            playlistList.setSelectedIndex(0);
        }
    }

    private void play() {
        playPauseButton.setIcon(pauseIcon);
        playPauseButton.setToolTipText(resourceBundle.getString("pause"));

        // TODO

        playing = true;
    }

    private void pause() {
        playPauseButton.setIcon(playIcon);
        playPauseButton.setToolTipText(resourceBundle.getString("play"));

        // TODO

        playing = false;
    }

    private void movePrevious() {
        // TODO
    }

    private void moveNext() {
        // TODO
    }

    private void toggleShuffle() {
        // TODO
    }

    private void toggleRepeat() {
        // TODO
    }

    private void showQueue() {
        // TODO
    }

    private void addSongs() {
        // TODO
    }

    private void addPlaylist() {
        // TODO
    }

    private void skipToPosition() {
        // TODO
    }

    private void showSearchDialog() {
        var searchDialog = new SearchDialog(this);

        searchDialog.pack();
        searchDialog.setLocationRelativeTo(this);

        searchDialog.setVisible(true);

        selectedSong = searchDialog.getSelectedSong();

        if (selectedSong != null) {
            var artist = selectedSong.getArtist();

            var artistListModel = artistList.getModel();

            var n = artistListModel.getSize();

            for (var i = 0; i < n; i++) {
                if (artistListModel.getElementAt(i).getName().equals(artist)) {
                    artistList.setSelectedIndex(i);
                    artistList.requestFocus();

                    break;
                }
            }
        }
    }

    private void showSettingsDialog() {
        // TODO
    }

    private void showSelectedCollection() {
        CollectionDetailPanel collectionDetailPanel;
        if (artistList.isFocusOwner()) {
            var artistDetailPanel = new ArtistDetailPanel(artistList.getSelectedValue());

            if (selectedSong != null) {
                SwingUtilities.invokeLater(() -> {
                    artistDetailPanel.scrollToSong(selectedSong);

                    selectedSong = null;
                });
            }

            collectionDetailPanel = artistDetailPanel;
        } else if (playlistList.isFocusOwner()) {
            collectionDetailPanel = new PlaylistDetailPanel(playlistList.getSelectedValue());
        } else {
            collectionDetailPanel = null;
        }

        if (collectionDetailPanel != null) {
            collectionDetailPanel.load();
        }

        collectionScrollPane.setViewportView(collectionDetailPanel);
    }

    public static void main(String[] args) throws Exception {
        FlatDarkLaf.setup();

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

    public static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(String.format("jdbc:sqlite:%s", dbFile.toAbsolutePath()));
    }

    public static Image getAlbumArtwork(Artist artist, String name) {
        var path = rootDirectory.resolve("media")
            .resolve(artist.getName())
            .resolve(name)
            .resolve("artwork.jpg");

        try (var inputStream = Files.newInputStream(path)) {
            return ImageIO.read(inputStream);
        } catch (IOException exception) {
            return null;
        }
    }
}
