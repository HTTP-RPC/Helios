package org.httprpc.helios;

import org.httprpc.sierra.ColumnPanel;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;

public abstract class CollectionCellRenderer<T> extends ColumnPanel implements ListCellRenderer<T> {
    private JLabel nameLabel = new JLabel();
    private JLabel countLabel = new JLabel();

    protected CollectionCellRenderer() {
        setOpaque(true);

        setBorder(new EmptyBorder(4, 8, 4, 8));

        add(nameLabel);
        add(countLabel);

        countLabel.putClientProperty("FlatLaf.styleClass", "mini");
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends T> list,
        T value, int index,
        boolean selected, boolean cellHasFocus) {
        nameLabel.setText(getName(value));
        countLabel.setText(getCountText(value));

        Color background;
        Color foreground;
        if (selected) {
            background = list.getSelectionBackground();
            foreground = list.getSelectionForeground();
        } else {
            background = list.getBackground();
            foreground = list.getForeground();
        }

        setBackground(background);

        nameLabel.setForeground(foreground);
        countLabel.setForeground(foreground);

        return this;
    }

    protected abstract String getName(T value);
    protected abstract String getCountText(T value);
}
