package org.httprpc.helios;

import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.UILoader;

import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.util.ResourceBundle;

public class SongDetailPanel extends StackPanel {
    private Song song;

    private @Outlet JLabel titleLabel = null;

    private @Outlet JButton playSongButton = null;
    private @Outlet JButton editButton = null;

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
    }

    private void playSong() {
        // TODO
    }

    private void showEditSongDialog() {
        var editSongDialog = new EditSongDialog(MainFrame.getInstance(), song);

        editSongDialog.pack();
        editSongDialog.setLocationRelativeTo(editSongDialog.getOwner());

        editSongDialog.setVisible(true);

        // TODO Update title label

        // TODO Update song instance
    }

    private void showButtons() {
        playSongButton.setVisible(true);
        editButton.setVisible(true);
    }

    private void hideButtons() {
        playSongButton.setVisible(false);
        editButton.setVisible(false);
    }
}
