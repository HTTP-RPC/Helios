// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.sierra.BasicListModel;
import org.httprpc.sierra.ColumnPanel;
import org.httprpc.sierra.Outlet;
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
import java.util.ResourceBundle;

public class SearchDialog extends AbstractDialog {
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

            artistAlbumLabel.setEnabled(selected);

            return this;
        }
    }

    private @Outlet JTextField titleTextField = null;

    private @Outlet JList<Song> resultList = null;

    private Song selectedSong = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(SearchDialog.class.getName());

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

    private void search() {
        resultList.setModel(new BasicListModel<>(MusicLibrary.findSongs(titleTextField.getText())));
    }

    public Song getSelectedSong() {
        return selectedSong;
    }
}
