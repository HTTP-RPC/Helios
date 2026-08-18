package org.httprpc.helios;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.UILoader;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.ResourceBundle;

public class MainFrame extends JFrame implements Runnable {
    private @Outlet JButton playButton = null;

    private @Outlet JButton previousButton = null;
    private @Outlet JButton nextButton = null;

    private @Outlet JToggleButton shuffleButton = null;

    private @Outlet JLabel elapsedTimeLabel = null;
    private @Outlet JSlider positionSlider = null;
    private @Outlet JLabel remainingTimeLabel = null;

    private @Outlet JTextField searchTextField = null;

    private @Outlet JSplitPane splitPane = null;

    private @Outlet JScrollPane albumScrollPane = null;

    private FlatSVGIcon playIcon = new FlatSVGIcon(MainFrame.class.getResource("icons/play_arrow_24dp.svg"));
    private FlatSVGIcon pauseIcon = new FlatSVGIcon(MainFrame.class.getResource("icons/pause_24dp.svg"));

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(MainFrame.class.getName());

    private MainFrame() {
        super(resourceBundle.getString("title"));

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        var playButtonColorFilter = new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground"));

        playIcon.setColorFilter(playButtonColorFilter);
        pauseIcon.setColorFilter(playButtonColorFilter);
    }

    @Override
    public void run() {
        setContentPane(UILoader.load(this, "MainFrame.xml", resourceBundle));

        // TODO
        playButton.setIcon(playIcon);
        playButton.setToolTipText(resourceBundle.getString("play"));

        // TODO
        elapsedTimeLabel.setText("00:00");
        remainingTimeLabel.setText("-00:00");

        // TODO Preferences
        setSize(1024, 768);

        // TODO Preferences
        splitPane.setDividerLocation(280);

        setVisible(true);
    }

    public static void main(String[] args) {
        // TODO Preferences
        FlatDarkLaf.setup();

        SwingUtilities.invokeLater(new MainFrame());
    }
}
