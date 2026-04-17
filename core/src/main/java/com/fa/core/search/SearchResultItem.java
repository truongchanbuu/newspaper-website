package com.fa.core.search;

public final class SearchResultItem {

    private final String path;
    private final String title;
    private final String description;
    private final String category;
    private final String publishedDate;
    private final String imageUrl;
    private final String author;

    public SearchResultItem(String path, String title, String description,
                                String category, String publishedDate,
                                String imageUrl, String author) {
        this.path          = path;
        this.title         = title;
        this.description   = description;
        this.category      = category;
        this.publishedDate = publishedDate;
        this.imageUrl      = imageUrl;
        this.author        = author;
    }

    public String getPath()          { return path; }
    public String getTitle()         { return title; }
    public String getDescription()   { return description; }
    public String getCategory()      { return category; }
    public String getPublishedDate() { return publishedDate; }
    public String getImageUrl()      { return imageUrl; }
    public String getAuthor()        { return author; }
}
