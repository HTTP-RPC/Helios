// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.sierra.Outlet;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.TaskExecutor;
import org.httprpc.sierra.UILoader;

import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.RootPaneContainer;
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

        ((RootPaneContainer)getTopLevelAncestor()).getGlassPane().setVisible(true);

        setVisible(paths.size() > 1);

        addNext();
    }

    private void addNext() {
        index++;

        progressBar.setValue(index);

        if (index < paths.size()) {
            taskExecutor.execute(() -> {
                Library.addSong(paths.get(index));

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
        ((RootPaneContainer)getTopLevelAncestor()).getGlassPane().setVisible(false);

        setVisible(false);

        var mainFrame = MainFrame.getInstance();

        mainFrame.loadArtists();
        mainFrame.loadGenres();
    }
}
