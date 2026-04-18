package com.fa.core.models;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.fa.core.search.ArticleSearchService;
import com.fa.core.search.SearchResultItem;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ArticleListingModel {

    private static final Logger LOG = LoggerFactory.getLogger(ArticleListingModel.class);
    private static final int DEFAULT_MAX_ITEMS = 6;

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    private ArticleSearchService searchService;

    @ValueMapValue
    private String primaryTag;

    @ValueMapValue
    private Integer maxItems;

    @ValueMapValue
    private String sectionTitle;

    private List<ArticleCardItem> cards = new ArrayList<>();
    private String displayTitle;

    @PostConstruct
    protected void init() {
        if (searchService == null) {
            LOG.warn("ArticleListingModel: ArticleSearchService not available");
            return;
        }

        PageManager pageManager = request.getResourceResolver().adaptTo(PageManager.class);
        if (pageManager == null) return;

        Page currentPage = pageManager.getContainingPage(request.getResource());
        if (currentPage == null) return;

        displayTitle = StringUtils.isNotBlank(sectionTitle) ? sectionTitle : currentPage.getTitle();

        Page langRoot = currentPage.getAbsoluteParent(3);
        if (langRoot == null) return;

        int limit = (maxItems != null && maxItems > 0) ? maxItems : DEFAULT_MAX_ITEMS;
        String tag = StringUtils.trimToNull(primaryTag);

        List<SearchResultItem> results =
                searchService.search(langRoot.getPath(), null, tag, null, null, 0, limit);

        for (SearchResultItem result : results) {
            cards.add(new ArticleCardItem(result, langRoot.getPath()));
        }
    }

    public boolean hasItems()               { return !cards.isEmpty(); }
    public List<ArticleCardItem> getCards() { return Collections.unmodifiableList(cards); }
    public String getDisplayTitle()         { return displayTitle; }

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

        ArticleCardItem(SearchResultItem result, String langRootPath) {
            title       = result.getTitle();
            link        = result.getPath() + ".html";
            imageSrc    = result.getImageUrl();
            description = result.getDescription();
            author      = result.getAuthor();

            String cat  = result.getCategory();
            category    = StringUtils.isNotBlank(cat)
                          ? StringUtils.capitalize(cat.toLowerCase(Locale.ENGLISH))
                          : null;
            categoryLink = StringUtils.isNotBlank(cat)
                           ? langRootPath + "/" + cat + ".html"
                           : null;

            formattedDate = result.getPublishedDate();
            isoDate       = extractIsoDate(result.getPath());
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

        /**
         * Derives ISO date (yyyy-MM-dd) from the article path segments
         * /articles/<yyyy>/<mm>/<dd>/<name> — avoids storing a redundant property.
         */
        private static String extractIsoDate(String path) {
            if (path == null) return null;
            String[] seg = path.split("/");
            for (int i = 0; i < seg.length - 3; i++) {
                if ("articles".equals(seg[i])) {
                    try {
                        int y = Integer.parseInt(seg[i + 1]);
                        int m = Integer.parseInt(seg[i + 2]);
                        int d = Integer.parseInt(seg[i + 3]);
                        return String.format("%04d-%02d-%02d", y, m, d);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            }
            return null;
        }
    }
}
