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
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.TaskExecutor;
import org.httprpc.sierra.UILoader;

import javax.swing.JButton;
import javax.swing.JProgressBar;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;

public class ImportStatusPanel extends StackPanel {
    private @Outlet JProgressBar progressBar = null;

    private @Outlet JButton cancelButton = null;

    private List<Path> paths = null;

    private int index = -1;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(ImportStatusPanel.class.getName());

    private static final TaskExecutor taskExecutor = new TaskExecutor(Executors.newSingleThreadExecutor(runnable -> {
        var thread = new Thread(runnable);

        thread.setDaemon(true);

        return thread;
    }));

    public ImportStatusPanel() {
        add(UILoader.load(this, "ImportStatusPanel.xml", resourceBundle));

        cancelButton.addActionListener(event -> cancel());
    }

    public void addAll(List<Path> paths) {
        this.paths = paths;

        index = -1;

        progressBar.setMinimum(0);
        progressBar.setMaximum(paths.size());

        MainFrame.getInstance().getGlassPane().setVisible(true);

        setVisible(paths.size() > 1);

        addNext();
    }

    private void addNext() {
        index++;

        progressBar.setValue(index);

        if (index < paths.size()) {
            taskExecutor.execute(() -> {
                MusicLibrary.addSong(paths.get(index));

                return null;
            }, (result, exception) -> addNext());
        } else {
            close();
        }
    }

    private void cancel() {
        index = paths.size();

        cancelButton.setEnabled(false);
    }

    private void close() {
        setVisible(false);

        var mainFrame = MainFrame.getInstance();

        mainFrame.getGlassPane().setVisible(false);

        mainFrame.loadArtists();
        mainFrame.loadGenres();
    }
}
