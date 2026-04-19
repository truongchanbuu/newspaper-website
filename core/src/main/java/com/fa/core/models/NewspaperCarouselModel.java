package com.fa.core.models;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.fa.core.models.dto.SearchResultItem;
import com.fa.core.search.ArticleSearchService;
import com.fa.core.utils.CategoryUtils;
import com.fa.core.utils.DateUtils;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class NewspaperCarouselModel {

    private static final Logger LOG = LoggerFactory.getLogger(NewspaperCarouselModel.class);
    private static final String SOURCE_AUTO = "auto";
    private static final int DEFAULT_MAX_ITEMS = 5;
    private static final int DEFAULT_DELAY = 5000;

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    private ArticleSearchService searchService;

    @ValueMapValue
    private String source;

    @ValueMapValue
    private String primaryTag;

    @ValueMapValue
    private Integer maxItems;

    @ValueMapValue(name = "autoplay")
    private Boolean autoplayEnabled;

    @ValueMapValue
    private Integer delay;

    private List<AutoSlide> slides = Collections.emptyList();

    @PostConstruct
    protected void init() {
        if (!SOURCE_AUTO.equals(source)) return;

        if (searchService == null) {
            LOG.warn("NewspaperCarouselModel: ArticleSearchService unavailable — no slides rendered");
            return;
        }

        PageManager pm = request.getResourceResolver().adaptTo(PageManager.class);
        if (pm == null) return;

        Page currentPage = pm.getContainingPage(request.getResource());
        if (currentPage == null) return;

        Page langRoot = currentPage.getAbsoluteParent(3);
        if (langRoot == null) return;

        Map<String, String> labels = CategoryUtils.loadCategories(langRoot, null);
        int limit = (maxItems != null && maxItems > 0) ? maxItems : DEFAULT_MAX_ITEMS;
        String tag = StringUtils.trimToNull(primaryTag);

        List<SearchResultItem> results =
            searchService.search(langRoot.getPath(), null, tag, null, null, 0, limit);

        slides = results.stream()
            .map(r -> new AutoSlide(r, langRoot.getPath(), labels))
            .collect(Collectors.toList());
    }

    public boolean isManual()          { return !SOURCE_AUTO.equals(source); }
    public boolean isAuto()            { return SOURCE_AUTO.equals(source); }
    public boolean hasSlides()         { return !slides.isEmpty(); }
    public List<AutoSlide> getSlides() { return slides; }
    public boolean isAutoplay()        { return autoplayEnabled == null || autoplayEnabled; }
    public int getDelay()              { return delay != null ? delay : DEFAULT_DELAY; }

    // ── Slide DTO (auto mode only) ─────────────────────────────────────────────
    public static class AutoSlide {

        private final String title;
        private final String link;
        private final String imageSrc;
        private final String imageAlt;
        private final String category;
        private final String categoryLink;
        private final String author;
        private final String timeAgo;
        private final String isoDate;

        AutoSlide(SearchResultItem r, String langRootPath, Map<String, String> labels) {
            title    = r.getArticleTitle();
            link     = r.getPath() + ".html";
            imageSrc = r.getThumbnail();
            imageAlt = StringUtils.defaultIfBlank(r.getArticleTitle(), "");

            String cat   = r.getCategory();
            String slug  = StringUtils.isNotBlank(cat) ? cat.toLowerCase(Locale.ENGLISH) : null;
            category     = slug != null ? labels.getOrDefault(slug, slug) : null;
            categoryLink = slug != null ? langRootPath + "/" + slug + ".html" : null;

            author  = r.getAuthorName();
            isoDate = extractIsoDate(r.getPath());
            timeAgo = DateUtils.computeTimeAgo(isoDate);
        }

        public String getTitle()        { return title; }
        public String getLink()         { return link; }
        public String getImageSrc()     { return imageSrc; }
        public String getImageAlt()     { return imageAlt; }
        public String getCategory()     { return category; }
        public String getCategoryLink() { return categoryLink; }
        public String getAuthor()       { return author; }
        public String getTimeAgo()      { return timeAgo; }
        public String getIsoDate()      { return isoDate; }

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
