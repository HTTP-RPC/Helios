package org.httprpc.helios;

import java.util.ResourceBundle;

public class ArtistCellRenderer extends CollectionCellRenderer<ExpandedArtist> {
    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(ArtistCellRenderer.class.getName());

    @Override
    protected String getName(ExpandedArtist value) {
        return value.getName();
    }

    @Override
    protected String getCountText(ExpandedArtist value) {
        var albumCount = value.getAlbumCount();
        var songCount = value.getSongCount();

        return String.format(resourceBundle.getString("countFormat"),
            String.format(resourceBundle.getString(albumCount == 1 ? "singleAlbum" : "multipleAlbums"), albumCount),
            String.format(resourceBundle.getString(songCount == 1 ? "singleSong" : "multipleSongs"), songCount));
    }
}
