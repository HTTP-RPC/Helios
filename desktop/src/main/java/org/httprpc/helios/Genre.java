// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.kilo.sql.Column;
import org.httprpc.kilo.sql.PrimaryKey;
import org.httprpc.kilo.sql.Table;

import static org.httprpc.kilo.util.Optionals.*;

@Table("Genre")
public class Genre {
    private String name;
    private String sortableName;

    @Column("name")
    @PrimaryKey
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
