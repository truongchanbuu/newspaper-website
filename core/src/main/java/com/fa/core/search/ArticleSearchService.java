package com.fa.core.search;

import com.fa.core.models.dto.SearchResultItem;

import java.util.List;

/**
 * OSGi service contract for article search.
 *
 * <p>Articles are CQ Pages stored under
 * {@code /content/newspaper/language-masters/<lang>/articles/<yyyy>/<mm>/<dd>/<name>}.
 * Each article's data lives in its {@code jcr:content} node:
 * <ul>
 *   <li>{@code jcr:title}          → {@link SearchResultItem#getArticleTitle()}</li>
 *   <li>{@code jcr:description}    → {@link SearchResultItem#getSummary()}</li>
 *   <li>{@code primaryTag}         → {@link SearchResultItem#getCategory()}</li>
 *   <li>{@code cq:tags}            — all tags (included in free-text query match)</li>
 *   <li>{@code image/fileReference}→ {@link SearchResultItem#getThumbnail()}</li>
 *   <li>{@code articleAuthor}      → {@link SearchResultItem#getAuthorName()}</li>
 *   <li>{@code articleDate}        → {@link SearchResultItem#getPublishDate()}</li>
 *   <li>JCR node path              → {@link SearchResultItem#getPath()} (required for article links)</li>
 * </ul>
 *
 * <p>The production implementation (JCR query / Oak index) is owned by a separate team.
 * {@link com.fa.core.search.impl.MockArticleSearchService} provides mock data until it ships.
 */
public interface ArticleSearchService {

    /**
     * @param searchRoot  language-root JCR path, e.g.
     *                    {@code /content/newspaper/language-masters/en}
     * @param query       free-text matched against {@code jcr:title}, {@code jcr:description},
     *                    and {@code cq:tags}; may be blank
     * @param category    filter on the {@code primaryTag} page property (e.g. "politics");
     *                    {@code null} = all categories
     * @param dateFrom    ISO-8601 lower bound on {@code articleDate} ({@code yyyy-MM-dd});
     *                    {@code null} = unbounded
     * @param dateTo      ISO-8601 upper bound on {@code articleDate} ({@code yyyy-MM-dd});
     *                    {@code null} = unbounded
     * @param offset      zero-based pagination offset
     * @param limit       maximum number of results to return
     */
    List<SearchResultItem> search(String searchRoot, String query, String category,
                                  String dateFrom, String dateTo,
                                  int offset, int limit);

    long count(String searchRoot, String query, String category,
               String dateFrom, String dateTo);

    /**
     * Distinct {@code primaryTag} values present on article pages under
     * {@code searchRoot}, sorted alphabetically.
     */
    List<String> getCategories(String searchRoot);
}
