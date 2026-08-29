package org.httprpc.helios;

import org.httprpc.sierra.ColumnPanel;

import javax.swing.border.EmptyBorder;

public abstract class CollectionDetailPanel extends ColumnPanel {
    public CollectionDetailPanel() {
        setSpacing(4);

        setBorder(new EmptyBorder(8, 8, 8, 8));
    }

    public abstract void load();
}
