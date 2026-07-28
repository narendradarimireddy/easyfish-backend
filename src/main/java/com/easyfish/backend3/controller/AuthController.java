package com.easyfish.backend3.controller;

import com.easyfish.backend3.dto.AuthRequest;
import com.easyfish.backend3.dto.AuthResponse;
import com.easyfish.backend3.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) { this.service = service; }

    @PostMapping("/google")
    public AuthResponse googleLogin(@RequestBody Map<String, String> body) {
        return service.googleLogin(body.getOrDefault("credential", ""));
    }

    // Phone login is removed. Kept only to return a clear message to old clients.
    @PostMapping("/direct-login")
    public AuthResponse directLogin(@RequestBody Map<String, String> body) {
        return service.directLogin(body.getOrDefault("phone", ""));
    }

    @PostMapping("/send-otp")
    public Map<String, String> sendOtp(@RequestBody Map<String, String> body) {
        return Map.of("message", service.sendLoginOtp(body.getOrDefault("phone", "")));
    }

    @PostMapping("/verify-otp")
    public AuthResponse verifyOtp(@RequestBody Map<String, String> body) {
        return service.verifyLoginOtp(body.getOrDefault("phone", ""), body.getOrDefault("otp", ""));
    }

    @PutMapping("/profile/name")
    public AuthResponse updateName(@RequestBody Map<String, String> body) {
        return service.updateProfileName(body.getOrDefault("userId", body.getOrDefault("phone", "")), body.getOrDefault("name", ""));
    }

    @PostMapping("/register")
    public Map<String, String> register(@RequestBody AuthRequest request) { return Map.of("message", service.register(request)); }
    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@RequestBody Map<String, String> body) { return Map.of("message", service.forgotPassword("")); }
    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@RequestBody Map<String, String> body) { return Map.of("message", service.resetPassword("", "", "")); }
    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) { return service.login("", ""); }
}
