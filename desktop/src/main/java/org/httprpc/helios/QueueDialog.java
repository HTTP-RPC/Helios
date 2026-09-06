// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.sierra.BasicListModel;
import org.httprpc.sierra.ColumnPanel;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.UILoader;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.time.Duration;
import java.util.List;
import java.util.ResourceBundle;

public class QueueDialog extends ModalDialog {
    private static class SongCellRenderer extends ColumnPanel implements ListCellRenderer<Song> {
        JLabel titleTimeLabel = new JLabel();
        JLabel artistAlbumLabel = new JLabel();

        SongCellRenderer() {
            setOpaque(true);

            setBorder(new EmptyBorder(4, 8, 4, 8));

            add(titleTimeLabel);
            add(artistAlbumLabel);

            artistAlbumLabel.putClientProperty("FlatLaf.styleClass", "small");
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Song> list,
            Song song, int index,
            boolean selected, boolean cellHasFocus) {
            var duration = Duration.ofSeconds(song.getTime());

            titleTimeLabel.setText(String.format(resourceBundle.getString("titleTimeFormat"),
                song.getTitle(),
                duration.toMinutesPart(),
                duration.toSecondsPart()));

            artistAlbumLabel.setText(String.format(resourceBundle.getString("artistAlbumFormat"),
                song.getArtist(),
                song.getAlbum()));

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

            titleTimeLabel.setForeground(foreground);
            artistAlbumLabel.setForeground(foreground);

            return this;
        }
    }

    private @Outlet JScrollPane scrollPane = null;

    private @Outlet JList<Song> queueList = null;

    private static ResourceBundle resourceBundle = ResourceBundle.getBundle(QueueDialog.class.getName());

    public QueueDialog(MainFrame owner, List<Song> queue) {
        super(owner);

        setTitle(resourceBundle.getString("windowTitle"));

        setContentPane(UILoader.load(this, "QueueDialog.xml", resourceBundle));

        scrollPane.setBorder(null);

        queueList.setModel(new BasicListModel<>(queue));

        queueList.setCellRenderer(new SongCellRenderer());

        setResizable(false);
    }
}
