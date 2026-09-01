package org.httprpc.helios;

import org.httprpc.sierra.BasicListModel;
import org.httprpc.sierra.RowPanel;
import org.httprpc.sierra.Spacer;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import java.util.ResourceBundle;

public class QueueDialog extends ModalDialog {
    private static class SongCellRenderer extends RowPanel implements ListCellRenderer<Song> {
        JLabel titleLabel = new JLabel();
        JLabel artistLabel = new JLabel();

        SongCellRenderer() {
            setOpaque(true);

            setBorder(new EmptyBorder(4, 4, 4, 4));

            add(titleLabel);
            add(new Spacer(), 1.0);
            add(artistLabel);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Song> list,
            Song song, int index,
            boolean selected, boolean cellHasFocus) {
            titleLabel.setText(song.getTitle());
            artistLabel.setText(song.getArtist());

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

            titleLabel.setForeground(foreground);
            artistLabel.setForeground(foreground);

            return this;
        }
    }

    private static ResourceBundle resourceBundle = ResourceBundle.getBundle(QueueDialog.class.getName());

    public QueueDialog(MainFrame owner, List<Song> queue) {
        super(owner);

        setTitle(resourceBundle.getString("windowTitle"));

        var queueList = new JList<>(new BasicListModel<>(queue));

        queueList.setCellRenderer(new SongCellRenderer());

        var scrollPane = new JScrollPane(queueList);

        scrollPane.setPreferredSize(new Dimension(360, 480));
        scrollPane.setBorder(null);

        setContentPane(scrollPane);

        setResizable(false);
    }
}
