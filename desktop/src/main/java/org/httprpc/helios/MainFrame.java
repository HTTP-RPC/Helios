package org.httprpc.helios;

import com.formdev.flatlaf.FlatLightLaf;
import org.httprpc.sierra.UILoader;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.util.ResourceBundle;

public class MainFrame extends JFrame implements Runnable {
    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(MainFrame.class.getName());

    private MainFrame() {
        super(resourceBundle.getString("title"));

        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    @Override
    public void run() {
        setContentPane(UILoader.load(this, "MainFrame.xml", resourceBundle));

        pack();
        setVisible(true);
    }

    public static void main(String[] args) {
        FlatLightLaf.setup();

        SwingUtilities.invokeLater(new MainFrame());
    }
}
