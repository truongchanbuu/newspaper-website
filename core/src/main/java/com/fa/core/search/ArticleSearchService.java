package com.fa.core.search;

import java.util.List;

/**
 * OSGi service contract for article search.
 *
 * <p>Articles are CQ Pages stored under
 * {@code /content/newspaper/language-masters/<lang>/articles/<yyyy>/<mm>/<dd>/<name>}.
 * Each article's data lives in its {@code jcr:content} node:
 * <ul>
 *   <li>{@code jcr:title}       — article headline → {@link SearchResultItem#getTitle()}</li>
 *   <li>{@code jcr:description} — article intro    → {@link SearchResultItem#getDescription()}</li>
 *   <li>{@code primaryTag}      — single category  → {@link SearchResultItem#getCategory()}</li>
 *   <li>{@code cq:tags}         — all tags (included in free-text query match)</li>
 *   <li>path {@code /articles/<yyyy>/<mm>/<dd>/} — date derived from path segments,
 *       not a stored property (always authoritative, indexed by Oak)</li>
 *   <li>{@code image/fileReference} — hero image  → {@link SearchResultItem#getImageUrl()}</li>
 *   <li>{@code articleAuthor}   — byline           → {@link SearchResultItem#getAuthor()}</li>
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
