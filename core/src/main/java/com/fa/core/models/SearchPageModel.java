package com.fa.core.models;

import com.day.cq.wcm.api.Page;
import com.fa.core.search.ArticleSearchService;
import com.fa.core.search.SearchResultItem;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.annotation.PostConstruct;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Model(
        adaptables = SlingHttpServletRequest.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class SearchPageModel {

    // ── Constants ──────────────────────────────────────────────────────────────

    private static final int    PAGE_SIZE      = 10;
    private static final String ARTICLES_NODE  = "articles";

    // ── Injected ───────────────────────────────────────────────────────────────

    @Self
    private SlingHttpServletRequest request;

    @ScriptVariable
    private Page currentPage;

    @OSGiService
    private ArticleSearchService searchService;

    // ── Parsed request params ─────────────────────────────────────────────────

    private String query;
    private String category;
    private String fromDate;
    private String toDate;
    private String datePreset;
    private int    pageNum;   // 1-based

    // ── Computed ───────────────────────────────────────────────────────────────

    private List<SearchResultItem> results             = Collections.emptyList();
    private List<CategoryItem>     categories          = Collections.emptyList();
    private List<PageNumberItem>   pageNumbers         = Collections.emptyList();
    private long                   totalCount;
    private int                    totalPages          = 1;

    // ── Init ───────────────────────────────────────────────────────────────────

    @PostConstruct
    private void init() {
        parseParams();
        applyDatePreset();
        categories = buildCategories();

        if (searchService == null) return;

        String root = deriveLanguageRoot();
        totalCount  = searchService.count(root, query, category, fromDate, toDate);
        totalPages  = totalCount > 0 ? (int) Math.ceil((double) totalCount / PAGE_SIZE) : 1;
        pageNum     = Math.max(1, Math.min(pageNum, totalPages));
        int offset  = (pageNum - 1) * PAGE_SIZE;
        results     = searchService.search(root, query, category, fromDate, toDate, offset, PAGE_SIZE);
        pageNumbers = buildPageNumbers(pageNum, totalPages);
    }

    private void parseParams() {
        query      = StringUtils.trimToEmpty(request.getParameter("q"));
        category   = StringUtils.trimToNull(request.getParameter("category"));
        fromDate   = StringUtils.trimToNull(request.getParameter("fromDate"));
        toDate     = StringUtils.trimToNull(request.getParameter("toDate"));
        datePreset = StringUtils.trimToNull(request.getParameter("datePreset"));

        String pageParam = request.getParameter("page");
        pageNum = StringUtils.isNumeric(pageParam) ? Math.max(1, Integer.parseInt(pageParam)) : 1;
    }

    // ── Date helpers ───────────────────────────────────────────────────────────

    /** Presets override manual date inputs when a recognized preset value is present. */
    private void applyDatePreset() {
        if (datePreset != null) {
            LocalDate today = LocalDate.now();
            switch (datePreset) {
                case "today": fromDate = today.toString();                          break;
                case "1":     fromDate = today.minusDays(1).toString();             break;
                case "7":     fromDate = today.minusDays(7).toString();             break;
                case "14":    fromDate = today.minusDays(14).toString();            break;
                case "30":    fromDate = today.minusDays(30).toString();            break;
                default:      datePreset = null;                                    break;
            }
            if (datePreset != null) {
                toDate = today.toString();
            }
        }
    }

    // ── Category helpers ───────────────────────────────────────────────────────

    /**
     * Reads sibling nav pages under the language root (excluding {@code articles/}).
     * Falls back to {@link ArticleSearchService#getCategories} when no nav pages exist yet
     * (e.g. fresh install without category pages).
     */
    private List<CategoryItem> buildCategories() {
        if (currentPage == null) return Collections.emptyList();

        Page langRoot = currentPage.getAbsoluteParent(3);
        if (langRoot == null) return Collections.emptyList();

        List<CategoryItem> list = new ArrayList<>();
        String selfPath = currentPage.getPath();
        Iterator<Page> children = langRoot.listChildren();
        while (children.hasNext()) {
            Page child = children.next();
            if (ARTICLES_NODE.equalsIgnoreCase(child.getName())) continue;
            if (child.getPath().equals(selfPath)) continue;
            if (child.getProperties().get("hideInNav", false)) continue;
            list.add(new CategoryItem(child.getName(), child.getTitle()));
        }
        if (!list.isEmpty()) return list;

        // Fallback: populate from service when JCR nav pages haven't been created yet
        if (searchService != null) {
            return searchService.getCategories(langRoot.getPath()).stream()
                    .map(name -> new CategoryItem(name, name))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    // ── Pagination helpers ─────────────────────────────────────────────────────

    /**
     * Builds the page-number window: always shows 1, last, and current±2 pages,
     * with ellipsis nodes ({@code number=0, ellipsis=true}) filling the gaps.
     */
    private List<PageNumberItem> buildPageNumbers(int current, int total) {
        if (total <= 1) return Collections.emptyList();

        Set<Integer> show = new LinkedHashSet<>();
        show.add(1);
        for (int i = Math.max(2, current - 2); i <= Math.min(total - 1, current + 2); i++) {
            show.add(i);
        }
        show.add(total);

        List<PageNumberItem> result = new ArrayList<>();
        int prev = 0;
        for (int p : show) {
            if (p - prev > 1) {
                result.add(new PageNumberItem(0, false, true, null));
            }
            result.add(new PageNumberItem(p, p == current, false, buildPageUrl(p)));
            prev = p;
        }
        return result;
    }

    // ── URL builder ────────────────────────────────────────────────────────────

    private String buildPageUrl(int page) {
        if (currentPage == null) return "#";
        List<String> params = new ArrayList<>();
        if (StringUtils.isNotEmpty(query))    params.add("q=" + encode(query));
        if (StringUtils.isNotEmpty(category)) params.add("category=" + encode(category));
        if (StringUtils.isNotEmpty(datePreset)) {
            params.add("datePreset=" + encode(datePreset));
        } else {
            if (StringUtils.isNotEmpty(fromDate)) params.add("fromDate=" + encode(fromDate));
            if (StringUtils.isNotEmpty(toDate))   params.add("toDate=" + encode(toDate));
        }
        if (page > 1) params.add("page=" + page);
        String base = currentPage.getPath() + ".html";
        return params.isEmpty() ? base : base + "?" + String.join("&", params);
    }

    private static String encode(String v) {
        try { return URLEncoder.encode(v, "UTF-8"); }
        catch (UnsupportedEncodingException e) { return v; }
    }

    // ── Language root ──────────────────────────────────────────────────────────

    private String deriveLanguageRoot() {
        // /content/newspaper/language-masters/en/... → level 3
        if (currentPage != null) {
            Page langRoot = currentPage.getAbsoluteParent(3);
            if (langRoot != null) return langRoot.getPath();
        }
        return "/content/newspaper/language-masters";
    }

    // ── Computed display helpers ───────────────────────────────────────────────

    public String getResultsLabel() {
        if (totalCount == 0) return "No results found";
        int from = (pageNum - 1) * PAGE_SIZE + 1;
        int to   = (int) Math.min((long) pageNum * PAGE_SIZE, totalCount);
        return "Showing " + from + "\u2013" + to + " of " + totalCount
               + " result" + (totalCount == 1 ? "" : "s");
    }

    public String getPageUrl() {
        return currentPage != null ? currentPage.getPath() + ".html" : "#";
    }

    public String getLanguageCode() {
        if (currentPage != null) {
            Page lr = currentPage.getAbsoluteParent(3);
            if (lr != null) return lr.getName();
        }
        return "en";
    }

    public String getPrevPageUrl() {
        return pageNum > 1 ? buildPageUrl(pageNum - 1) : null;
    }

    public String getNextPageUrl() {
        return pageNum < totalPages ? buildPageUrl(pageNum + 1) : null;
    }

    // ── Accessors ──────────────────────────────────────────────────────────────

    public String                  getQuery()          { return query; }
    public String                  getCategory()       { return category; }
    public String                  getFromDate()       { return fromDate; }
    public String                  getToDate()         { return toDate; }
    public String                  getDatePreset()     { return datePreset; }
    public int                     getPageNum()        { return pageNum; }
    public int                     getTotalPages()     { return totalPages; }
    public long                    getTotalCount()     { return totalCount; }
    public List<SearchResultItem>  getResults()        { return results; }
    public List<CategoryItem>      getCategories()     { return categories; }
    public List<PageNumberItem>    getPageNumbers()    { return pageNumbers; }

    public boolean isHasResults()     { return !results.isEmpty(); }
    public boolean isHasPrevPage()    { return pageNum > 1; }
    public boolean isHasNextPage()    { return pageNum < totalPages; }
    public boolean isShowPagination() { return totalPages > 1; }
    /** True when the user entered manual dates with no preset — drives custom date range visibility. */
    public boolean isCustomDateMode() { return datePreset == null && (fromDate != null || toDate != null); }

    // ── Inner value objects ────────────────────────────────────────────────────

    public static final class CategoryItem {
        private final String name;
        private final String title;

        public CategoryItem(String name, String title) {
            this.name  = name;
            this.title = title;
        }

        public String getName()  { return name; }
        public String getTitle() { return StringUtils.isNotEmpty(title) ? title : name; }
    }

    public static final class PageNumberItem {
        private final int     number;
        private final boolean current;
        private final boolean ellipsis;
        private final String  url;

        public PageNumberItem(int number, boolean current, boolean ellipsis, String url) {
            this.number   = number;
            this.current  = current;
            this.ellipsis = ellipsis;
            this.url      = url;
        }

        public int     getNumber()      { return number; }
        public boolean isCurrent()      { return current; }
        public boolean isEllipsis()     { return ellipsis; }
        public String  getUrl()         { return url; }
    }
}
