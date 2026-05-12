package com.easyfish.backend3.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Service
public class Fast2SmsService {
    @Value("${fast2sms.api-key:}")
    private String apiKey;

    /*
     * Use Fast2SMS OTP route by default instead of Quick SMS route.
     *
     * OLD:
     *   FAST2SMS_ROUTE=q
     *   This uses Quick SMS and can cost much higher per SMS.
     *
     * NEW:
     *   FAST2SMS_ROUTE=otp
     *   This uses Fast2SMS OTP route with variables_values.
     *
     * If your Fast2SMS account has DLT transactional template approved,
     * set FAST2SMS_ROUTE=dlt and add FAST2SMS_SENDER_ID + FAST2SMS_TEMPLATE_ID.
     */
    @Value("${fast2sms.route:otp}")
    private String route;

    @Value("${fast2sms.sender-id:}")
    private String senderId;

    @Value("${fast2sms.template-id:}")
    private String templateId;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendLoginOtp(String phone, String otp) {
        sendOtp(phone, otp);
    }

    public void sendDeliveryOtp(String phone, String otp) {
        sendOtp(phone, otp);
    }

    private void sendOtp(String phone, String otp) {
        String cleanPhone = normalizePhone(phone);
        String cleanOtp = normalizeOtp(otp);

        if (apiKey == null || apiKey.isBlank() || apiKey.equalsIgnoreCase("your_fast2sms_api_key_here")) {
            throw new RuntimeException("Fast2SMS API key missing in environment variables");
        }

        String selectedRoute = route == null || route.isBlank() ? "otp" : route.trim().toLowerCase();

        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", apiKey.trim());
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        if ("otp".equals(selectedRoute)) {
            /*
             * Fast2SMS OTP route.
             * This avoids Quick SMS route charges and uses OTP variables.
             */
            body.add("route", "otp");
            body.add("variables_values", cleanOtp);
            body.add("numbers", cleanPhone);
        } else if ("dlt".equals(selectedRoute)) {
            /*
             * DLT route requires approved Sender ID and Template ID in Fast2SMS.
             * Template should contain one variable for OTP.
             */
            body.add("route", "dlt");
            body.add("message", cleanOtp);
            body.add("variables_values", cleanOtp);
            body.add("numbers", cleanPhone);

            if (senderId != null && !senderId.isBlank()) {
                body.add("sender_id", senderId.trim());
            }
            if (templateId != null && !templateId.isBlank()) {
                body.add("template_id", templateId.trim());
            }
        } else {
            /*
             * Fallback for old accounts. Avoid using this in production OTP
             * because Quick SMS may cost more.
             */
            body.add("route", selectedRoute);
            body.add("message", "Your Easyfish OTP is " + cleanOtp);
            body.add("language", "english");
            body.add("flash", "0");
            body.add("numbers", cleanPhone);

            if (senderId != null && !senderId.isBlank() && !"FSTSMS".equalsIgnoreCase(senderId.trim())) {
                body.add("sender_id", senderId.trim());
            }
        }

        try {
            ResponseEntity<String> res = restTemplate.postForEntity(
                    "https://www.fast2sms.com/dev/bulkV2",
                    new HttpEntity<>(body, headers),
                    String.class
            );

            String response = res.getBody() == null ? "" : res.getBody();
            String lower = response.toLowerCase();

            if (!res.getStatusCode().is2xxSuccessful()
                    || lower.contains("\"return\":false")
                    || lower.contains("false")) {
                throw new RuntimeException("Fast2SMS rejected OTP: " + response);
            }
        } catch (HttpStatusCodeException e) {
            String response = e.getResponseBodyAsString();
            throw new RuntimeException("Fast2SMS error " + e.getStatusCode().value() + ": " + response);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage() == null ? "Fast2SMS OTP send failed" : e.getMessage());
        }
    }

    private String normalizePhone(String phone) {
        String p = phone == null ? "" : phone.replaceAll("\\D", "");
        if (p.startsWith("91") && p.length() == 12) p = p.substring(2);
        if (!p.matches("[6-9]\\d{9}")) throw new RuntimeException("Enter valid 10 digit phone number");
        return p;
    }

    private String normalizeOtp(String otp) {
        String value = otp == null ? "" : otp.replaceAll("\\D", "");
        if (!value.matches("\\d{4,8}")) {
            throw new RuntimeException("Invalid OTP generated");
        }
        return value;
    }
}
