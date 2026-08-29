package org.httprpc.helios;

import org.httprpc.sierra.BasicListModel;
import org.httprpc.sierra.ImagePane;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.UILoader;

import javax.swing.JLabel;
import javax.swing.JList;
import java.util.List;

public class AlbumDetailPanel extends StackPanel {
    private @Outlet ImagePane imagePane = null;

    private @Outlet JLabel nameLabel = null;
    private @Outlet JList<Song> songList = null;

    public AlbumDetailPanel(Artist artist, String name, List<Song> songs) {
        add(UILoader.load(this, "AlbumDetailPanel.xml"));

        imagePane.setImage(MainFrame.getAlbumArtwork(artist, name));

        nameLabel.setText(name);

        songList.setModel(new BasicListModel<>(songs));
    }
}
