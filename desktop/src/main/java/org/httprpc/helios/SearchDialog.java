// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.sql.QueryBuilder;
import org.httprpc.sierra.BasicListModel;
import org.httprpc.sierra.ColumnPanel;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.TaskExecutor;
import org.httprpc.sierra.UILoader;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Component;
import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;

public class SearchDialog extends ModalDialog {
    private static class ResultCellRenderer extends ColumnPanel implements ListCellRenderer<Song> {
        JLabel titleLabel = new JLabel();
        JLabel artistAlbumLabel = new JLabel();

        ResultCellRenderer() {
            setOpaque(true);

            setBorder(new EmptyBorder(4, 4, 4, 4));

            add(titleLabel);
            add(artistAlbumLabel);

            artistAlbumLabel.putClientProperty("FlatLaf.styleClass", "small");
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Song> list,
            Song song, int index,
            boolean selected, boolean cellHasFocus) {
            titleLabel.setText(song.getTitle());
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

            titleLabel.setForeground(foreground);
            artistAlbumLabel.setForeground(foreground);

            return this;
        }
    }

    private @Outlet JTextField titleTextField = null;

    private @Outlet JList<Song> resultList = null;

    private Song selectedSong = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(SearchDialog.class.getName());

    private static final TaskExecutor taskExecutor = new TaskExecutor(Executors.newSingleThreadExecutor(runnable -> {
        var thread = new Thread(runnable);

        thread.setDaemon(true);

        return thread;
    }));

    public SearchDialog(MainFrame owner) {
        super(owner);

        setTitle(resourceBundle.getString("windowTitle"));

        setContentPane(UILoader.load(this, "SearchDialog.xml", resourceBundle));

        titleTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                search();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                search();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                // No-op
            }
        });

        resultList.setCellRenderer(new ResultCellRenderer());

        resultList.addListSelectionListener(event -> {
            selectedSong = resultList.getSelectedValue();

            dispose();
        });

        setResizable(false);
    }

    public Song getSelectedSong() {
        return selectedSong;
    }

    private void search() {
        var text = titleTextField.getText().strip();

        if (text.isEmpty()) {
            resultList.setModel(new BasicListModel<>(listOf()));

            return;
        }

        taskExecutor.execute(() -> {
            // TODO Move to Library
            var queryBuilder = QueryBuilder.select(Song.class).filterByIndexLike("title");

            try (var connection = Library.openConnection();
                var statement = queryBuilder.prepare(connection);
                var results = queryBuilder.executeQuery(statement, mapOf(
                    entry("title", String.format("%%%s%%", text))
                ))) {
                return sortBy(mapAll(results, BeanAdapter.toType(Song.class)), Comparator.comparing(Song::getTitle)
                    .thenComparing(Song::getAlbum)
                    .thenComparing(Song::getArtist));
            }
        }, (results, exception) -> {
            if (exception == null) {
                resultList.setModel(new BasicListModel<>(results));
            }
        });
    }
}
