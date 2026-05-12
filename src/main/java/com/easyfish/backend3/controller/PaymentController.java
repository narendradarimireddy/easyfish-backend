package com.easyfish.backend3.controller;

import com.easyfish.backend3.service.PaymentVerificationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentVerificationService service;

    // ✅ FIX: manual constructor
    public PaymentController(PaymentVerificationService service) {
        this.service = service;
    }

    @PostMapping("/verify")
    public String verify(@RequestParam String orderId,
                         @RequestParam String paymentId,
                         @RequestParam String signature) throws Exception {

        boolean valid = service.verify(orderId, paymentId, signature);

        if (valid) return "Payment Verified";
        return "Invalid Payment";
    }
}