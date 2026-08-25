package org.httprpc.helios;

import org.httprpc.kilo.sql.Column;
import org.httprpc.kilo.sql.Index;
import org.httprpc.kilo.sql.PrimaryKey;
import org.httprpc.kilo.sql.Table;

@Table("Artist")
public interface Artist {
    @Column("name")
    @PrimaryKey
    @Index
    String getName();
}
