package com.fa.core.search.impl;

import com.fa.core.models.dto.SearchResultItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MockArticleSearchServiceTest {

    // Mock data has 8 articles:
    // Apr-10 politics, Apr-11 technology, Apr-12 sport, Apr-13 world,
    // Apr-14 politics, Apr-15 technology, Apr-16 culture, Apr-17 business

    private MockArticleSearchService service;

    @BeforeEach
    void setUp() {
        service = new MockArticleSearchService();
    }

    @Test
    void allResultsReturnedWithNoFilters() {
        assertEquals(8, service.search(null, null, null, null, null, 0, 100).size());
    }

    @Test
    void textFilterIsCaseInsensitive() {
        // "ELECTION" matches the title of the Apr-10 politics article
        List<SearchResultItem> results = service.search(null, "ELECTION", null, null, null, 0, 10);
        assertEquals(1, results.size());
        assertTrue(results.get(0).getArticleTitle().toLowerCase().contains("election"));
    }

    @Test
    void textFilterMatchesSummary() {
        // "coalition" appears only in the UK election article summary
        List<SearchResultItem> results = service.search(null, "coalition", null, null, null, 0, 10);
        assertEquals(1, results.size());
    }

    @Test
    void categoryFilterExact() {
        List<SearchResultItem> results = service.search(null, null, "technology", null, null, 0, 10);
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(r -> "technology".equals(r.getCategory())));
    }

    @Test
    void categoryFilterIsCaseInsensitive() {
        assertEquals(1, service.search(null, null, "SPORT", null, null, 0, 10).size());
    }

    @Test
    void unknownCategoryReturnsEmpty() {
        assertTrue(service.search(null, null, "unknown-category", null, null, 0, 10).isEmpty());
    }

    @Test
    void dateFromFilterExcludesEarlierArticles() {
        // Apr-15 onwards: Apr-15 (tech), Apr-16 (culture), Apr-17 (business) = 3
        List<SearchResultItem> results = service.search(null, null, null, "2026-04-15", null, 0, 10);
        assertEquals(3, results.size());
    }

    @Test
    void dateToFilterExcludesLaterArticles() {
        // Up to Apr-11 inclusive: Apr-10 (politics), Apr-11 (technology) = 2
        List<SearchResultItem> results = service.search(null, null, null, null, "2026-04-11", 0, 10);
        assertEquals(2, results.size());
    }

    @Test
    void dateRangeBothBounds() {
        // Apr-12 to Apr-14: sport(12), world(13), politics-nhs(14) = 3
        List<SearchResultItem> results = service.search(null, null, null, "2026-04-12", "2026-04-14", 0, 10);
        assertEquals(3, results.size());
    }

    @Test
    void dateRangeSingleDay() {
        List<SearchResultItem> results = service.search(null, null, null, "2026-04-13", "2026-04-13", 0, 10);
        assertEquals(1, results.size());
        assertEquals("world", results.get(0).getCategory());
    }

    @Test
    void paginationOffsetAndLimit() {
        List<SearchResultItem> page1 = service.search(null, null, null, null, null, 0, 3);
        List<SearchResultItem> page2 = service.search(null, null, null, null, null, 3, 3);
        List<SearchResultItem> page3 = service.search(null, null, null, null, null, 6, 3);

        assertEquals(3, page1.size());
        assertEquals(3, page2.size());
        assertEquals(2, page3.size());
        // pages are disjoint
        assertNotEquals(page1.get(0).getPath(), page2.get(0).getPath());
        assertNotEquals(page2.get(0).getPath(), page3.get(0).getPath());
    }

    @Test
    void offsetBeyondResultsReturnsEmpty() {
        assertTrue(service.search(null, null, null, null, null, 100, 10).isEmpty());
    }

    @Test
    void countMatchesSearchResultSize() {
        long count = service.count(null, "Labour", null, null, null);
        long searchSize = service.search(null, "Labour", null, null, null, 0, 100).size();
        assertEquals(count, searchSize);
    }

    @Test
    void countWithCategoryFilter() {
        assertEquals(2, service.count(null, null, "politics", null, null));
    }

    @Test
    void countWithDateRange() {
        assertEquals(3, service.count(null, null, null, "2026-04-15", null));
    }

    @Test
    void getCategoriesContainsKnownValues() {
        List<String> categories = service.getCategories("/any/root");
        assertFalse(categories.isEmpty());
        assertTrue(categories.contains("technology"));
        assertTrue(categories.contains("sport"));
        assertTrue(categories.contains("politics"));
        assertTrue(categories.contains("culture"));
        assertTrue(categories.contains("business"));
    }

    @Test
    void searchRootParameterIgnoredByMock() {
        // Mock ignores searchRoot — same results regardless of root
        long c1 = service.count("/content/en", null, null, null, null);
        long c2 = service.count("/content/fr", null, null, null, null);
        assertEquals(c1, c2);
    }
}
