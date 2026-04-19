package com.fa.core.models;

import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.fa.core.utils.DateUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ArticleCardModel {

    private static final String SOURCE_PAGE = "page";
    private static final String SOURCE_MANUAL = "manual";

    @Self
    private SlingHttpServletRequest request;

    @ValueMapValue
    private String source;

    @ValueMapValue
    private String pagePath;

    @ValueMapValue
    private String title;

    @ValueMapValue
    private String summary;

    @ValueMapValue
    private String link;

    @ValueMapValue
    private String imageSrc;

    @ValueMapValue
    private String imageAlt;

    @ValueMapValue
    private String author;

    @ValueMapValue(name = "articleDate")
    private Calendar articleDate;

    private Page articlePage;

    private String sectionSlug;
    private String sectionLabel;
    private String sectionLink;

    private String resolvedTitle;
    private String resolvedSummary;
    private String resolvedLink;
    private String resolvedImageSrc;
    private String resolvedImageAlt;
    private String resolvedAuthor;
    private Calendar resolvedDate;

    private String formattedDate;
    private String isoDate;
    private String timeAgo;

    @PostConstruct
    protected void init() {
        if (StringUtils.isBlank(source)) {
            source = SOURCE_PAGE;
        }

        if (SOURCE_PAGE.equals(source)) {
            resolveFromPageContext();
        } else {
            resolveFromManual();
        }

        resolveDates();
    }

    private void resolveFromPageContext() {
        articlePage = resolveArticlePage();
        if (articlePage == null) {
            return;
        }

        String pageTitle = articlePage.getTitle();
        resolvedTitle = StringUtils.defaultIfBlank(pageTitle, articlePage.getName());
        resolvedSummary = articlePage.getDescription();
        resolvedLink = articlePage.getPath() + ".html";

        resolvedAuthor = articlePage.getProperties().get("articleAuthor", String.class);
        resolvedDate = articlePage.getProperties().get("articleDate", Calendar.class);

        // image/fileReference is stored as a property on the child 'image' node
        Resource contentResource = articlePage.getContentResource();
        if (contentResource != null) {
            Resource imageResource = contentResource.getChild("image");
            if (imageResource != null) {
                resolvedImageSrc = imageResource.getValueMap().get("fileReference", String.class);
            }
        }
        String alt = articlePage.getProperties().get("imageAlt", String.class);
        resolvedImageAlt = StringUtils.defaultIfBlank(alt, resolvedTitle);

        resolveSectionFromTags(articlePage);
    }

    private Page resolveArticlePage() {
        ResourceResolver resolver = request.getResourceResolver();
        PageManager pageManager = resolver.adaptTo(PageManager.class);
        if (pageManager == null) {
            return null;
        }

        if (StringUtils.isNotBlank(pagePath)) {
            Page page = pageManager.getPage(pagePath);
            if (page != null) {
                return page;
            }
        }

        Resource resource = request.getResource();
        Page page = resource.adaptTo(Page.class);
        if (page != null) {
            return page;
        }

        return pageManager.getContainingPage(resource);
    }

    private void resolveFromManual() {
        resolvedTitle = title;
        resolvedSummary = summary;
        resolvedLink = normalizeLink(link);

        resolvedImageSrc = imageSrc;
        resolvedImageAlt = imageAlt;
        resolvedAuthor = author;
        resolvedDate = articleDate;
    }

    private void resolveSectionFromTags(Page page) {
        String[] tags = page.getProperties().get("cq:tags", String[].class);
        if (tags == null || tags.length == 0) {
            return;
        }

        ResourceResolver resolver = request.getResourceResolver();
        TagManager tagManager = resolver.adaptTo(TagManager.class);
        if (tagManager == null) {
            return;
        }

        Tag tag = tagManager.resolve(tags[0]);
        if (tag == null) {
            return;
        }

        sectionSlug = tag.getName();
        sectionLabel = tag.getTitle();
        sectionLink = buildSectionLink(page, sectionSlug);
    }

    private String buildSectionLink(Page articlePage, String slug) {
        Page langRoot = articlePage.getAbsoluteParent(3);
        if (langRoot == null) {
            return null;
        }
        return langRoot.getPath() + "/" + slug + ".html";
    }

    private void resolveDates() {
        if (resolvedDate != null) {
            // Calendar type — stored by programmatic authoring or typed JCR DATE node
            formattedDate = new SimpleDateFormat(
                    DateUtils.DATE_WITH_NAME_PATTERN, Locale.ENGLISH).format(resolvedDate.getTime());
            isoDate = new SimpleDateFormat(
                    DateUtils.ISO_DATE_PATTERN, Locale.ENGLISH).format(resolvedDate.getTime());
            timeAgo = DateUtils.computeTimeAgo(resolvedDate);
            return;
        }

        // Fallback: Granite datepicker stores a bare ISO string ("yyyy-MM-dd").
        // ValueMap cannot coerce that to Calendar, so read it as String directly.
        String rawDate = request.getResource().getValueMap().get("articleDate", String.class);
        if (StringUtils.isBlank(rawDate) && articlePage != null) {
            rawDate = articlePage.getProperties().get("articleDate", String.class);
        }
        if (StringUtils.isBlank(rawDate)) {
            return;
        }

        isoDate = rawDate.length() > 10 ? rawDate.substring(0, 10) : rawDate;
        timeAgo = DateUtils.computeTimeAgo(isoDate);

        try {
            java.util.Date parsed = new SimpleDateFormat(
                    DateUtils.ISO_DATE_PATTERN, Locale.ENGLISH).parse(isoDate);
            formattedDate = new SimpleDateFormat(
                    DateUtils.DATE_WITH_NAME_PATTERN, Locale.ENGLISH).format(parsed);
        } catch (java.text.ParseException ignored) {
            formattedDate = isoDate;
        }
    }

    private static String normalizeLink(String link) {
        if (StringUtils.isBlank(link)) {
            return null;
        }
        return link.contains(".") ? link : link + ".html";
    }

    public boolean isEmpty() {
        return StringUtils.isBlank(resolvedTitle);
    }

    public String getTitle() {
        return resolvedTitle;
    }

    public String getSummary() {
        return resolvedSummary;
    }

    public String getLink() {
        return resolvedLink;
    }

    public String getImageSrc() {
        return resolvedImageSrc;
    }

    public String getImageAlt() {
        return StringUtils.defaultIfBlank(resolvedImageAlt, resolvedTitle);
    }

    /* Section */

    public String getSection() {
        return sectionSlug;
    }

    public String getSectionDisplay() {
        return sectionLabel;
    }

    public String getSectionLink() {
        return sectionLink;
    }

    /* Meta */

    public String getAuthor() {
        return resolvedAuthor;
    }

    public String getFormattedDate() {
        return formattedDate;
    }

    public String getIsoDate() {
        return isoDate;
    }

    public String getTimeAgo() {
        return timeAgo;
    }
}