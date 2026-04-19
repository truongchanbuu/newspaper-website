package com.fa.core.models;

import com.day.cq.wcm.api.Page;
import com.fa.core.models.dto.SearchResultItem;
import com.fa.core.search.ArticleSearchService;
import com.fa.core.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class ArticleListingModelTest {

    private final AemContext context = AppAemContext.newAemContext();

    @Mock
    private ArticleSearchService mockSearchService;

    @BeforeEach
    void setUp() {
        context.registerService(ArticleSearchService.class, mockSearchService);
        context.addModelsForClasses(ArticleListingModel.class);
    }

    @Test
    void primaryTagPassedToService() {
        context.create().page("/content/newspaper/language-masters/en");
        Page page = context.create().page("/content/newspaper/language-masters/en/sport");
        when(mockSearchService.search(
            eq("/content/newspaper/language-masters/en"),
            isNull(), eq("sport"), isNull(), isNull(), eq(0), eq(6)))
            .thenReturn(Collections.emptyList());

        Resource res = context.create().resource(page, "listing",
            "sling:resourceType", "newspaper/components/content/articlelisting",
            "primaryTag", "sport");
        context.currentResource(res);
        context.request().adaptTo(ArticleListingModel.class);

        verify(mockSearchService).search(
            eq("/content/newspaper/language-masters/en"),
            isNull(), eq("sport"), isNull(), isNull(), eq(0), eq(6));
    }

    @Test
    void cardItemFieldsMappedFromSearchResult() {
        context.create().page("/content/newspaper/language-masters/en");
        Page page = context.create().page("/content/newspaper/language-masters/en/politics");

        SearchResultItem result = new SearchResultItem(
            "/content/newspaper/language-masters/en/articles/2026/04/15/test-article",
            "Test Title", null, "/dam/img.jpg", "Test summary",
            null, "politics", "Test Author");
        when(mockSearchService.search(any(), any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(Collections.singletonList(result));

        Resource res = context.create().resource(page, "listing",
            "sling:resourceType", "newspaper/components/content/articlelisting");
        context.currentResource(res);
        ArticleListingModel model = context.request().adaptTo(ArticleListingModel.class);

        assertNotNull(model);
        assertTrue(model.hasItems());
        ArticleListingModel.ArticleCardItem card = model.getCards().get(0);
        assertEquals("Test Title", card.getTitle());
        assertEquals("/content/newspaper/language-masters/en/articles/2026/04/15/test-article.html",
            card.getLink());
        assertEquals("/dam/img.jpg", card.getImageSrc());
        assertEquals("Test summary", card.getDescription());
        assertEquals("Test Author", card.getAuthor());
        assertEquals("politics", card.getSectionSlug());
        assertEquals("2026-04-15", card.getIsoDate());
        assertNotNull(card.getTimeAgo());
    }

    @Test
    void isoDateExtractedFromPathSegments() {
        context.create().page("/content/newspaper/language-masters/en");
        Page page = context.create().page("/content/newspaper/language-masters/en/world");

        SearchResultItem result = new SearchResultItem(
            "/content/newspaper/language-masters/en/articles/2025/12/31/new-years-eve",
            "New Year's Eve", null, null, "Summary", null, "world", "Author");
        when(mockSearchService.search(any(), any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(Collections.singletonList(result));

        Resource res = context.create().resource(page, "listing",
            "sling:resourceType", "newspaper/components/content/articlelisting");
        context.currentResource(res);
        ArticleListingModel model = context.request().adaptTo(ArticleListingModel.class);

        assertEquals("2025-12-31", model.getCards().get(0).getIsoDate());
    }

    @Test
    void hasItemsFalseWhenServiceReturnsEmpty() {
        context.create().page("/content/newspaper/language-masters/en");
        Page page = context.create().page("/content/newspaper/language-masters/en/tech");
        when(mockSearchService.search(any(), any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());

        Resource res = context.create().resource(page, "listing",
            "sling:resourceType", "newspaper/components/content/articlelisting");
        context.currentResource(res);
        ArticleListingModel model = context.request().adaptTo(ArticleListingModel.class);

        assertNotNull(model);
        assertFalse(model.hasItems());
    }

    @Test
    void maxItemsLimitPassedToService() {
        context.create().page("/content/newspaper/language-masters/en");
        Page page = context.create().page("/content/newspaper/language-masters/en/politics");
        when(mockSearchService.search(any(), any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());

        Resource res = context.create().resource(page, "listing",
            "sling:resourceType", "newspaper/components/content/articlelisting",
            "maxItems", 3);
        context.currentResource(res);
        context.request().adaptTo(ArticleListingModel.class);

        verify(mockSearchService).search(any(), any(), any(), any(), any(), eq(0), eq(3));
    }
}
