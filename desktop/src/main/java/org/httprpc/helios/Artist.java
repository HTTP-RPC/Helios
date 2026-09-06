// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import org.httprpc.kilo.sql.Column;
import org.httprpc.kilo.sql.Index;
import org.httprpc.kilo.sql.PrimaryKey;
import org.httprpc.kilo.sql.Table;

@Table("Artist")
public class Artist {
    private String name;

    @Column("name")
    @PrimaryKey
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
