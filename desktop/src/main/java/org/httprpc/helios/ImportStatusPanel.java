// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.UILoader;

import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.RootPaneContainer;
import java.util.ResourceBundle;

public class ImportStatusPanel extends StackPanel {
    private @Outlet JProgressBar progressBar = null;

    private @Outlet JButton cancelButton = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(ImportStatusPanel.class.getName());

    public ImportStatusPanel() {
        add(UILoader.load(this, "ImportStatusPanel.xml", resourceBundle));

        cancelButton.addActionListener(event -> cancel());
    }

    private void cancel() {
        ((RootPaneContainer)getTopLevelAncestor()).getGlassPane().setVisible(false);

        setVisible(false);

        // TODO
    }
}
