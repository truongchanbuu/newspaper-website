package com.fa.core.models.dto;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Canonical DTO for a single article search result.
 *
 * <p>Field names follow the coworker's contract. Two extra fields are added by this project:
 * <ul>
 *   <li>{@code path} — JCR page path, required to build article links (.html suffix added by callers)</li>
 *   <li>{@link #getFormattedPublishDate()} — pre-formatted "d MMM yyyy" string ready for HTL rendering</li>
 * </ul>
 *
 * <p>Implementations of {@link com.fa.core.search.ArticleSearchService} must populate
 * {@code path} from the JCR query result node path.
 */
public class SearchResultItem {

    private static final String DATE_PATTERN = "d MMM yyyy";

    private final String path;
    private final String articleTitle;
    private final String subtitle;
    private final String thumbnail;
    private final String summary;
    private final Date   publishDate;
    private final String category;
    private final String authorName;

    public SearchResultItem(String path, String articleTitle, String subtitle,
                            String thumbnail, String summary, Date publishDate,
                            String category, String authorName) {
        this.path         = path;
        this.articleTitle = articleTitle;
        this.subtitle     = subtitle;
        this.thumbnail    = thumbnail;
        this.summary      = summary;
        this.publishDate  = publishDate;
        this.category     = category;
        this.authorName   = authorName;
    }

    public String getPath()          { return path; }
    public String getArticleTitle()  { return articleTitle; }
    public String getSubtitle()      { return subtitle; }
    public String getThumbnail()     { return thumbnail; }
    public String getSummary()       { return summary; }
    public Date   getPublishDate()   { return publishDate; }
    public String getCategory()      { return category; }
    public String getAuthorName()    { return authorName; }

    /** Returns {@code publishDate} formatted as "d MMM yyyy" (e.g. "10 Apr 2026") for direct use in HTL. */
    public String getFormattedPublishDate() {
        if (publishDate == null) return null;
        return new SimpleDateFormat(DATE_PATTERN, Locale.ENGLISH).format(publishDate);
    }
}
