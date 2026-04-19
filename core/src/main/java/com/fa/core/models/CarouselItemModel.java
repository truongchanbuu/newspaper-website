package com.fa.core.models;

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
public class CarouselItemModel {

    @Self
    private SlingHttpServletRequest request;

    @ValueMapValue
    private String articlePath;

    @ValueMapValue
    private String categoryLabel;

    @ValueMapValue
    private String titleOverride;

    private String title;
    private String description;
    private String imageSrc;
    private String imageAlt;
    private String link;
    private String author;
    private String category;
    private String isoDate;
    private String formattedDate;
    private String timeAgo;
    private String jsonLd;

    @PostConstruct
    protected void init() {
        if (StringUtils.isBlank(articlePath)) return;

        ResourceResolver resolver = request.getResourceResolver();
        PageManager pageManager = resolver.adaptTo(PageManager.class);
        if (pageManager == null) return;

        Page page = pageManager.getPage(articlePath);
        if (page == null) return;

        String pageTitle = StringUtils.defaultIfBlank(page.getTitle(), page.getName());
        title = StringUtils.isNotBlank(titleOverride) ? titleOverride : pageTitle;
        description = page.getDescription();
        link = articlePath + ".html";
        author = page.getProperties().get("articleAuthor", String.class);

        String primaryTag = page.getProperties().get("primaryTag", String.class);
        category = StringUtils.isNotBlank(categoryLabel) ? categoryLabel : primaryTag;

        Resource contentResource = page.getContentResource();
        if (contentResource != null) {
            Resource imageRes = contentResource.getChild("image");
            if (imageRes != null) {
                imageSrc = imageRes.getValueMap().get("fileReference", String.class);
            }
        }
        String rawAlt = page.getProperties().get("imageAlt", String.class);
        imageAlt = StringUtils.defaultIfBlank(rawAlt, title);

        Calendar articleDate = page.getProperties().get("articleDate", Calendar.class);
        if (articleDate != null) {
            isoDate = new SimpleDateFormat(DateUtils.ISO_DATE_PATTERN, Locale.ENGLISH)
                          .format(articleDate.getTime());
            formattedDate = new SimpleDateFormat(DateUtils.DATE_WITH_NAME_PATTERN, Locale.ENGLISH)
                                .format(articleDate.getTime());
            timeAgo = DateUtils.computeTimeAgo(articleDate);
        }

        buildJsonLd();
    }

    private void buildJsonLd() {
        if (link == null) return;
        StringBuilder sb = new StringBuilder(
            "{\"@context\":\"https://schema.org\",\"@type\":\"NewsArticle\"");
        appendJson(sb, "headline", title);
        appendJson(sb, "url", link);
        if (isoDate != null) appendJson(sb, "datePublished", isoDate);
        if (author != null) {
            sb.append(",\"author\":{\"@type\":\"Person\"");
            appendJson(sb, "name", author);
            sb.append("}");
        }
        if (imageSrc != null) appendJson(sb, "image", imageSrc);
        sb.append("}");
        jsonLd = sb.toString();
    }

    private static void appendJson(StringBuilder sb, String key, String value) {
        if (value == null) return;
        sb.append(",\"").append(key).append("\":\"")
          .append(value.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", ""))
          .append("\"");
    }

    public boolean isConfigured()    { return StringUtils.isNotBlank(link); }
    public String getTitle()         { return title; }
    public String getDescription()   { return description; }
    public String getImageSrc()      { return imageSrc; }
    public String getImageAlt()      { return StringUtils.defaultIfBlank(imageAlt, title); }
    public String getLink()          { return link; }
    public String getAuthor()        { return author; }
    public String getCategory()      { return category; }
    public String getIsoDate()       { return isoDate; }
    public String getFormattedDate() { return formattedDate; }
    public String getTimeAgo()       { return timeAgo; }
    public String getJsonLd()        { return jsonLd; }
}
