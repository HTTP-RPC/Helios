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

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ResourceBundle;

public abstract class AbstractDialog extends JDialog {
    private boolean canceled = false;

    private static final String ESCAPE_ACTION_KEY = "escape";

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(AbstractDialog.class.getName());

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

        rootPane.putClientProperty("apple.awt.transparentTitleBar", true);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                cancel();
            }
        });

        getGlassPane().addMouseListener(new MouseAdapter() {});
    }

    public boolean isCanceled() {
        return canceled;
    }

    protected void cancel() {
        canceled = true;

        dispose();
    }

    protected void alertRequired(String key, JComponent component) {
        var message = String.format(resourceBundle.getString("requiredFieldFormat"),
            ResourceBundle.getBundle(getClass().getName()).getString(key));

        JOptionPane.showMessageDialog(this, message,
            resourceBundle.getString("error"),
            JOptionPane.ERROR_MESSAGE);

        component.requestFocus();
    }
}
