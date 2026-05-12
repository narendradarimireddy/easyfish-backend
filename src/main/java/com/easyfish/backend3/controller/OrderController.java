package com.easyfish.backend3.controller;

import com.easyfish.backend3.dto.CheckoutRequest;
import com.easyfish.backend3.dto.CheckoutResponse;
import com.easyfish.backend3.entity.Order;
import com.easyfish.backend3.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(@RequestBody CheckoutRequest request) throws Exception {
        return service.checkout(request);
    }

    @GetMapping("/user/{userId}")
    public List<Order> byUser(@PathVariable String userId) {
        return service.getOrdersByUser(userId);
    }

    @GetMapping("/admin/all")
    public List<Order> allOrders() {
        return service.getAllOrders();
    }

    @PutMapping("/{orderId}/status")
    public Order updateStatus(@PathVariable Long orderId, @RequestBody Map<String, String> body) {
        return service.updateStatus(orderId, body.getOrDefault("status", "ORDER_PLACED"));
    }

    @PutMapping("/{orderId}/mark-paid")
    public Order markPaid(@PathVariable Long orderId, @RequestBody(required = false) Map<String, String> body) {
        String transactionId = body == null ? null : body.get("transactionId");
        String gatewayOrderId = body == null ? null : body.get("gatewayOrderId");
        return service.markPaymentSuccess(orderId, transactionId, gatewayOrderId);
    }

    @PostMapping("/{orderId}/delivery/send-otp")
    public Order sendDeliveryOtp(@PathVariable Long orderId) {
        return service.sendDeliveryOtp(orderId);
    }

    @PostMapping("/{orderId}/delivery/verify-otp")
    public Order verifyDeliveryOtp(@PathVariable Long orderId, @RequestBody Map<String, String> body) {
        return service.verifyDeliveryOtp(orderId, body.getOrDefault("otp", ""));
    }

    @PostMapping("/{orderId}/delivery/cash")
    public Order markDeliveredCash(@PathVariable Long orderId) {
        return service.markDeliveredCash(orderId);
    }
}
