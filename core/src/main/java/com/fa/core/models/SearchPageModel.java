package com.fa.core.models;

import com.day.cq.wcm.api.Page;
import com.fa.core.search.SearchResultItem;
import com.fa.core.search.ArticleSearchService;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.annotation.PostConstruct;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

@Model(
        adaptables = SlingHttpServletRequest.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class SearchPageModel {

    private static final int PAGE_SIZE = 12;

    @SlingObject
    private SlingHttpServletRequest request;

    @ScriptVariable
    private Page currentPage;

    @OSGiService
    private ArticleSearchService searchService;

    private String query;
    private String category;
    private String dateFrom;
    private String dateTo;
    private int currentOffset;

    private List<SearchResultItem> results       = Collections.emptyList();
    private List<String>              availableCategories = Collections.emptyList();
    private long                      totalCount;

    @PostConstruct
    private void init() {
        query    = StringUtils.trimToEmpty(request.getParameter("q"));
        category = StringUtils.trimToNull(request.getParameter("category"));
        dateFrom = StringUtils.trimToNull(request.getParameter("dateFrom"));
        dateTo   = StringUtils.trimToNull(request.getParameter("dateTo"));

        String offsetParam = request.getParameter("offset");
        currentOffset = StringUtils.isNumeric(offsetParam) ? Integer.parseInt(offsetParam) : 0;

        if (searchService == null) {
            return;
        }

        String root         = deriveLanguageRoot();
        availableCategories = searchService.getCategories(root);
        totalCount          = searchService.count(root, query, category, dateFrom, dateTo);
        results             = searchService.search(root, query, category, dateFrom, dateTo,
                                                   currentOffset, PAGE_SIZE);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String deriveLanguageRoot() {
        // /content/newspaper/language-masters/en/search → level 3 = /content/newspaper/language-masters/en
        if (currentPage != null) {
            Page langRoot = currentPage.getAbsoluteParent(3);
            if (langRoot != null) {
                return langRoot.getPath();
            }
        }
        return "/content/newspaper/language-masters";
    }

    private String encode(String value) {
        if (StringUtils.isEmpty(value)) return "";
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    private String buildPageUrl(int offset) {
        if (currentPage == null) return "#";
        StringBuilder sb = new StringBuilder(currentPage.getPath()).append(".html?");
        sb.append("q=").append(encode(query));
        if (StringUtils.isNotEmpty(category)) sb.append("&category=").append(encode(category));
        if (StringUtils.isNotEmpty(dateFrom))  sb.append("&dateFrom=").append(encode(dateFrom));
        if (StringUtils.isNotEmpty(dateTo))    sb.append("&dateTo=").append(encode(dateTo));
        if (offset > 0)                        sb.append("&offset=").append(offset);
        return sb.toString();
    }

    // ── Computed properties for HTL ────────────────────────────────────────────

    public String getLanguageCode() {
        if (currentPage != null) {
            Page langRoot = currentPage.getAbsoluteParent(3);
            if (langRoot != null) {
                return langRoot.getName();
            }
        }
        return "en";
    }

    public String getPageUrl() {
        return currentPage != null ? currentPage.getPath() + ".html" : "/search.html";
    }

    public String getResultLabel() {
        return totalCount + " result" + (totalCount == 1 ? "" : "s");
    }

    public String getPrevPageUrl() { return buildPageUrl(getPrevOffset()); }
    public String getNextPageUrl() { return buildPageUrl(getNextOffset()); }

    // ── Accessors ──────────────────────────────────────────────────────────────

    public String                    getQuery()              { return query; }
    public String                    getCategory()           { return category; }
    public String                    getDateFrom()           { return dateFrom; }
    public String                    getDateTo()             { return dateTo; }
    public List<SearchResultItem> getResults()            { return results; }
    public List<String>              getAvailableCategories(){ return availableCategories; }
    public long                      getTotalCount()         { return totalCount; }
    public int                       getCurrentOffset()      { return currentOffset; }
    public int                       getPageSize()           { return PAGE_SIZE; }
    public int                       getNextOffset()         { return currentOffset + PAGE_SIZE; }
    public int                       getPrevOffset()         { return Math.max(0, currentOffset - PAGE_SIZE); }
    public boolean                   isHasPrev()             { return currentOffset > 0; }
    public boolean                   isHasNext()             { return (currentOffset + PAGE_SIZE) < totalCount; }
    public boolean                   isHasResults()          { return !results.isEmpty(); }
    public boolean                   isHasQuery()            {
        return StringUtils.isNotEmpty(query) || category != null || dateFrom != null || dateTo != null;
    }
}
