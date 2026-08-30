package org.httprpc.helios;

import org.httprpc.sierra.BasicListModel;
import org.httprpc.sierra.ImagePane;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.RowPanel;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.UILoader;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.Component;
import java.time.Duration;
import java.util.List;
import java.util.ResourceBundle;

public class AlbumDetailPanel extends StackPanel {
    private static class SongCellRenderer extends RowPanel implements ListCellRenderer<Song> {
        JLabel titleLabel = new JLabel();
        JLabel timeLabel = new JLabel();

        SongCellRenderer() {
            setAlignToBaseline(true);

            setBorder(new EmptyBorder(4, 0, 4, 0));

            add(titleLabel, 1.0);
            add(timeLabel);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Song> list,
            Song song, int index,
            boolean selected, boolean cellHasFocus) {
            titleLabel.setText(song.getTitle());

            var duration = Duration.ofSeconds(song.getTime());

            timeLabel.setText(String.format(resourceBundle.getString("timeFormat"),
                duration.toMinutesPart(),
                duration.toSecondsPart()));

            return this;
        }
    }

    private @Outlet ImagePane imagePane = null;

    private @Outlet JLabel nameLabel = null;
    private @Outlet JList<Song> songList = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(AlbumDetailPanel.class.getName());

    public AlbumDetailPanel(Artist artist, String name, List<Song> songs) {
        add(UILoader.load(this, "AlbumDetailPanel.xml"));

        imagePane.setImage(MainFrame.getAlbumArtwork(artist, name));

        nameLabel.setText(name);

        songList.setCellRenderer(new SongCellRenderer());
        songList.setModel(new BasicListModel<>(songs));

        songList.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")),
            new EmptyBorder(2, 0, 0, 0)
        ));
    }
}
