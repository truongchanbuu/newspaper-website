package com.fa.core.search.impl;

import com.fa.core.search.ArticleSearchService;
import com.fa.core.search.SearchResultItem;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Temporary mock — replace with the real JCR/Oak query implementation once available.
 *
 * <p>Simulates querying CQ Pages under
 * {@code /content/newspaper/language-masters/<lang>/articles/<yyyy>/<mm>/<dd>/<name>}.
 *
 * <p>Date filtering uses the path structure directly — {@code yyyy/mm/dd} segments after
 * {@code articles/} — matching the optimization the real service should apply
 * (path segments are always authoritative and indexed by Oak without extra properties).
 *
 * <p>Text search covers {@code jcr:title}, {@code jcr:description}, {@code primaryTag},
 * and all {@code cq:tags} values.
 */
@Component(service = ArticleSearchService.class)
@ServiceDescription("Mock Article Search Service")
public class MockArticleSearchService implements ArticleSearchService {

    private static final List<SearchResultItem> MOCK_DATA = Arrays.asList(
        new SearchResultItem(
            "/content/newspaper/language-masters/en/articles/2026/04/10/uk-election-results",
            "UK Election Results: Labour Leads Coalition Talks",
            "After a historic night, Labour emerged with a majority mandate and coalition negotiations have begun.",
            "politics", "10 Apr 2026", "/content/dam/newspaper/asset.jpg", "Jane Dougherty",
            Arrays.asList("politics", "labour", "uk-politics", "election")),
        new SearchResultItem(
            "/content/newspaper/language-masters/en/articles/2026/04/11/ai-climate-research",
            "AI Tools Accelerate Climate Change Research",
            "A new generation of AI models is helping scientists predict weather patterns with unprecedented accuracy.",
            "technology", "11 Apr 2026", "/content/dam/newspaper/asset.jpg", "Marcus Elliot",
            Arrays.asList("technology", "artificial-intelligence", "climate", "science")),
        new SearchResultItem(
            "/content/newspaper/language-masters/en/articles/2026/04/12/premier-league-roundup",
            "Premier League Roundup: Title Race Goes to Final Day",
            "With only one match remaining, three clubs are still mathematically alive in the title race.",
            "sport", "12 Apr 2026", "/content/dam/newspaper/asset.jpg", "Sarah Bright",
            Arrays.asList("sport", "football", "premier-league", "manchester")),
        new SearchResultItem(
            "/content/newspaper/language-masters/en/articles/2026/04/13/global-trade-summit",
            "Global Trade Summit Ends Without Breakthrough",
            "Negotiators failed to agree on new tariff frameworks despite three days of talks in Geneva.",
            "world", "13 Apr 2026", "/content/dam/newspaper/asset.jpg", "Oliver Strand",
            Arrays.asList("world", "trade", "tariffs", "geneva", "economics")),
        new SearchResultItem(
            "/content/newspaper/language-masters/en/articles/2026/04/14/nhs-funding-plan",
            "NHS to Receive Record £12bn Funding Boost",
            "The Health Secretary announced the largest single investment in NHS infrastructure in decades.",
            "politics", "14 Apr 2026", "/content/dam/newspaper/asset.jpg", "Emma Coward",
            Arrays.asList("politics", "health", "nhs", "uk-politics")),
        new SearchResultItem(
            "/content/newspaper/language-masters/en/articles/2026/04/15/tech-startup-regulation",
            "Tech Startups Warn Against Heavy-Handed Regulation",
            "Industry leaders say new EU digital market rules could push innovation to other regions.",
            "technology", "15 Apr 2026", "/content/dam/newspaper/asset.jpg", "Tom Vickers",
            Arrays.asList("technology", "startups", "regulation", "eu", "policy")),
        new SearchResultItem(
            "/content/newspaper/language-masters/en/articles/2026/04/16/culture-award-shortlist",
            "Turner Prize Shortlist Unveiled at Tate Modern",
            "Four artists have been nominated for this year's prize, with a ceremony scheduled for November.",
            "culture", "16 Apr 2026", "/content/dam/newspaper/asset.jpg", "Lisa Park",
            Arrays.asList("culture", "art", "turner-prize", "tate", "london")),
        new SearchResultItem(
            "/content/newspaper/language-masters/en/articles/2026/04/17/economic-outlook-q2",
            "Q2 Economic Outlook: Growth Expected to Slow",
            "Analysts forecast a dip in GDP growth as energy prices and interest rates remain elevated.",
            "business", "17 Apr 2026", "/content/dam/newspaper/asset.jpg", "Henry Walsh",
            Arrays.asList("business", "economics", "gdp", "interest-rates", "inflation"))
    );

