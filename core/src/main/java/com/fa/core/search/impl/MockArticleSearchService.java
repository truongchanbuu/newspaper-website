package com.fa.core.search.impl;

import com.fa.core.models.dto.SearchResultItem;
import com.fa.core.search.ArticleSearchService;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Temporary mock — replace with the real JCR/Oak query implementation once available.
 */
@Component(service = ArticleSearchService.class)
@ServiceDescription("Mock Article Search Service")
public class MockArticleSearchService implements ArticleSearchService {

    private static final Logger LOG = LoggerFactory.getLogger(MockArticleSearchService.class);

    private static final List<SearchResultItem> MOCK_DATA = Arrays.asList(
        item("/content/newspaper/language-masters/en/articles/2026/04/10/uk-election-results",
            "UK Election Results: Labour Leads Coalition Talks",
            "After a historic night, Labour emerged with a majority mandate and coalition negotiations have begun.",
            "politics", "10 Apr 2026", "Jane Dougherty"),
        item("/content/newspaper/language-masters/en/articles/2026/04/11/ai-climate-research",
            "AI Tools Accelerate Climate Change Research",
            "A new generation of AI models is helping scientists predict weather patterns with unprecedented accuracy.",
            "technology", "11 Apr 2026", "Marcus Elliot"),
        item("/content/newspaper/language-masters/en/articles/2026/04/12/premier-league-roundup",
            "Premier League Roundup: Title Race Goes to Final Day",
            "With only one match remaining, three clubs are still mathematically alive in the title race.",
            "sport", "12 Apr 2026", "Sarah Bright"),
        item("/content/newspaper/language-masters/en/articles/2026/04/13/global-trade-summit",
            "Global Trade Summit Ends Without Breakthrough",
            "Negotiators failed to agree on new tariff frameworks despite three days of talks in Geneva.",
            "world", "13 Apr 2026", "Oliver Strand"),
        item("/content/newspaper/language-masters/en/articles/2026/04/14/nhs-funding-plan",
            "NHS to Receive Record £12bn Funding Boost",
            "The Health Secretary announced the largest single investment in NHS infrastructure in decades.",
            "politics", "14 Apr 2026", "Emma Coward"),
        item("/content/newspaper/language-masters/en/articles/2026/04/15/tech-startup-regulation",
            "Tech Startups Warn Against Heavy-Handed Regulation",
            "Industry leaders say new EU digital market rules could push innovation to other regions.",
            "technology", "15 Apr 2026", "Tom Vickers"),
        item("/content/newspaper/language-masters/en/articles/2026/04/16/culture-award-shortlist",
            "Turner Prize Shortlist Unveiled at Tate Modern",
            "Four artists have been nominated for this year's prize, with a ceremony scheduled for November.",
            "culture", "16 Apr 2026", "Lisa Park"),
        item("/content/newspaper/language-masters/en/articles/2026/04/17/economic-outlook-q2",
            "Q2 Economic Outlook: Growth Expected to Slow",
            "Analysts forecast a dip in GDP growth as energy prices and interest rates remain elevated.",
            "business", "17 Apr 2026", "Henry Walsh")
    );

    private static final List<String> CATEGORIES = Arrays.asList(
        "business", "culture", "environment", "lifestyle", "politics", "sport", "technology", "world"
    );

    @Activate
    protected void activate() {
        LOG.warn("MockArticleSearchService is active — replace with real JCR/Oak implementation before production deployment");
    }

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
        return r.getArticleTitle().toLowerCase().contains(q)
            || r.getSummary().toLowerCase().contains(q)
            || r.getCategory().toLowerCase().contains(q);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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

    private static SearchResultItem item(String path, String title, String summary,
                                         String category, String dateStr, String author) {
        Date date = null;
        try {
            date = new SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).parse(dateStr);
        } catch (ParseException e) {
            LOG.error("MockArticleSearchService: failed to parse date '{}'", dateStr, e);
        }
        return new SearchResultItem(path, title, null, "/content/dam/newspaper/asset.jpg",
                                    summary, date, category, author);
    }
}
