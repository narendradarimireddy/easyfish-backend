package com.easyfish.backend3.dto;

import lombok.Data;

@Data
public class ReviewRequest {
    private Long productId;
    private String userId;
    private Integer stars;
    private String comment;
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Integer getStars() { return stars; }
    public void setStars(Integer stars) { this.stars = stars; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
