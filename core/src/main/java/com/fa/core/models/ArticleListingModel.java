package com.fa.core.models;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.jcr.query.Query;
import java.text.SimpleDateFormat;
import java.util.*;

@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ArticleListingModel {
    private static final Logger LOG = LoggerFactory.getLogger(ArticleListingModel.class);
    private static final int DEFAULT_MAX_ITEMS = 6;

    @Self
    private SlingHttpServletRequest request;

    @ValueMapValue
    private String articlesRootPath;

    @ValueMapValue
    private Integer maxItems;

    @ValueMapValue
    private String sectionTitle;

    private List<ArticleCardItem> cards = new ArrayList<>();
    private String displayTitle;

    @PostConstruct
    protected void init() {
        ResourceResolver resolver = request.getResourceResolver();
        PageManager pageManager = resolver.adaptTo(PageManager.class);
        if (pageManager == null) return;

        Page currentPage = pageManager.getContainingPage(request.getResource());
        if (currentPage == null) return;

        displayTitle = StringUtils.isNotBlank(sectionTitle) ? sectionTitle : currentPage.getTitle();

        if (StringUtils.isBlank(articlesRootPath)) {
            LOG.warn("ArticleListingModel: articlesRootPath is empty");
            return;
        }

        String language = currentPage.getAbsoluteParent(3).getName();
        String langRootPath = articlesRootPath + "/" + language;
        String articlesPath = langRootPath + "/articles";

        for (Page page : queryArticlePages(resolver, pageManager, articlesPath)) {
            cards.add(new ArticleCardItem(page, langRootPath));
        }
    }

    private List<Page> queryArticlePages(ResourceResolver resolver, PageManager pageManager, String articlesPath) {
        List<Page> results = new ArrayList<>();
        int limit = (maxItems != null && maxItems > 0) ? maxItems : DEFAULT_MAX_ITEMS;

        String sql = "SELECT page.* FROM [cq:Page] AS page " +
                "WHERE ISDESCENDANTNODE(page, '" + escapeJcrPath(articlesPath) + "') " +
                "ORDER BY page.[jcr:content/articleDate] DESC";

        try {
            Iterator<Resource> resources = resolver.findResources(sql, Query.JCR_SQL2);
            while (resources.hasNext() && results.size() < limit) {
                Resource res = resources.next();
                Page page = pageManager.getPage(res.getPath());
                if (page != null && page.getContentResource() != null) {
                    results.add(page);
                }
            }
        } catch (Exception e) {
            LOG.error("ArticleListingModel: query failed", e);
        }
        return results;
    }

    private static String escapeJcrPath(String path) {
        return path.replaceAll("['\"]", "");
    }

    public boolean hasItems()                        { return !cards.isEmpty(); }
    public List<ArticleCardItem> getCards()          { return Collections.unmodifiableList(cards); }
    public String getDisplayTitle()                  { return displayTitle; }

    // ── Article card DTO ───────────────────────────────────────────────────────
    public static class ArticleCardItem {
        private final String title;
        private final String link;
        private final String imageSrc;
        private final String category;
        private final String categoryLink;
        private final String description;
        private final String author;
        private final String formattedDate;
        private final String isoDate;

        ArticleCardItem(Page page, String langRootPath) {
            ValueMap props = page.getProperties();

            title       = StringUtils.defaultString(page.getTitle(), page.getName());
            link        = page.getPath() + ".html";
            imageSrc    = props.get("image/fileReference", String.class);
            description = page.getDescription();
            author      = props.get("articleAuthor", String.class);

            String tag = props.get("primaryTag", String.class);
            category = StringUtils.isNotBlank(tag)
                    ? StringUtils.capitalize(tag.toLowerCase(Locale.ENGLISH))
                    : null;
            categoryLink = StringUtils.isNotBlank(tag) ? langRootPath + "/" + tag + ".html" : null;

            Calendar articleDate = props.get("articleDate", Calendar.class);
            if (articleDate != null) {
                isoDate       = new SimpleDateFormat("yyyy-MM-dd").format(articleDate.getTime());
                formattedDate = new SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).format(articleDate.getTime());
            } else {
                isoDate       = null;
                formattedDate = null;
            }
        }

        public String getTitle()         { return title; }
        public String getLink()          { return link; }
        public String getImageSrc()      { return imageSrc; }
        public String getCategory()      { return category; }
        public String getCategoryLink()  { return categoryLink; }
        public String getDescription()   { return description; }
        public String getAuthor()        { return author; }
        public String getFormattedDate() { return formattedDate; }
        public String getIsoDate()       { return isoDate; }
    }
}
