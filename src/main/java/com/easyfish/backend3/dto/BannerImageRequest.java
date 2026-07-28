package com.easyfish.backend3.dto;

public class BannerImageRequest {
    private String title;
    private String imageUrl;
    private Integer sortOrder;
    private Boolean active;
    private Long targetProductId;
    private String promotionType;

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
    public String getPromotionType() { return promotionType; }
    public void setPromotionType(String promotionType) { this.promotionType = promotionType; }
}
