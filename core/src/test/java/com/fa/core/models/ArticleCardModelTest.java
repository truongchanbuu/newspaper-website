package com.fa.core.models;

import com.day.cq.wcm.api.Page;
import com.fa.core.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(AemContextExtension.class)
class ArticleCardModelTest {

    private final AemContext context = AppAemContext.newAemContext();

    @BeforeEach
    void setUp() {
        context.addModelsForClasses(ArticleCardModel.class);
    }

    @Test
    void sourcePageResolvesFromPagePath() {
        Calendar articleDate = new GregorianCalendar(2026, Calendar.APRIL, 10);
        Map<String, Object> pageProps = new HashMap<>();
        pageProps.put("jcr:title", "Test Article");
        pageProps.put("jcr:description", "Test summary");
        pageProps.put("articleAuthor", "Test Author");
        pageProps.put("articleDate", articleDate);

        Page articlePage = context.create().page(
            "/content/newspaper/language-masters/en/articles/2026/04/10/test-article",
            null, pageProps);
        context.create().resource(articlePage.getContentResource(), "image",
            "fileReference", "/content/dam/test.jpg");

        Resource cardResource = context.create().resource(
            "/content/testpage/jcr:content/card",
            "sling:resourceType", "newspaper/components/content/articlecard",
            "source", "page",
            "pagePath", articlePage.getPath());

        context.currentResource(cardResource);
        ArticleCardModel model = context.request().adaptTo(ArticleCardModel.class);

        assertNotNull(model);
        assertFalse(model.isEmpty());
        assertEquals("Test Article", model.getTitle());
        assertEquals("Test summary", model.getSummary());
        assertEquals(articlePage.getPath() + ".html", model.getLink());
        assertEquals("/content/dam/test.jpg", model.getImageSrc());
        assertEquals("Test Author", model.getAuthor());
        assertNotNull(model.getFormattedDate());
        assertEquals("2026-04-10", model.getIsoDate());
        assertNotNull(model.getTimeAgo());
    }

    @Test
    void sourceManualUsesComponentProperties() {
        Resource cardResource = context.create().resource(
            "/content/testpage/jcr:content/card",
            "sling:resourceType", "newspaper/components/content/articlecard",
            "source", "manual",
            "title", "Manual Title",
            "summary", "Manual summary",
            "link", "/content/some-page",
            "imageSrc", "/content/dam/img.jpg",
            "imageAlt", "Alt text",
            "author", "Manual Author");

        context.currentResource(cardResource);
        ArticleCardModel model = context.request().adaptTo(ArticleCardModel.class);

        assertNotNull(model);
        assertFalse(model.isEmpty());
        assertEquals("Manual Title", model.getTitle());
        assertEquals("Manual summary", model.getSummary());
        assertEquals("/content/some-page.html", model.getLink());
        assertEquals("/content/dam/img.jpg", model.getImageSrc());
        assertEquals("Alt text", model.getImageAlt());
        assertEquals("Manual Author", model.getAuthor());
    }

    @Test
    void isEmptyWhenNoTitle() {
        Resource cardResource = context.create().resource(
            "/content/testpage/jcr:content/card",
            "sling:resourceType", "newspaper/components/content/articlecard",
            "source", "manual");

        context.currentResource(cardResource);
        ArticleCardModel model = context.request().adaptTo(ArticleCardModel.class);

        assertNotNull(model);
        assertTrue(model.isEmpty());
    }

    @Test
    void normalizeLink_noExtension_appendsHtml() {
        Resource cardResource = context.create().resource(
            "/content/testpage/jcr:content/card",
            "sling:resourceType", "newspaper/components/content/articlecard",
            "source", "manual",
            "title", "T",
            "link", "/content/my-page");

        context.currentResource(cardResource);
        ArticleCardModel model = context.request().adaptTo(ArticleCardModel.class);

        assertEquals("/content/my-page.html", model.getLink());
    }

    @Test
    void normalizeLink_hasExtension_unchanged() {
        Resource cardResource = context.create().resource(
            "/content/testpage/jcr:content/card",
            "sling:resourceType", "newspaper/components/content/articlecard",
            "source", "manual",
            "title", "T",
            "link", "https://external.com/path.html");

        context.currentResource(cardResource);
        ArticleCardModel model = context.request().adaptTo(ArticleCardModel.class);

        assertEquals("https://external.com/path.html", model.getLink());
    }
}
