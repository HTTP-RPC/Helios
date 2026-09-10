// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

public enum OperatingSystem {
    MAC_OS,
    WINDOWS,
    OTHER;

    public static OperatingSystem getCurrent() {
        var name = System.getProperty("os.name").toLowerCase();

        if (name.startsWith("mac")) {
            return MAC_OS;
        } else if (name.startsWith("windows")) {
            return WINDOWS;
        } else {
            return OTHER;
        }
    }
}
