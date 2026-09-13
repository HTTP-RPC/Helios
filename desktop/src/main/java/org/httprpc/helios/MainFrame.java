// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.httprpc.sierra.ActivityIndicator;
import org.httprpc.sierra.BasicListModel;
import org.httprpc.sierra.ColumnPanel;
import org.httprpc.sierra.ImagePane;
import org.httprpc.sierra.MenuButton;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.TaskExecutor;
import org.httprpc.sierra.UILoader;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;
import static org.httprpc.kilo.util.Optionals.*;

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
            countLabel.setEnabled(false);
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

    private static class GenreCellRenderer extends CollectionCellRenderer<ExpandedGenre> {
        @Override
        protected String getName(ExpandedGenre value) {
            return value.getName();
        }

        @Override
        protected String getCountText(ExpandedGenre value) {
            var artistCount = coalesce(value.getArtistCount(), () -> 0);
            var songCount = coalesce(value.getSongCount(), () -> 0);

            return String.format(resourceBundle.getString("countFormat"),
                String.format(resourceBundle.getString(artistCount == 1 ? "singleArtistFormat" : "multipleArtistFormat"), artistCount),
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
            var artistCount = coalesce(value.getArtistCount(), () -> 0);
            var songCount = coalesce(value.getSongCount(), () -> 0);

            return String.format(resourceBundle.getString("countFormat"),
                String.format(resourceBundle.getString(artistCount == 1 ? "singleArtistFormat" : "multipleArtistFormat"), artistCount),
                String.format(resourceBundle.getString(songCount == 1 ? "singleSongFormat" : "multipleSongFormat"), songCount));
        }
    }

    private static class CollectionListSelectionModel extends DefaultListSelectionModel {
        CollectionListSelectionModel() {
            setSelectionMode(SINGLE_SELECTION);
        }

        @Override
        public void removeSelectionInterval(int index0, int index1) {
            // No-op
        }
    }

    private @Outlet JButton playPauseButton = null;

    private @Outlet JButton previousButton = null;
    private @Outlet JButton nextButton = null;

    private @Outlet JToggleButton shuffleButton = null;
    private @Outlet JToggleButton repeatButton = null;

    private @Outlet JToggleButton queueButton = null;

    private @Outlet MenuButton addButton = null;
    private @Outlet JMenuItem addSongsMenuItem = null;
    private @Outlet JMenuItem addPlaylistMenuItem = null;

    private @Outlet ColumnPanel currentSongPanel = null;

    private @Outlet ImagePane artworkImagePane = null;

    private @Outlet JLabel songTitleLabel = null;
    private @Outlet JLabel artistAlbumLabel = null;
    private @Outlet JLabel timeLabel = null;

    private @Outlet JProgressBar positionProgressBar = null;

    private @Outlet ActivityIndicator albumArtworkActivityIndicator = null;
    private @Outlet JButton getAlbumArtworkButton = null;

    private @Outlet JButton searchButton = null;
    private @Outlet JButton settingsButton = null;

    private @Outlet JTabbedPane collectionTabbedPane = null;

    private @Outlet JScrollPane artistScrollPane = null;
    private @Outlet JList<ExpandedArtist> artistList = null;

    private @Outlet JScrollPane genreScrollPane = null;
    private @Outlet JList<ExpandedGenre> genreList = null;

    private @Outlet JScrollPane playlistScrollPane = null;
    private @Outlet JList<ExpandedPlaylist> playlistList = null;

    private @Outlet JScrollPane collectionScrollPane = null;

    private ImportStatusPanel importStatusPanel = new ImportStatusPanel();

    private List<ExpandedPlaylist> playlists = listOf();

    private List<Song> songs = emptyListOf(Song.class);

    private List<Song> queue = listOf();
    private int nextSongIndex = 0;

    private AudioPlayer audioPlayer = null;
    private long lastTime = 0;

    private Timer timer = new Timer(250, event -> updatePosition());

    private QueueDialog queueDialog = null;

    private static MainFrame instance = null;

    public static final String DARK_MODE_KEY = "darkMode";

    public static final String MP3_EXTENSION = ".mp3";
    public static final String M4A_EXTENSION = ".m4a";

    private static final String PLAY_PAUSE_KEY = "playPause";
    private static final String PREVIOUS_KEY = "previous";
    private static final String NEXT_KEY = "next";
    private static final String SHUFFLE_KEY = "shuffle";
    private static final String REPEAT_KEY = "repeat";
    private static final String QUEUE_KEY = "queue";
    private static final String ADD_KEY = "add";
    private static final String GET_ALBUM_ARTWORK_KEY = "getAlbumArtwork";
    private static final String SEARCH_KEY = "search";
    private static final String SETTINGS_KEY = "settings";
    private static final String ARTISTS_KEY = "artists";
    private static final String GENRES_KEY = "genres";
    private static final String PLAYLISTS_KEY = "playlists";

    private static final String LOCATION_X_KEY = "locationX";
    private static final String LOCATION_Y_KEY = "locationY";
    private static final String SIZE_WIDTH_KEY = "sizeWidth";
    private static final String SIZE_HEIGHT_KEY = "sizeHeight";

    private static final int ARTIST_TAB_INDEX = 0;
    private static final int GENRE_TAB_INDEX = 1;
    private static final int PLAYLIST_TAB_INDEX = 2;

    private static final FlatSVGIcon playIcon = new FlatSVGIcon(MainFrame.class.getResource("icons/play_arrow_24dp.svg")).derive(32, 32);
    private static final FlatSVGIcon pauseIcon = new FlatSVGIcon(MainFrame.class.getResource("icons/pause_24dp.svg")).derive(32, 32);

    static {
        var playButtonColorFilter = new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground"));

        playIcon.setColorFilter(playButtonColorFilter);
        pauseIcon.setColorFilter(playButtonColorFilter);
    }

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(MainFrame.class.getName());

    private static final Preferences preferences = Preferences.userRoot().node(MainFrame.class.getName());

    private static final TaskExecutor taskExecutor = new TaskExecutor(Executors.newCachedThreadPool(runnable -> {
        var thread = new Thread(runnable);

        thread.setDaemon(true);

        return thread;
    }));

    private MainFrame() {
        super(resourceBundle.getString("title"));

        setDefaultCloseOperation(EXIT_ON_CLOSE);

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

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, shortcutModifier, false), GET_ALBUM_ARTWORK_KEY);
        actionMap.put(GET_ALBUM_ARTWORK_KEY, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                getAlbumArtworkButton.doClick();
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

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_1, shortcutModifier, false), ARTISTS_KEY);
        actionMap.put(ARTISTS_KEY, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                collectionTabbedPane.setSelectedIndex(ARTIST_TAB_INDEX);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_2, shortcutModifier, false), GENRES_KEY);
        actionMap.put(GENRES_KEY, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                collectionTabbedPane.setSelectedIndex(GENRE_TAB_INDEX);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_3, shortcutModifier, false), PLAYLISTS_KEY);
        actionMap.put(PLAYLISTS_KEY, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                collectionTabbedPane.setSelectedIndex(PLAYLIST_TAB_INDEX);
            }
        });
    }

    public static MainFrame getInstance() {
        return instance;
    }

    public List<ExpandedPlaylist> getPlaylists() {
        return playlists;
    }

    @Override
    public void run() {
        setContentPane(UILoader.load(this, "MainFrame.xml", resourceBundle));

        playPauseButton.addActionListener(event -> {
            if (audioPlayer == null || !audioPlayer.isPlaying()) {
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

        shuffleButton.addActionListener(event -> {
            if (!songs.isEmpty()) {
                playAll(songs);
            }
        });

        var repeatIcon = (FlatSVGIcon)repeatButton.getIcon();

        repeatIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> {
            if (repeatButton.isSelected() || repeatButton.getModel().isPressed()) {
                return UIManager.getColor("Slider.thumbColor");
            } else {
                return UILoader.getColor("Button.foreground");
            }
        }));

        queueButton.addActionListener(event -> {
            if (queueButton.isSelected() || queueDialog != null) {
                toggleQueueDialog();
            }
        });

        updateControls();

        addSongsMenuItem.addActionListener(event -> addSongs());
        addPlaylistMenuItem.addActionListener(event -> addPlaylist());

        currentSongPanel.setVisible(false);

        songTitleLabel.setText(" ");
        artistAlbumLabel.setText(" ");
        timeLabel.setText(" ");

        positionProgressBar.setValue(0);

        getAlbumArtworkButton.addActionListener(event -> getAlbumArtwork());

        searchButton.addActionListener(event -> showSearchDialog());
        settingsButton.addActionListener(event -> showSettingsDialog());

        var tabWidth = collectionTabbedPane.getPreferredSize().width;

        collectionTabbedPane.setPreferredSize(new Dimension(tabWidth + 20, 0));

        collectionTabbedPane.addChangeListener(event -> showSelectedCollection());

        artistScrollPane.setBorder(null);

        artistList.setCellRenderer(new ArtistCellRenderer());
        artistList.setSelectionModel(new CollectionListSelectionModel());

        artistList.addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }

            if (collectionTabbedPane.getSelectedIndex() == ARTIST_TAB_INDEX) {
                showSelectedCollection();
            }
        });

        genreScrollPane.setBorder(null);

        genreList.setCellRenderer(new GenreCellRenderer());
        genreList.setSelectionModel(new CollectionListSelectionModel());

        genreList.addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }

            if (collectionTabbedPane.getSelectedIndex() == GENRE_TAB_INDEX) {
                showSelectedCollection();
            }
        });

        playlistScrollPane.setBorder(null);

        playlistList.setCellRenderer(new PlaylistCellRenderer());
        playlistList.setSelectionModel(new CollectionListSelectionModel());

        playlistList.addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }

            if (collectionTabbedPane.getSelectedIndex() == PLAYLIST_TAB_INDEX) {
                showSelectedCollection();
            }
        });

        importStatusPanel.setVisible(false);

        var glassPane = new StackPanel();

        glassPane.addMouseListener(new MouseAdapter() {});

        glassPane.add(importStatusPanel);

        setGlassPane(glassPane);

        pack();
        setMinimumSize(map(getSize(), size -> new Dimension(size.width, (int)Math.ceil(size.width * (2.0 / 3.0)))));

        setLocationRelativeTo(null);

        setLocation(preferences.getInt(LOCATION_X_KEY, getX()), preferences.getInt(LOCATION_Y_KEY, getY()));
        setSize(preferences.getInt(SIZE_WIDTH_KEY, getWidth()), preferences.getInt(SIZE_HEIGHT_KEY, getHeight()));

        loadArtists();
        loadGenres();
        loadPlaylists();

        setVisible(true);

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

        setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferHandler.TransferSupport support) {
                if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    var sourceDropActions = support.getSourceDropActions();

                    if ((sourceDropActions & DnDConstants.ACTION_COPY_OR_MOVE) > 0) {
                        support.setDropAction(DnDConstants.ACTION_COPY);

                        return true;
                    }
                }

                return false;
            }

            @Override
            public boolean importData(TransferHandler.TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }

                var transferable = support.getTransferable();

                List<?> fileList;
                try {
                    fileList = (java.util.List<?>)transferable.getTransferData(DataFlavor.javaFileListFlavor);
                } catch (UnsupportedFlavorException | IOException exception) {
                    throw new RuntimeException(exception);
                }

                var paths = listOf(flatten(fileList, element -> {
                    var root = ((File)element).toPath();

                    try (var stream = Files.walk(root)) {
                        return listOf(filter(iterableOf(stream), path -> {
                            var fileName = path.getFileName().toString();

                            return fileName.endsWith(MP3_EXTENSION) || fileName.endsWith(M4A_EXTENSION);
                        }));
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                }));

                importStatusPanel.addAll(paths);

                return true;
            }
        });
    }

    @Override
    public void dispose() {
        stop();

        super.dispose();
    }

    public void loadArtists() {
        var artists = MusicLibrary.getArtists();

        var selectedArtistName = map(artistList.getSelectedValue(), Artist::getName);

        artistList.setModel(new BasicListModel<>(artists));

        if (!artists.isEmpty()) {
            if (selectedArtistName != null) {
                artistList.setSelectedIndex(indexOf(artists, whereEqualTo(Artist::getName, selectedArtistName)));
            } else {
                artistList.setSelectedIndex(0);
            }
        }
    }

    public void loadGenres() {
        var genres = MusicLibrary.getGenres();

        var selectedGenreName = map(genreList.getSelectedValue(), Genre::getName);

        genreList.setModel(new BasicListModel<>(genres));

        if (!genres.isEmpty()) {
            if (selectedGenreName != null) {
                genreList.setSelectedIndex(indexOf(genres, whereEqualTo(Genre::getName, selectedGenreName)));
            } else {
                genreList.setSelectedIndex(0);
            }
        }
    }

    public void loadPlaylists() {
        playlists = MusicLibrary.getPlaylists();

        var selectedPlaylistName = map(playlistList.getSelectedValue(), Playlist::getName);

        playlistList.setModel(new BasicListModel<>(playlists));

        if (!playlists.isEmpty()) {
            if (selectedPlaylistName != null) {
                playlistList.setSelectedIndex(indexOf(playlists, whereEqualTo(Playlist::getName, selectedPlaylistName)));
            } else {
                playlistList.setSelectedIndex(0);
            }
        }
    }

    public void playAll(List<Song> songs) {
        stop();

        this.songs = songs;

        queue.clear();
        queue.addAll(songs);

        if (shuffleButton.isSelected()) {
            java.util.Collections.shuffle(queue);
        }

        nextSongIndex = 0;

        play();
    }

    private void play() {
        playPauseButton.setIcon(pauseIcon);
        playPauseButton.setToolTipText(resourceBundle.getString("pause"));

        if (audioPlayer == null) {
            var song = queue.get(nextSongIndex);

            var artist = song.getArtist();
            var album = song.getAlbum();

            currentSongPanel.setVisible(true);

            taskExecutor.execute(() -> {
                try (var inputStream = Files.newInputStream(MusicLibrary.getArtworkPath(artist, album))) {
                    return ImageIO.read(inputStream);
                }
            }, (image, exception) -> artworkImagePane.setImage(image));

            songTitleLabel.setText(song.getTitle());
            artistAlbumLabel.setText(String.format(resourceBundle.getString("artistAlbumFormat"), artist, album));

            positionProgressBar.setValue(0);

            positionProgressBar.setMaximum(song.getTime() * 1000);

            audioPlayer = AudioPlayer.create(MusicLibrary.getContentPath(song));

            updateControls();

            if (queueDialog != null) {
                queueDialog.update(nextSongIndex);
            }
        }

        perform(audioPlayer, AudioPlayer::play);

        lastTime = System.currentTimeMillis();

        timer.start();
    }

    private void pause() {
        playPauseButton.setIcon(playIcon);
        playPauseButton.setToolTipText(resourceBundle.getString("play"));

        perform(audioPlayer, AudioPlayer::pause);

        timer.stop();
    }

    private void movePrevious() {
        var elapsedTime = positionProgressBar.getValue();

        stop();

        if (elapsedTime < 2500 && nextSongIndex > 0) {
            nextSongIndex--;
        }

        play();
    }

    private void moveNext() {
        stop();

        nextSongIndex++;

        play();
    }

    private void stop() {
        pause();

        artworkImagePane.setImage(null);

        songTitleLabel.setText(" ");
        artistAlbumLabel.setText(" ");

        positionProgressBar.setValue(0);

        perform(audioPlayer, AudioPlayer::dispose);

        audioPlayer = null;

        lastTime = 0;
    }

    private void updatePosition() {
        if (audioPlayer.isPlaying()) {
            var currentTime = System.currentTimeMillis();

            positionProgressBar.setValue(positionProgressBar.getValue() + (int)(currentTime - lastTime));

            lastTime = currentTime;

            var elapsedDuration = Duration.ofMillis(positionProgressBar.getValue());
            var totalDuration = Duration.ofMillis(positionProgressBar.getMaximum());

            timeLabel.setText(String.format(resourceBundle.getString("timeFormat"),
                elapsedDuration.toMinutesPart(), elapsedDuration.toSecondsPart(),
                totalDuration.toMinutesPart(), totalDuration.toSecondsPart()
            ));
        } else {
            stop();

            nextSongIndex++;

            var n = queue.size();

            if (nextSongIndex == n && repeatButton.isSelected()) {
                nextSongIndex = 0;
            }

            if (nextSongIndex < n) {
                play();
            } else {
                currentSongPanel.setVisible(false);

                songs = emptyListOf(Song.class);

                queue.clear();

                nextSongIndex = 0;
            }

            updateControls();
        }
    }

    private void updateControls() {
        var n = queue.size();

        playPauseButton.setEnabled(n > 0);

        previousButton.setEnabled(playPauseButton.isEnabled());
        nextButton.setEnabled(nextSongIndex < n - 1);
    }

    private void toggleQueueDialog() {
        if (queueButton.isSelected()) {
            queueDialog = new QueueDialog(this, queue, nextSongIndex);

            queueDialog.pack();
            queueDialog.setLocationRelativeTo(this);

            var preferences = Preferences.userRoot().node(QueueDialog.class.getName());

            queueDialog.setLocation(preferences.getInt(LOCATION_X_KEY, queueDialog.getX()), preferences.getInt(LOCATION_Y_KEY, queueDialog.getY()));

            queueDialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    var location = queueDialog.getLocation();

                    preferences.putInt(LOCATION_X_KEY, location.x);
                    preferences.putInt(LOCATION_Y_KEY, location.y);

                    try {
                        preferences.flush();
                    } catch (BackingStoreException exception) {
                        // No-op
                    }
                }

                @Override
                public void windowClosed(WindowEvent event) {
                    queueButton.setSelected(false);

                    queueDialog = null;
                }
            });

            queueDialog.setVisible(true);
        } else {
            queueDialog.dispose();

            queueDialog = null;
        }
    }

    private void addSongs() {
        var fileChooser = new JFileChooser();

        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (file.isDirectory()) {
                    return true;
                } else {
                    var path = file.getPath();

                    return path.endsWith(MP3_EXTENSION) || path.endsWith(M4A_EXTENSION);
                }
            }

            @Override
            public String getDescription() {
                return resourceBundle.getString("audioFileFilterDescription");
            }
        });

        var result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            addSongs(fileChooser.getSelectedFile().toPath());
        }
    }

    private void addSongs(Path root) {
        List<Path> paths;
        try (var stream = Files.walk(root)) {
            paths = listOf(filter(iterableOf(stream), path -> {
                var fileName = path.getFileName().toString();

                return fileName.endsWith(MP3_EXTENSION) || fileName.endsWith(M4A_EXTENSION);
            }));
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        if (!paths.isEmpty()) {
            importStatusPanel.addAll(paths);
        }
    }

    private void addPlaylist() {
        collectionTabbedPane.setSelectedIndex(PLAYLIST_TAB_INDEX);

        var playlist = new ExpandedPlaylist();

        playlist.setName(resourceBundle.getString("newPlaylistName"));

        playlists.add(playlist);

        playlistList.setModel(new BasicListModel<>(playlists));
        playlistList.setSelectedIndex(playlists.size() - 1);
    }

    private void getAlbumArtwork() {
        getAlbumArtworkButton.setEnabled(false);

        albumArtworkActivityIndicator.start();

        taskExecutor.execute(() -> {
            MusicLibrary.getAlbumArtwork();

            return null;
        }, (result, exception) -> {
            getAlbumArtworkButton.setEnabled(true);

            albumArtworkActivityIndicator.stop();

            loadArtists();
        });
    }

    private void showSearchDialog() {
        var searchDialog = new SearchDialog(this);

        searchDialog.pack();
        searchDialog.setLocationRelativeTo(this);

        searchDialog.setVisible(true);

        var selectedSong = searchDialog.getSelectedSong();

        if (selectedSong == null) {
            return;
        }

        if (!selectedSong.isCompilation()) {
            var artist = selectedSong.getArtist();

            var artistListModel = artistList.getModel();

            var n = artistListModel.getSize();

            for (var i = 0; i < n; i++) {
                if (artistListModel.getElementAt(i).getName().equals(artist)) {
                    collectionTabbedPane.setSelectedIndex(ARTIST_TAB_INDEX);

                    artistList.setSelectedIndex(i);
                    artistList.ensureIndexIsVisible(i);

                    SwingUtilities.invokeLater(() -> {
                        if (collectionScrollPane.getViewport().getView() instanceof ArtistDetailPanel artistDetailPanel) {
                            artistDetailPanel.scrollToSong(selectedSong);
                        }
                    });

                    break;
                }
            }
        } else {
            var genre = selectedSong.getGenre();

            var genreListModel = genreList.getModel();

            var n = genreListModel.getSize();

            for (var i = 0; i < n; i++) {
                if (genreListModel.getElementAt(i).getName().equals(genre)) {
                    collectionTabbedPane.setSelectedIndex(GENRE_TAB_INDEX);

                    genreList.setSelectedIndex(i);
                    genreList.ensureIndexIsVisible(i);

                    SwingUtilities.invokeLater(() -> {
                        if (collectionScrollPane.getViewport().getView() instanceof GenreDetailPanel genreDetailPanel) {
                            genreDetailPanel.scrollToSong(selectedSong);
                        }
                    });

                    break;
                }
            }
        }
    }

    private void showSettingsDialog() {
        var settingsDialog = new SettingsDialog(this);

        settingsDialog.pack();
        settingsDialog.setLocationRelativeTo(this);

        settingsDialog.setVisible(true);
    }

    private void showSelectedCollection() {
        var collectionDetailPanel = switch (collectionTabbedPane.getSelectedIndex()) {
            case ARTIST_TAB_INDEX -> map(artistList.getSelectedValue(), artist -> new ArtistDetailPanel(artist, MusicLibrary.getAlbums(artist)));
            case GENRE_TAB_INDEX -> map(genreList.getSelectedValue(), genre -> new GenreDetailPanel(genre, MusicLibrary.getSongs(genre)));
            case PLAYLIST_TAB_INDEX -> map(playlistList.getSelectedValue(), playlist -> new PlaylistDetailPanel(playlist, MusicLibrary.getSongs(playlist)));
            default -> throw new UnsupportedOperationException();
        };

        collectionScrollPane.setViewportView(collectionDetailPanel);
    }

    public static void main(String[] args) throws Exception {
        if (preferences.getBoolean(DARK_MODE_KEY, true)) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }

        MusicLibrary.initialize();

        instance = new MainFrame();

        SwingUtilities.invokeLater(instance);
    }
}
