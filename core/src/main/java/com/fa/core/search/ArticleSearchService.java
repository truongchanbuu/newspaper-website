package com.fa.core.search;

import java.util.List;

/**
 * OSGi service contract for article search.
 * The production implementation (JCR query / Oak index) is owned by a separate team.
 * {@link com.fa.core.search.impl.MockArticleSearchService} provides mock data until it ships.
 */
public interface ArticleSearchService {

    /**
     * @param searchRoot  language-root JCR path, e.g. {@code /content/language-masters/en}
     * @param query       free-text query against title, description, and tags; may be blank
     * @param category    primary-tag value filter (e.g. "politics"); {@code null} = all
     * @param dateFrom    ISO-8601 date lower bound {@code yyyy-MM-dd}; {@code null} = unbounded
     * @param dateTo      ISO-8601 date upper bound {@code yyyy-MM-dd}; {@code null} = unbounded
     * @param offset      zero-based pagination offset
     * @param limit       maximum number of results to return
     */
    List<SearchResultItem> search(String searchRoot, String query, String category,
                                      String dateFrom, String dateTo,
                                      int offset, int limit);

    long count(String searchRoot, String query, String category,
               String dateFrom, String dateTo);

    /** Distinct primary-tag values present under {@code searchRoot}, sorted alphabetically. */
    List<String> getCategories(String searchRoot);
}
