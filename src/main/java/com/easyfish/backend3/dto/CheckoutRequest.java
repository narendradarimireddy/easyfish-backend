package com.easyfish.backend3.dto;

public class CheckoutRequest {
    private String userId;
    private Long productId;
    private Integer quantity;
    private String packQuantity;
    private String packUnit;
    private Integer totalGrams;
    private String paymentMode;
    private DeliveryAddressDto address;

    public CheckoutRequest() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
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
    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }
    public DeliveryAddressDto getAddress() { return address; }
    public void setAddress(DeliveryAddressDto address) { this.address = address; }
}
