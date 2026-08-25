package org.httprpc.helios;

import org.httprpc.kilo.Name;
import org.httprpc.kilo.sql.Column;
import org.httprpc.kilo.sql.Index;
import org.httprpc.kilo.sql.PrimaryKey;
import org.httprpc.kilo.sql.Table;

@Table("Playlist")
public interface Playlist {
    @Name("id")
    @Column("id")
    @PrimaryKey
    Integer getID();

    @Column("name")
    @Index
    String getName();
    void setName(String name);
}
