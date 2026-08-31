package org.httprpc.helios;

import javax.swing.JScrollPane;
import java.awt.Dimension;
import java.util.ResourceBundle;

public class QueueDialog extends ModalDialog {
    private static ResourceBundle resourceBundle = ResourceBundle.getBundle(QueueDialog.class.getName());

    public QueueDialog(MainFrame owner) {
        super(owner);

        setTitle(resourceBundle.getString("title"));

        var scrollPane = new JScrollPane();

        scrollPane.setPreferredSize(new Dimension(320, 480));

        setContentPane(scrollPane);

        setResizable(false);
    }
}
