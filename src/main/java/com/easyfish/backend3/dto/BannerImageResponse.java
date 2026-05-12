package com.easyfish.backend3.dto;

public class BannerImageResponse {

    private Long id;
    private String title;
    private String imageUrl;
    private Integer sortOrder;
    private Boolean active;

    // Constructor
    public BannerImageResponse(Long id, String title, String imageUrl, Integer sortOrder, Boolean active) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
        this.active = active;
    }

    // Default constructor
    public BannerImageResponse() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}