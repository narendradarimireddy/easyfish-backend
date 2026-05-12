package com.easyfish.backend3.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RazorpayService {

    @Value("${razorpay.key-id:dummy}")
    private String keyId;

    @Value("${razorpay.key-secret:dummy}")
    private String keySecret;

    public JSONObject createOrder(Double amount) throws Exception {
        RazorpayClient client = new RazorpayClient(keyId, keySecret);
        JSONObject options = new JSONObject();
        options.put("amount", Math.round(amount * 100));
        options.put("currency", "INR");
        options.put("receipt", "easyfish_" + System.currentTimeMillis());
        Order order = client.orders.create(options);
        return new JSONObject(order.toString());
    }
}
