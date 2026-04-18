package com.fa.core.utils;

import com.day.cq.wcm.api.Page;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CategoryUtils {

    private static final String ARTICLES_NODE = "articles";

    private CategoryUtils() {}

    /**
     * Returns an ordered slug → display-title map for all nav category pages directly
     * under {@code langRoot}. Titles come from {@code jcr:title}, so they are naturally
     * localised per language tree.
     *
     * @param langRoot    the language-root page (e.g. /content/newspaper/language-masters/en)
     * @param excludePath path of a child page to skip (e.g. the search page itself); null = skip nothing
     */
    public static Map<String, String> loadCategories(Page langRoot, String excludePath) {
        if (langRoot == null) return Collections.emptyMap();

        Map<String, String> map = new LinkedHashMap<>();
        Iterator<Page> children = langRoot.listChildren();
        while (children.hasNext()) {
            Page child = children.next();
            if (ARTICLES_NODE.equalsIgnoreCase(child.getName())) continue;
            if (child.getPath().equals(excludePath)) continue;
            if (child.getProperties().get("hideInNav", false)) continue;
            String title = child.getTitle();
            map.put(child.getName(), title != null ? title : child.getName());
        }
        return Collections.unmodifiableMap(map);
    }
}
