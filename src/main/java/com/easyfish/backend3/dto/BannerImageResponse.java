package com.easyfish.backend3.dto;

public class BannerImageResponse {
    private Long id;
    private String title;
    private String imageUrl;
    private Integer sortOrder;
    private Boolean active;
    private Long targetProductId;
    private String promotionType;

    public BannerImageResponse(Long id, String title, String imageUrl, Integer sortOrder, Boolean active) {
        this(id, title, imageUrl, sortOrder, active, null, "GENERAL");
    }

    public BannerImageResponse(Long id, String title, String imageUrl, Integer sortOrder, Boolean active, Long targetProductId) {
        this(id, title, imageUrl, sortOrder, active, targetProductId, "GENERAL");
    }

    public BannerImageResponse(Long id, String title, String imageUrl, Integer sortOrder, Boolean active, Long targetProductId, String promotionType) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
        this.active = active;
        this.targetProductId = targetProductId;
        this.promotionType = promotionType == null ? "GENERAL" : promotionType;
    }

    public BannerImageResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Long getTargetProductId() { return targetProductId; }
    public void setTargetProductId(Long targetProductId) { this.targetProductId = targetProductId; }
    public String getPromotionType() { return promotionType == null ? "GENERAL" : promotionType; }
    public void setPromotionType(String promotionType) { this.promotionType = promotionType; }
}
