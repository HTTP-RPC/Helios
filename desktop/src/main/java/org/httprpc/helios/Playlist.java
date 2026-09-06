// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.kilo.Name;
import org.httprpc.kilo.sql.Column;
import org.httprpc.kilo.sql.Index;
import org.httprpc.kilo.sql.PrimaryKey;
import org.httprpc.kilo.sql.Table;

@Table("Playlist")
public class Playlist {
    private Integer id;
    private String name;

    @Name("id")
    @Column("id")
    @PrimaryKey
    public Integer getID() {
        return id;
    }

    public void setID(Integer id) {
        this.id = id;
    }

    @Column("name")
    @Index
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
