// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.kilo.sql.Column;
import org.httprpc.kilo.sql.Table;

@Table("ExpandedArtist")
public interface ExpandedArtist extends Artist {
    @Column("albumCount")
    Integer getAlbumCount();

    @Column("songCount")
    Integer getSongCount();
}
