package com.fa.core.search.impl;

import com.fa.core.search.SearchResultItem;
import com.fa.core.search.ArticleSearchService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Temporary mock — replace with the real JCR/Oak query implementation once available.
 * Applies basic in-memory text and category filtering over a fixed article list.
 */
@Component(service = ArticleSearchService.class)
@ServiceDescription("Mock Article Search Service")
public class MockArticleSearchService implements ArticleSearchService {

    private static final List<SearchResultItem> MOCK_DATA = Arrays.asList(
        new SearchResultItem(
            "/content/language-masters/en/articles/2026/04/10/uk-election-results",
            "UK Election Results: Labour Leads Coalition Talks",
            "After a historic night, Labour emerged with a majority mandate and coalition negotiations have begun.",
            "politics", "10 Apr 2026", "/content/dam/newspaper/asset.jpg", "Jane Dougherty"),
        new SearchResultItem(
            "/content/language-masters/en/articles/2026/04/11/ai-climate-research",
            "AI Tools Accelerate Climate Change Research",
            "A new generation of AI models is helping scientists predict weather patterns with unprecedented accuracy.",
            "technology", "11 Apr 2026", "/content/dam/newspaper/asset.jpg", "Marcus Elliot"),
        new SearchResultItem(
            "/content/language-masters/en/articles/2026/04/12/premier-league-roundup",
            "Premier League Roundup: Title Race Goes to Final Day",
            "With only one match remaining, three clubs are still mathematically alive in the title race.",
            "sport", "12 Apr 2026", "/content/dam/newspaper/asset.jpg", "Sarah Bright"),
        new SearchResultItem(
            "/content/language-masters/en/articles/2026/04/13/global-trade-summit",
            "Global Trade Summit Ends Without Breakthrough",
            "Negotiators failed to agree on new tariff frameworks despite three days of talks in Geneva.",
            "world", "13 Apr 2026", "/content/dam/newspaper/asset.jpg", "Oliver Strand"),
        new SearchResultItem(
            "/content/language-masters/en/articles/2026/04/14/nhs-funding-plan",
            "NHS to Receive Record £12bn Funding Boost",
            "The Health Secretary announced the largest single investment in NHS infrastructure in decades.",
            "politics", "14 Apr 2026", "/content/dam/newspaper/asset.jpg", "Emma Coward"),
        new SearchResultItem(
            "/content/language-masters/en/articles/2026/04/15/tech-startup-regulation",
            "Tech Startups Warn Against Heavy-Handed Regulation",
            "Industry leaders say new EU digital market rules could push innovation to other regions.",
            "technology", "15 Apr 2026", "/content/dam/newspaper/asset.jpg", "Tom Vickers"),
        new SearchResultItem(
            "/content/language-masters/en/articles/2026/04/16/culture-award-shortlist",
            "Turner Prize Shortlist Unveiled at Tate Modern",
            "Four artists have been nominated for this year's prize, with a ceremony scheduled for November.",
            "culture", "16 Apr 2026", "/content/dam/newspaper/asset.jpg", "Lisa Park"),
        new SearchResultItem(
            "/content/language-masters/en/articles/2026/04/17/economic-outlook-q2",
            "Q2 Economic Outlook: Growth Expected to Slow",
            "Analysts forecast a dip in GDP growth as energy prices and interest rates remain elevated.",
            "business", "17 Apr 2026", "/content/dam/newspaper/asset.jpg", "Henry Walsh")
    );

    private static final List<String> CATEGORIES = Arrays.asList(
        "business", "culture", "environment", "lifestyle", "politics", "sport", "technology", "world"
    );

    @Override
    public List<SearchResultItem> search(String searchRoot, String query, String category,
                                             String dateFrom, String dateTo,
                                             int offset, int limit) {
        List<SearchResultItem> filtered = applyFilters(query, category);
        int from = Math.min(offset, filtered.size());
        int to   = Math.min(from + limit, filtered.size());
        return filtered.subList(from, to);
    }

    @Override
    public long count(String searchRoot, String query, String category,
                      String dateFrom, String dateTo) {
        return applyFilters(query, category).size();
    }

    @Override
    public List<String> getCategories(String searchRoot) {
        return Collections.unmodifiableList(CATEGORIES);
    }

    private List<SearchResultItem> applyFilters(String query, String category) {
        return MOCK_DATA.stream()
            .filter(r -> category == null || category.isEmpty()
                         || category.equalsIgnoreCase(r.getCategory()))
            .filter(r -> {
                if (query == null || query.isEmpty()) return true;
                String q = query.toLowerCase();
                return r.getTitle().toLowerCase().contains(q)
                    || r.getDescription().toLowerCase().contains(q)
                    || r.getCategory().toLowerCase().contains(q);
            })
            .collect(Collectors.toList());
    }
}
