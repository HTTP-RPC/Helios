// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.kilo.sql.Column;
import org.httprpc.kilo.sql.Index;
import org.httprpc.kilo.sql.Table;

@Table("ArtistAlbum")
public interface ArtistAlbum {
    @Column("artist")
    @Index(1)
    String getArtist();

    @Column("album")
    @Index(2)
    String getAlbum();
}
