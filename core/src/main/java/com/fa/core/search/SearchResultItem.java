package com.fa.core.search;

import java.util.Collections;
import java.util.List;

public final class SearchResultItem {

    private final String       path;
    private final String       title;
    private final String       description;
    private final String       category;
    private final String       publishedDate;
    private final String       imageUrl;
    private final String       author;
    private final List<String> tags;

    public SearchResultItem(String path, String title, String description,
                             String category, String publishedDate,
                             String imageUrl, String author,
                             List<String> tags) {
        this.path          = path;
        this.title         = title;
        this.description   = description;
        this.category      = category;
        this.publishedDate = publishedDate;
        this.imageUrl      = imageUrl;
        this.author        = author;
        this.tags          = tags != null ? tags : Collections.emptyList();
    }

    public String       getPath()          { return path; }
    public String       getTitle()         { return title; }
    public String       getDescription()   { return description; }
    public String       getCategory()      { return category; }
    public String       getPublishedDate() { return publishedDate; }
    public String       getImageUrl()      { return imageUrl; }
    public String       getAuthor()        { return author; }
    public List<String> getTags()          { return tags; }
}
