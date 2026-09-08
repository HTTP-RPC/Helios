// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import java.util.List;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;

public class Library {
    private static List<String> articles;

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(Library.class.getName());

    static {
        articles = listOf(mapAll(iterableOf(resourceBundle.getString("articles").split(",")), article -> article.strip().toLowerCase()));
    }

    public static String getSortableValue(String value) {
        var sortableValue = value.toLowerCase().strip();

        for (var article : articles) {
            var n = article.length();

            if (sortableValue.startsWith(article)
                && n < value.length()
                && Character.isWhitespace(value.charAt(n))) {
                return sortableValue.substring(n).strip();
            }
        }

        return sortableValue;
    }
}
