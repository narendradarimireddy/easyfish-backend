package com.easyfish.backend3.dto;

public class CheckoutItemRequest {
    private Long productId;
    private Integer quantity;
    private String packQuantity;
    private String packUnit;
    private Integer totalGrams;

    public CheckoutItemRequest() {}
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getPackQuantity() { return packQuantity; }
    public void setPackQuantity(String packQuantity) { this.packQuantity = packQuantity; }
    public String getPackUnit() { return packUnit; }
    public void setPackUnit(String packUnit) { this.packUnit = packUnit; }
    public Integer getTotalGrams() { return totalGrams; }
    public void setTotalGrams(Integer totalGrams) { this.totalGrams = totalGrams; }
}
