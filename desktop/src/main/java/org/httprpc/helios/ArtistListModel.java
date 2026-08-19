package org.httprpc.helios;

import javax.swing.ListModel;
import javax.swing.event.ListDataListener;
import java.util.List;

public class ArtistListModel implements ListModel<ExpandedArtist> {
    private List<ExpandedArtist> artists;

    public ArtistListModel(List<ExpandedArtist> artists) {
        this.artists = artists;
    }

    @Override
    public int getSize() {
        return artists.size();
    }

    @Override
    public ExpandedArtist getElementAt(int index) {
        return artists.get(index);
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
