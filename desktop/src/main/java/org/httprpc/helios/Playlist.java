// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.kilo.Name;
import org.httprpc.kilo.sql.Column;
import org.httprpc.kilo.sql.PrimaryKey;
import org.httprpc.kilo.sql.Table;

import static org.httprpc.kilo.util.Optionals.*;

@Table("Playlist")
public class Playlist {
    private Integer id;

    private String name;
    private String sortableName;

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
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;

        sortableName = map(name, Library::getSortableValue);
    }

    public String getSortableName() {
        return sortableName;
    }

    @Override
    public String toString() {
        return sortableName;
    }
}
