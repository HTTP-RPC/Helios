package org.httprpc.helios;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.httprpc.sierra.RowPanel;
import org.httprpc.sierra.Spacer;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.util.ResourceBundle;

public class SongDetailPanel extends RowPanel {
    private Song song;

    private JLabel titleLabel = new JLabel();
    private JButton playSongButton = new JButton();

    private JLabel timeLabel = new JLabel();

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(SongDetailPanel.class.getName());

    public SongDetailPanel(Song song) {
        this.song = song;

        setSpacing(4);
        setAlignToBaseline(true);

        titleLabel.setText(song.getTitle());

        add(titleLabel);

        var playIcon = new FlatSVGIcon(MainFrame.class.getResource("icons/play_arrow_24dp.svg")).derive(16, 16);

        playIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground")));

        playSongButton.setIcon(playIcon);
        playSongButton.setFocusable(false);
        playSongButton.setToolTipText(resourceBundle.getString("playSong"));
        playSongButton.setVisible(false);
        playSongButton.putClientProperty("FlatLaf.style", "buttonType: borderless");

        add(playSongButton);

        add(new Spacer(), 1.0);

        var duration = Duration.ofSeconds(song.getTime());

        timeLabel.setText(String.format(resourceBundle.getString("timeFormat"),
            duration.toMinutesPart(),
            duration.toSecondsPart()));

        add(timeLabel);

        setBorder(new EmptyBorder(4, 0, 4, 0));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                playSongButton.setVisible(true);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                if (getComponentAt(event.getX(), event.getY()) != playSongButton) {
                    playSongButton.setVisible(false);
                }
            }
        });

        playSongButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                playSongButton.setVisible(false);
            }
        });
    }
}