    private static final List<String> CATEGORIES = Arrays.asList(
        "business", "culture", "environment", "lifestyle", "politics", "sport", "technology", "world"
    );

    @Override
    public List<SearchResultItem> search(String searchRoot, String query, String category,
                                          String dateFrom, String dateTo,
                                          int offset, int limit) {
        List<SearchResultItem> filtered = applyFilters(query, category, dateFrom, dateTo);
        int from = Math.min(offset, filtered.size());
        int to   = Math.min(from + limit, filtered.size());
        return filtered.subList(from, to);
    }

    @Override
    public long count(String searchRoot, String query, String category,
                      String dateFrom, String dateTo) {
        return applyFilters(query, category, dateFrom, dateTo).size();
    }

    @Override
    public List<String> getCategories(String searchRoot) {
        return Collections.unmodifiableList(CATEGORIES);
    }

    // ── Filtering ─────────────────────────────────────────────────────────────

    private List<SearchResultItem> applyFilters(String query, String category,
                                                 String dateFrom, String dateTo) {
        LocalDate from = StringUtils.isNotEmpty(dateFrom) ? LocalDate.parse(dateFrom) : null;
        LocalDate to   = StringUtils.isNotEmpty(dateTo)   ? LocalDate.parse(dateTo)   : null;

        return MOCK_DATA.stream()
            .filter(r -> matchesCategory(r, category))
            .filter(r -> matchesDate(r, from, to))
            .filter(r -> matchesText(r, query))
            .collect(Collectors.toList());
    }

    private boolean matchesCategory(SearchResultItem r, String category) {
        return category == null || category.isEmpty()
               || category.equalsIgnoreCase(r.getCategory());
    }

    /**
     * Derives the article date from the path segments {@code /articles/<yyyy>/<mm>/<dd>/}
     * instead of a stored property — the same optimisation the real service should use.
     */
    private boolean matchesDate(SearchResultItem r, LocalDate from, LocalDate to) {
        if (from == null && to == null) return true;
        LocalDate articleDate = extractDateFromPath(r.getPath());
        if (articleDate == null) return true;
        if (from != null && articleDate.isBefore(from)) return false;
        if (to   != null && articleDate.isAfter(to))   return false;
        return true;
    }

    private boolean matchesText(SearchResultItem r, String query) {
        if (StringUtils.isEmpty(query)) return true;
        String q = query.toLowerCase();
        if (r.getTitle().toLowerCase().contains(q))       return true;
        if (r.getDescription().toLowerCase().contains(q)) return true;
        if (r.getCategory().toLowerCase().contains(q))    return true;
        return r.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(q));
    }

    /**
     * Parses {@code yyyy}, {@code mm}, {@code dd} from the path segment that follows
     * {@code articles/}, e.g.
     * {@code /content/newspaper/language-masters/en/articles/2026/04/11/article-name}
     * → {@code 2026-04-11}.
     */
    private static LocalDate extractDateFromPath(String path) {
        String[] segments = path.split("/");
        for (int i = 0; i < segments.length - 3; i++) {
            if ("articles".equals(segments[i])) {
                try {
                    return LocalDate.of(
                        Integer.parseInt(segments[i + 1]),
                        Integer.parseInt(segments[i + 2]),
                        Integer.parseInt(segments[i + 3])
                    );
                } catch (NumberFormatException | java.time.DateTimeException e) {
                    return null;
                }
            }
        }
        return null;
    }
}
