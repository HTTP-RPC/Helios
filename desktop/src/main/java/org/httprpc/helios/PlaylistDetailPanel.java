package org.httprpc.helios;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.httprpc.sierra.RowPanel;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import java.util.ResourceBundle;

public class PlaylistDetailPanel extends CollectionDetailPanel {
    private Playlist playlist;

    private JLabel nameLabel = new JLabel();

    private JButton playAllButton = new JButton();

    private JTable songTable = new JTable();

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(PlaylistDetailPanel.class.getName());

    public PlaylistDetailPanel(Playlist playlist) {
        this.playlist = playlist;
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

        playlistNamePanel.add(playAllButton);

        add(playlistNamePanel);

        // TODO

        add(songTable);
    }
}
