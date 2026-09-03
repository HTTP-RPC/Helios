// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.UILoader;

import javax.swing.JButton;
import javax.swing.JRadioButton;
import java.util.ResourceBundle;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class SettingsDialog extends ModalDialog {
    private @Outlet JRadioButton lightRadioButton = null;
    private @Outlet JRadioButton darkRadioButton = null;

    private @Outlet JButton cancelButton = null;
    private @Outlet JButton okButton = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(SettingsDialog.class.getName());

    public SettingsDialog(MainFrame owner) {
        super(owner);

        setTitle(resourceBundle.getString("windowTitle"));

        setContentPane(UILoader.load(this, "SettingsDialog.xml", resourceBundle));

        cancelButton.addActionListener(event -> dispose());
        okButton.addActionListener(event -> save());

        rootPane.setDefaultButton(okButton);

        setResizable(false);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            load();
        }

        super.setVisible(visible);
    }

    private void load() {
        var preferences = Preferences.userRoot().node(MainFrame.class.getName());

        var darkMode = preferences.getBoolean(MainFrame.DARK_MODE_KEY, true);

        if (darkMode) {
            darkRadioButton.setSelected(true);
        } else {
            lightRadioButton.setSelected(true);
        }
    }

    private void save() {
        var preferences = Preferences.userRoot().node(MainFrame.class.getName());

        preferences.putBoolean(MainFrame.DARK_MODE_KEY, darkRadioButton.isSelected());

        try {
            preferences.flush();
        } catch (BackingStoreException exception) {
            // No-op
        }

        dispose();
    }
}
