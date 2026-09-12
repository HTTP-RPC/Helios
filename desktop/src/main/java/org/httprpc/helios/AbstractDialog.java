// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public abstract class AbstractDialog extends JDialog {
    private static final String ESCAPE_ACTION_KEY = "escape";

    public AbstractDialog(MainFrame owner) {
        this(owner, true);
    }

    public AbstractDialog(MainFrame owner, boolean modal) {
        super(owner, modal);

        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), ESCAPE_ACTION_KEY);

        rootPane.getActionMap().put(ESCAPE_ACTION_KEY, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
}
