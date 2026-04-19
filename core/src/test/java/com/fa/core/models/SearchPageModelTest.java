package com.fa.core.models;

import com.day.cq.wcm.api.Page;
import com.fa.core.models.dto.SearchResultItem;
import com.fa.core.search.ArticleSearchService;
import com.fa.core.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(AemContextExtension.class)
class SearchPageModelTest {

    private final AemContext context = AppAemContext.newAemContext();
    private ArticleSearchService mockSearchService;

    @BeforeEach
    void setUp() {
        mockSearchService = mock(ArticleSearchService.class);
        context.registerService(ArticleSearchService.class, mockSearchService);
        context.addModelsForClasses(SearchPageModel.class);

        context.create().page("/content/newspaper/language-masters/en");
        Page searchPage = context.create().page("/content/newspaper/language-masters/en/search");
        context.currentPage(searchPage);

        when(mockSearchService.search(any(), any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());
        when(mockSearchService.count(any(), any(), any(), any(), any())).thenReturn(0L);
        when(mockSearchService.getCategories(any())).thenReturn(Collections.emptyList());
    }

    @Test
    void resultsLabelWhenNoResults() {
        SearchPageModel model = context.request().adaptTo(SearchPageModel.class);
        assertNotNull(model);
        assertEquals("No results found", model.getResultsLabel());
    }

    @Test
    void resultsLabelShowsRange() {
        when(mockSearchService.count(any(), any(), any(), any(), any())).thenReturn(15L);
        when(mockSearchService.search(any(), any(), any(), any(), any(), eq(0), eq(10)))
            .thenReturn(Collections.nCopies(10, mockResult()));

        SearchPageModel model = context.request().adaptTo(SearchPageModel.class);

        // en-dash U+2013 between from and to
        assertEquals("Showing 1\u201310 of 15 results", model.getResultsLabel());
    }

    @Test
    void datePreset7SetsFromAndToDates() {
        setParams("datePreset", "7");

        SearchPageModel model = context.request().adaptTo(SearchPageModel.class);

        assertEquals(LocalDate.now().minusDays(7).toString(), model.getFromDate());
        assertEquals(LocalDate.now().toString(), model.getToDate());
        assertEquals("7", model.getDatePreset());
    }

    @Test
    void datePresetTodaySetsFromAndToDates() {
        setParams("datePreset", "today");

        SearchPageModel model = context.request().adaptTo(SearchPageModel.class);

        String today = LocalDate.now().toString();
        assertEquals(today, model.getFromDate());
        assertEquals(today, model.getToDate());
    }

    @Test
    void unknownDatePresetIgnored() {
        setParams("datePreset", "bogus");

        SearchPageModel model = context.request().adaptTo(SearchPageModel.class);

        assertNull(model.getDatePreset());
        assertNull(model.getFromDate());
    }

    @Test
    void customDateModeWhenManualDatesWithoutPreset() {
        Map<String, Object> params = new HashMap<>();
        params.put("fromDate", "2026-01-01");
        params.put("toDate", "2026-01-31");
        context.request().setParameterMap(params);

        SearchPageModel model = context.request().adaptTo(SearchPageModel.class);

        assertTrue(model.isCustomDateMode());
        assertNull(model.getDatePreset());
        assertEquals("2026-01-01", model.getFromDate());
        assertEquals("2026-01-31", model.getToDate());
    }

    @Test
    void selectedDatePresetEmptyStringWhenNone() {
        SearchPageModel model = context.request().adaptTo(SearchPageModel.class);
        assertEquals("", model.getSelectedDatePreset());
    }

    @Test
    void selectedDatePresetMatchesActivePreset() {
        setParams("datePreset", "30");
        SearchPageModel model = context.request().adaptTo(SearchPageModel.class);
        assertEquals("30", model.getSelectedDatePreset());
    }

    @Test
    void paginationWindowWithEllipsis() {
        // 100 results → 10 pages; navigate to page 5
        when(mockSearchService.count(any(), any(), any(), any(), any())).thenReturn(100L);
        setParams("page", "5");

        SearchPageModel model = context.request().adaptTo(SearchPageModel.class);

        assertEquals(10, model.getTotalPages());
        assertEquals(5, model.getPageNum());

        // Expected window: [1, …, 3, 4, 5, 6, 7, …, 10] = 9 items
        List<SearchPageModel.PageNumberItem> pns = model.getPageNumbers();
        assertEquals(9, pns.size());

        assertEquals(1, pns.get(0).getNumber());
        assertFalse(pns.get(0).isEllipsis());

        assertTrue(pns.get(1).isEllipsis());

        assertTrue(pns.stream().anyMatch(pn -> !pn.isEllipsis() && pn.isCurrent() && pn.getNumber() == 5));

        assertTrue(pns.get(pns.size() - 2).isEllipsis());
        assertEquals(10, pns.get(pns.size() - 1).getNumber());
    }

    @Test
    void paginationWindowNoEllipsisForSmallTotal() {
        // 20 results → 2 pages; both fit without ellipsis
        when(mockSearchService.count(any(), any(), any(), any(), any())).thenReturn(20L);
        setParams("page", "1");

        SearchPageModel model = context.request().adaptTo(SearchPageModel.class);
        List<SearchPageModel.PageNumberItem> pns = model.getPageNumbers();

        assertEquals(2, pns.size());
        assertTrue(pns.stream().noneMatch(SearchPageModel.PageNumberItem::isEllipsis));
    }

    @Test
    void pageNumClampedToTotalPages() {
        // count=10 → totalPages=1; page=99 → clamped to 1
        when(mockSearchService.count(any(), any(), any(), any(), any())).thenReturn(10L);
        setParams("page", "99");

        SearchPageModel model = context.request().adaptTo(SearchPageModel.class);
        assertEquals(1, model.getPageNum());
    }

    @Test
    void datePresetsListHasSixEntries() {
        SearchPageModel model = context.request().adaptTo(SearchPageModel.class);
        List<SearchPageModel.DatePresetItem> presets = model.getDatePresets();
        assertEquals(6, presets.size());
        assertEquals("", presets.get(0).getValue());
        assertEquals("Any time", presets.get(0).getLabel());
        assertEquals("7", presets.get(3).getValue());
    }

    @Test
    void hasPrevAndNextPageFlags() {
        when(mockSearchService.count(any(), any(), any(), any(), any())).thenReturn(30L);
        setParams("page", "2");

        SearchPageModel model = context.request().adaptTo(SearchPageModel.class);

        assertTrue(model.isHasPrevPage());
        assertTrue(model.isHasNextPage());
        assertTrue(model.isShowPagination());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setParams(String key, String value) {
        Map<String, Object> params = new HashMap<>();
        params.put(key, value);
        context.request().setParameterMap(params);
    }

    private SearchResultItem mockResult() {
        return new SearchResultItem(
            "/content/newspaper/language-masters/en/articles/2026/04/10/test",
            "Test", null, null, "Summary", null, "politics", "Author");
    }
}
