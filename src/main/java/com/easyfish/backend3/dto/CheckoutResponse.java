package com.easyfish.backend3.dto;

public class CheckoutResponse {
    private Long orderId;
    private String status;
    private String paymentStatus;
    private String paymentMode;
    private String razorpayOrderId;
    private Double productSubtotal;
    private Double deliveryCharge;
    private Double finalAmount;
    private String mapLink;

    public CheckoutResponse() {}

    public CheckoutResponse(Long orderId, String status, String paymentStatus, String paymentMode, String razorpayOrderId) {
        this.orderId = orderId;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.paymentMode = paymentMode;
        this.razorpayOrderId = razorpayOrderId;
    }

    public CheckoutResponse(Long orderId, String status, String paymentStatus, String paymentMode, String razorpayOrderId, Double productSubtotal, Double deliveryCharge, Double finalAmount, String mapLink) {
        this.orderId = orderId;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.paymentMode = paymentMode;
        this.razorpayOrderId = razorpayOrderId;
        this.productSubtotal = productSubtotal;
        this.deliveryCharge = deliveryCharge;
        this.finalAmount = finalAmount;
        this.mapLink = mapLink;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }

    public Double getProductSubtotal() { return productSubtotal; }
    public void setProductSubtotal(Double productSubtotal) { this.productSubtotal = productSubtotal; }

    public Double getDeliveryCharge() { return deliveryCharge; }
    public void setDeliveryCharge(Double deliveryCharge) { this.deliveryCharge = deliveryCharge; }

    public Double getFinalAmount() { return finalAmount; }
    public void setFinalAmount(Double finalAmount) { this.finalAmount = finalAmount; }

    public String getMapLink() { return mapLink; }
    public void setMapLink(String mapLink) { this.mapLink = mapLink; }
}
