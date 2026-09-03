// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.sierra.UILoader;

import java.util.ResourceBundle;

public class SettingsDialog extends ModalDialog {
    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(SettingsDialog.class.getName());

    public SettingsDialog(MainFrame owner) {
        super(owner);

        setTitle(resourceBundle.getString("windowTitle"));

        setContentPane(UILoader.load(this, "SettingsDialog.xml", resourceBundle));
    }
}
