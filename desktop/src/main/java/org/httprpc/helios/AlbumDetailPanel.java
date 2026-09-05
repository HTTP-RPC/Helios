// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.sierra.ColumnPanel;
import org.httprpc.sierra.ImagePane;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.RowPanel;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.UILoader;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ResourceBundle;

public class AlbumDetailPanel extends StackPanel {
    private String name;

    private @Outlet JLabel nameLabel = null;
    private @Outlet JButton playAlbumButton = null;

    private @Outlet StackPanel artworkPanel = null;

    private @Outlet RowPanel artworkButtonPanel = null;

    private @Outlet JButton downloadArtworkButton = null;
    private @Outlet JButton editArtworkButton = null;
    private @Outlet JButton deleteArtworkButton = null;

    private @Outlet ImagePane artworkImagePane = null;

    private @Outlet JLabel genreLabel = null;
    private @Outlet JLabel yearLabel = null;

    private @Outlet ColumnPanel songListPanel = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(AlbumDetailPanel.class.getName());

    public AlbumDetailPanel(String name, Image artwork, List<Song> songs) {
        this.name = name;

        add(UILoader.load(this, "AlbumDetailPanel.xml", resourceBundle));

        nameLabel.setText(name);

        playAlbumButton.addActionListener(event -> MainFrame.getInstance().playAll(songs));

        downloadArtworkButton.addActionListener(event -> downloadArtwork());
        downloadArtworkButton.setVisible(false);

        editArtworkButton.addActionListener(event -> editArtwork());
        editArtworkButton.setVisible(false);

        deleteArtworkButton.addActionListener(event -> deleteArtwork());
        deleteArtworkButton.setVisible(false);

        artworkPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                showButtons();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                var rectangle = new Rectangle(0, 0, artworkPanel.getWidth(), artworkPanel.getHeight());

                if (!rectangle.contains(event.getPoint())) {
                    hideButtons();
                }
            }
        });

        downloadArtworkButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                hideButtons();
            }
        });

        editArtworkButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                hideButtons();
            }
        });

        deleteArtworkButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                hideButtons();
            }
        });

        artworkImagePane.setImage(artwork);

        for (var song : songs) {
            var genre = song.getGenre();

            if (genre != null) {
                genreLabel.setText(genre);
                break;
            }
        }

        for (var song : songs) {
            var year = song.getYear();

            if (year != null) {
                yearLabel.setText(String.valueOf(year));
                break;
            }
        }

        songListPanel.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")),
            new EmptyBorder(2, 0, 0, 0)
        ));

        for (var song : songs) {
            songListPanel.add(new SongDetailPanel(song));
        }
    }

    private void downloadArtwork() {
        // TODO
    }

    private void editArtwork() {
        // TODO
    }

    private void deleteArtwork() {
        // TODO
    }

    private void showButtons() {
        artworkButtonPanel.setOpaque(true);

        downloadArtworkButton.setVisible(true);
        editArtworkButton.setVisible(true);
        deleteArtworkButton.setVisible(true);
    }

    private void hideButtons() {
        artworkButtonPanel.setOpaque(false);

        downloadArtworkButton.setVisible(false);
        editArtworkButton.setVisible(false);
        deleteArtworkButton.setVisible(false);
    }

    public boolean matches(String name) {
        return this.name.equals(name);
    }
}
