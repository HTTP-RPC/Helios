package org.httprpc.helios;

import javax.swing.ListModel;
import javax.swing.event.ListDataListener;
import java.util.List;

public class PlaylistListModel implements ListModel<ExpandedPlaylist> {
    private List<ExpandedPlaylist> playlists;

    public PlaylistListModel(List<ExpandedPlaylist> playlists) {
        this.playlists = playlists;
    }

    @Override
    public int getSize() {
        return playlists.size();
    }

    @Override
    public ExpandedPlaylist getElementAt(int index) {
        return playlists.get(index);
    }

    @Override
    public void addListDataListener(ListDataListener listener) {
        // No-op
    }

    @Override
    public void removeListDataListener(ListDataListener listener) {
        // No-op
    }
}
