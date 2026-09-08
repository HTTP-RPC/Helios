// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.sierra.BasicTableModel;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.UILoader;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.util.List;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Optionals.*;

public class GenreDetailPanel extends StackPanel {
    private static class GenreCellRenderer extends JLabel implements TableCellRenderer {
        GenreCellRenderer() {
            setText("A");
            setOpaque(true);

            setBorder(new EmptyBorder(4, 4, 4, 4));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
            Object value,
            boolean selected, boolean hasFocus,
            int row, int column) {
            setText(map(value, Object::toString));

            Color background;
            Color foreground;
            if (selected) {
                background = table.getSelectionBackground();
                foreground = table.getSelectionForeground();
            } else {
                background = table.getBackground();
                foreground = table.getForeground();
            }

            setBackground(background);
            setForeground(foreground);

            return this;
        }
    }

    private @Outlet JLabel nameLabel = null;
    private @Outlet JButton playAllButton = null;

    private @Outlet JButton editSelectedSongButton = null;

    private @Outlet JTable songTable = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(GenreDetailPanel.class.getName());

    public GenreDetailPanel(Genre genre, List<Song> songs) {
        add(UILoader.load(this, "GenreDetailPanel.xml", resourceBundle));

        nameLabel.setText(genre.getName());

        playAllButton.addActionListener(event -> MainFrame.getInstance().playAll(songs));

        editSelectedSongButton.addActionListener(event -> editSelectedSong());
        editSelectedSongButton.setEnabled(false);

        var songTableHeader = songTable.getTableHeader();

        songTableHeader.setReorderingAllowed(false);
        songTableHeader.setResizingAllowed(false);

        songTable.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        songTable.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

        songTable.setModel(new BasicTableModel<>(Song.class, songs,
            listOf("album", "title", "artist"),
            resourceBundle));

        var genreCellRenderer = new GenreCellRenderer();

        songTable.setDefaultRenderer(Object.class, genreCellRenderer);
        songTable.setRowHeight(genreCellRenderer.getPreferredSize().height);

        songTable.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }

            editSelectedSongButton.setEnabled(songTable.getSelectionModel().getMinSelectionIndex() != -1);
        });

        setBorder(new EmptyBorder(8, 8, 8, 8));

        setScrollableTracksViewportWidth(true);
        setScrollableTracksViewportHeight(true);
    }

    @SuppressWarnings("unchecked")
    private void editSelectedSong() {
        var song = ((BasicTableModel<Song>)songTable.getModel()).getRow(songTable.getSelectedRow());

        var editSongDialog = new EditSongDialog(MainFrame.getInstance(), song);

        editSongDialog.pack();
        editSongDialog.setLocationRelativeTo(editSongDialog.getOwner());

        editSongDialog.setVisible(true);
    }
}
