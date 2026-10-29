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

import org.httprpc.sierra.NumberField;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.UILoader;
import org.httprpc.sierra.ValidatedTextField;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JRadioButton;
import java.text.NumberFormat;
import java.util.ResourceBundle;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.httprpc.kilo.util.Optionals.*;

public class SettingsDialog extends AbstractDialog {
    private @Outlet JRadioButton systemDefaultRadioButton = null;
    private @Outlet JRadioButton lightRadioButton = null;
    private @Outlet JRadioButton darkRadioButton = null;

    private @Outlet JRadioButton nameRadioButton = null;
    private @Outlet JRadioButton yearRadioButton = null;
    private @Outlet JCheckBox descendingCheckBox = null;

    private @Outlet NumberField skipIncrementTextField = null;

    private @Outlet JCheckBox synchronizeLibraryCheckBox = null;

    private @Outlet JRadioButton hostRadioButton = null;
    private @Outlet JRadioButton joinRadioButton = null;

    private @Outlet ValidatedTextField hostNameTextField = null;
    private @Outlet NumberField portTextField = null;

    private @Outlet JList<?> linkedDeviceList = null;

    private @Outlet JButton linkDeviceButton = null;
    private @Outlet JButton removeLinkedDeviceButton = null;

    private @Outlet JButton cancelButton = null;
    private @Outlet JButton okButton = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(SettingsDialog.class.getName());

    public SettingsDialog(MainFrame owner) {
        super(owner, true);

        setTitle(resourceBundle.getString("windowTitle"));

        setContentPane(UILoader.load(this, "SettingsDialog.xml", resourceBundle));

        nameRadioButton.addActionListener(event -> updateAlbumSortOrder());
        yearRadioButton.addActionListener(event -> updateAlbumSortOrder());

        var integerFormat = NumberFormat.getIntegerInstance();

        integerFormat.setGroupingUsed(false);

        skipIncrementTextField.setFormat(integerFormat);

        synchronizeLibraryCheckBox.addActionListener(event -> updateRole());

        hostRadioButton.addActionListener(event -> updateRole());
        joinRadioButton.addActionListener(event -> updateRole());

        portTextField.setFormat(integerFormat);

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

        var darkMode = map(preferences.get(MainFrame.DARK_MODE_KEY, null), Boolean::parseBoolean);

        if (darkMode == null) {
            systemDefaultRadioButton.setSelected(true);
        } else if (darkMode) {
            darkRadioButton.setSelected(true);
        } else {
            lightRadioButton.setSelected(true);
        }

        var albumSortOrder = map(preferences.get(MainFrame.ALBUM_SORT_ORDER_KEY, null), AlbumSortOrder::valueOf);

        if (albumSortOrder == AlbumSortOrder.NAME) {
            nameRadioButton.setSelected(true);
        } else {
            yearRadioButton.setSelected(true);

            descendingCheckBox.setSelected(albumSortOrder == AlbumSortOrder.YEAR_DESCENDING);
        }

        synchronizeLibraryCheckBox.setSelected(map(preferences.get(MainFrame.SYNCHRONIZE_LIBRARY_KEY, null), Boolean::valueOf));

        var host = map(preferences.get(MainFrame.HOST_KEY, null), Boolean::valueOf);

        hostRadioButton.setSelected(host);
        joinRadioButton.setSelected(!host);

        portTextField.setValue(map(preferences.get(MainFrame.PORT_KEY, null), Integer::valueOf));

        updateAlbumSortOrder();
        updateRole();

        skipIncrementTextField.setValue(map(preferences.get(MainFrame.SKIP_INCREMENT_KEY, null), Integer::valueOf));
    }

    private void updateAlbumSortOrder() {
        if (nameRadioButton.isSelected()) {
            descendingCheckBox.setSelected(false);
            descendingCheckBox.setEnabled(false);
        } else {
            descendingCheckBox.setEnabled(true);
        }
    }

    private void updateRole() {
        var preferences = Preferences.userRoot().node(MainFrame.class.getName());

        var synchronizeLibrary = synchronizeLibraryCheckBox.isSelected();
        var host = hostRadioButton.isSelected();

        hostRadioButton.setEnabled(synchronizeLibrary);
        joinRadioButton.setEnabled(synchronizeLibrary);

        hostNameTextField.setValue(host ? null : preferences.get(MainFrame.HOST_NAME_KEY, null));
        hostNameTextField.setEnabled(!host);

        portTextField.setEnabled(synchronizeLibrary);

        // TODO Clear/populate device list
        linkedDeviceList.setEnabled(synchronizeLibrary && host);

        linkDeviceButton.setEnabled(synchronizeLibrary && host);
        removeLinkedDeviceButton.setEnabled(synchronizeLibrary && host);
    }

    private void save() {
        var preferences = Preferences.userRoot().node(MainFrame.class.getName());

        if (systemDefaultRadioButton.isSelected()) {
            preferences.remove(MainFrame.DARK_MODE_KEY);
        } else {
            preferences.putBoolean(MainFrame.DARK_MODE_KEY, darkRadioButton.isSelected());
        }

        if (nameRadioButton.isSelected()) {
            preferences.put(MainFrame.ALBUM_SORT_ORDER_KEY, AlbumSortOrder.NAME.toString());
        } else if (!descendingCheckBox.isSelected()) {
            preferences.put(MainFrame.ALBUM_SORT_ORDER_KEY, AlbumSortOrder.YEAR_ASCENDING.toString());
        } else {
            preferences.put(MainFrame.ALBUM_SORT_ORDER_KEY, AlbumSortOrder.YEAR_DESCENDING.toString());
        }

        var skipIncrement = map(skipIncrementTextField.getValue(), Number::intValue);

        if (skipIncrement == null) {
            alertRequired("skipIncrement", skipIncrementTextField);
            return;
        }

        if (skipIncrement < 1 || skipIncrement > 15) {
            alertInvalid("skipIncrement", skipIncrementTextField);
            return;
        }

        preferences.putInt(MainFrame.SKIP_INCREMENT_KEY, skipIncrement);

        var synchronizeLibrary = synchronizeLibraryCheckBox.isSelected();
        var host = hostRadioButton.isSelected();

        var hostName = hostNameTextField.getValue();

        if (!host && hostName == null) {
            alertRequired("hostName", hostNameTextField);
            return;
        }

        var port = map(portTextField.getValue(), Number::intValue);

        if (port == null) {
            alertRequired("port", portTextField);
            return;
        }

        if (port < 0 || port > 65535) {
            alertInvalid("port", portTextField);
            return;
        }

        preferences.putBoolean(MainFrame.SYNCHRONIZE_LIBRARY_KEY, synchronizeLibrary);
        preferences.putBoolean(MainFrame.HOST_KEY, host);

        if (hostName == null) {
            preferences.remove(MainFrame.HOST_NAME_KEY);
        } else {
            preferences.put(MainFrame.HOST_NAME_KEY, hostName);
        }

        preferences.putInt(MainFrame.PORT_KEY, port);

        try {
            preferences.flush();
        } catch (BackingStoreException exception) {
            // No-op
        }

        MainFrame.getInstance().loadAll();

        dispose();
    }
}
