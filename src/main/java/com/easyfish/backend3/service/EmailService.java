package com.easyfish.backend3.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    // ✅ FIX: manual constructor
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String to, String otp) {
        sendPlainText(to, "EasyFish OTP Verification", "Your OTP is: " + otp);
    }

    public void sendPlainText(String to, String subject, String body) {
        if (to == null || to.isBlank()) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to.trim());
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}