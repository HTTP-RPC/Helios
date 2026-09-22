/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.httprpc.helios;

import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.UILoader;

import javax.swing.JButton;
import javax.swing.JRadioButton;
import java.util.ResourceBundle;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class SettingsDialog extends AbstractDialog {
    private @Outlet JRadioButton lightRadioButton = null;
    private @Outlet JRadioButton darkRadioButton = null;

    private @Outlet JButton cancelButton = null;
    private @Outlet JButton okButton = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(SettingsDialog.class.getName());

    public SettingsDialog(MainFrame owner) {
        super(owner);

        setTitle(resourceBundle.getString("windowTitle"));

        setContentPane(UILoader.load(this, "SettingsDialog.xml", resourceBundle));

        cancelButton.addActionListener(event -> cancel());
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
