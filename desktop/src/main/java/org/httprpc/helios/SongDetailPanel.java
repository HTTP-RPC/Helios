package org.httprpc.helios;

import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.UILoader;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.*;

public class SongDetailPanel extends StackPanel {
    private Song song;

    private @Outlet JLabel titleLabel = null;

    private @Outlet JButton playSongButton = null;

    private @Outlet JButton editButton = null;
    private @Outlet JButton deleteButton = null;

    private @Outlet JLabel timeLabel  = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(SongDetailPanel.class.getName());

    public SongDetailPanel(Song song) {
        this.song = song;

        var content = UILoader.load(this, "SongDetailPanel.xml", resourceBundle);

        add(content);

        titleLabel.setText(song.getTitle());

        playSongButton.addActionListener(event -> playSong());
        playSongButton.setVisible(false);

        editButton.addActionListener(event -> showEditSongDialog());
        editButton.setVisible(false);

        deleteButton.addActionListener(event -> confirmDeleteSong());
        deleteButton.setVisible(false);

        var duration = Duration.ofSeconds(song.getTime());

        timeLabel.setText(String.format(resourceBundle.getString("timeFormat"),
            duration.toMinutesPart(),
            duration.toSecondsPart()));

        content.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                showButtons();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                if (content.getComponentAt(event.getX(), event.getY()) == null) {
                    hideButtons();
                }
            }
        });

        playSongButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                hideButtons();
            }
        });

        editButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                hideButtons();
            }
        });

        deleteButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                hideButtons();
            }
        });

        setBackground(UIManager.getColor("Component.borderColor"));
    }

    private void playSong() {
        MainFrame.getInstance().playAll(listOf(song));
    }

    private void confirmDeleteSong() {
        var result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
            String.format(resourceBundle.getString("confirmDeleteMessageFormat"), song.getTitle()),
            resourceBundle.getString("deleteSong"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            deleteSong();
        }
    }

    private void deleteSong() {
        // TODO
    }

    private void showEditSongDialog() {
        var editSongDialog = new EditSongDialog(MainFrame.getInstance(), song);

        editSongDialog.pack();
        editSongDialog.setLocationRelativeTo(editSongDialog.getOwner());

        editSongDialog.setVisible(true);
    }

    private void showButtons() {
        playSongButton.setVisible(true);

        editButton.setVisible(true);
        deleteButton.setVisible(true);

        setOpaque(true);
    }

    private void hideButtons() {
        playSongButton.setVisible(false);

        editButton.setVisible(false);
        deleteButton.setVisible(false);

        setOpaque(false);
    }
}
