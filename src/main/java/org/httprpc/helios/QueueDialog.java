/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.httprpc.helios;

import org.httprpc.sierra.BasicListModel;
import org.httprpc.sierra.ColumnPanel;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.UILoader;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.time.Duration;
import java.util.List;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.*;

public class QueueDialog extends AbstractDialog {
    private static class SongCellRenderer extends ColumnPanel implements ListCellRenderer<Song> {
        JLabel titleTimeLabel;
        JLabel artistAlbumLabel;

        SongCellRenderer() {
            setOpaque(true);

            add(new JLabel(), label -> titleTimeLabel = label);
            add(new JLabel(), label -> {
                label.putClientProperty("FlatLaf.styleClass", "small");

                artistAlbumLabel = label;
            });

            setBorder(new EmptyBorder(4, 8, 4, 8));
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

            artistAlbumLabel.setEnabled(selected);

            return this;
        }
    }

    private List<Song> queue;

    private @Outlet JList<Song> queueList = null;

    private static ResourceBundle resourceBundle = ResourceBundle.getBundle(QueueDialog.class.getName());

    public QueueDialog(MainFrame owner, List<Song> queue, int queueIndex) {
        super(owner, false);

        this.queue = queue;

        setTitle(resourceBundle.getString("windowTitle"));

        setContentPane(UILoader.load(this, "QueueDialog.xml", resourceBundle));

        queueList.setCellRenderer(new SongCellRenderer());

        update(queueIndex);

        setResizable(false);
    }

    public void update(int queueIndex) {
        var n = queue.size();

        if (queueIndex < n) {
            queueList.setModel(new BasicListModel<>(queue.subList(queueIndex + 1, n)));
        } else {
            queueList.setModel(new BasicListModel<>(emptyListOf(Song.class)));
        }
    }
}
