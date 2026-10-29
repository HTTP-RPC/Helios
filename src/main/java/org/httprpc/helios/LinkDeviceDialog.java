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

import org.httprpc.sierra.ActivityIndicator;
import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.UILoader;

import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.Window;
import java.util.ResourceBundle;

public class LinkDeviceDialog extends AbstractDialog {
    private @Outlet JLabel codeLabel = null;
    private @Outlet ActivityIndicator activityIndicator = null;

    private @Outlet JButton cancelButton = null;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(LinkDeviceDialog.class.getName());

    public LinkDeviceDialog(Window owner) {
        super(owner, true);

        setContentPane(UILoader.load(this, "LinkDeviceDialog.xml", resourceBundle));

        var code = (int)Math.round(Math.random() * 9999);

        codeLabel.setText(String.format("%04d", code));

        activityIndicator.start();

        cancelButton.addActionListener(event -> cancel());
    }
}
