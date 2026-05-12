package com.easyfish.backend3.service;

import com.easyfish.backend3.dto.AuthRequest;
import com.easyfish.backend3.dto.AuthResponse;
import com.easyfish.backend3.entity.Role;
import com.easyfish.backend3.entity.User;
import com.easyfish.backend3.repository.UserRepository;
import com.easyfish.backend3.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class AuthService {
    private final UserRepository userRepo;
    private final Fast2SmsService fast2SmsService;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${google.client-id:}")
    private String googleClientId;

    public AuthService(UserRepository userRepo, Fast2SmsService fast2SmsService, JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.fast2SmsService = fast2SmsService;
        this.jwtUtil = jwtUtil;
    }

    private String cleanPhone(String phone) {
        String p = phone == null ? "" : phone.replaceAll("\\D", "");
        if (p.startsWith("91") && p.length() == 12) p = p.substring(2);
        if (!p.matches("[6-9]\\d{9}")) throw new RuntimeException("Enter valid 10 digit phone number");
        return p;
    }

    private AuthResponse buildResponse(User user) {
        String token = jwtUtil.generateToken(user.getPhone());
        boolean profileRequired = user.getName() == null || user.getName().trim().isEmpty();
        return new AuthResponse(
                user.getPhone(),
                user.getName(),
                user.getPhone(),
                user.getEmail(),
                user.getProfilePicture(),
                user.getRole(),
                token,
                profileRequired
        );
    }

    public AuthResponse googleLogin(String credential) {
        if (credential == null || credential.trim().isEmpty()) throw new RuntimeException("Google credential is required");
        if (googleClientId == null || googleClientId.trim().isEmpty()) throw new RuntimeException("GOOGLE_CLIENT_ID is not configured on backend");

        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + credential.trim();
        Map<String, Object> googlePayload;
        try {
            googlePayload = restTemplate.getForObject(url, Map.class);
        } catch (Exception ex) {
            throw new RuntimeException("Invalid Google login token");
        }

        if (googlePayload == null) throw new RuntimeException("Invalid Google login token");
        String audience = String.valueOf(googlePayload.getOrDefault("aud", ""));
        if (!googleClientId.equals(audience)) throw new RuntimeException("Google client ID mismatch");
        String googleId = String.valueOf(googlePayload.getOrDefault("sub", ""));
        String email = String.valueOf(googlePayload.getOrDefault("email", ""));
        String emailVerified = String.valueOf(googlePayload.getOrDefault("email_verified", "false"));
        String name = String.valueOf(googlePayload.getOrDefault("name", ""));
        String picture = String.valueOf(googlePayload.getOrDefault("picture", ""));

        if (googleId.isBlank()) throw new RuntimeException("Google account ID not found");
        if (email.isBlank()) throw new RuntimeException("Google email not found");
        if (!"true".equalsIgnoreCase(emailVerified)) throw new RuntimeException("Google email is not verified");

        User user = userRepo.findByGoogleId(googleId)
                .or(() -> userRepo.findByEmail(email))
                .orElseGet(() -> {
                    User u = new User();
                    // Keep phone column as technical user ID for old order/review relationships.
                    u.setPhone(googleId);
                    u.setRole(Role.USER);
                    return u;
                });

        user.setGoogleId(googleId);
        user.setEmail(email);
        user.setName(name == null || name.isBlank() ? email : name);
        user.setProfilePicture(picture == null || picture.isBlank() ? null : picture);
        user.setVerified(true);
        user.setOtp(null);
        user.setOtpGeneratedAt(null);
        if (user.getRole() == null) user.setRole(Role.USER);
        userRepo.save(user);

        return buildResponse(user);
    }

    // Legacy phone/direct login kept disabled for compatibility/testing only. Google login is the active user login.
    public AuthResponse directLogin(String phone) {
        throw new RuntimeException("Phone login removed. Use Continue with Google.");
    }

    public String sendLoginOtp(String phone) {
        throw new RuntimeException("Phone OTP login removed. Use Continue with Google.");
    }

    public AuthResponse verifyLoginOtp(String phone, String otp) {
        throw new RuntimeException("Phone OTP login removed. Use Continue with Google.");
    }

    public AuthResponse updateProfileName(String userId, String name) {
        if (userId == null || userId.trim().isEmpty()) throw new RuntimeException("User ID is required");
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (name != null && !name.trim().isEmpty()) user.setName(name.trim());
        userRepo.save(user);
        return buildResponse(user);
    }

    public String register(AuthRequest request) { throw new RuntimeException("Registration removed. Use Continue with Google."); }
    public String forgotPassword(String email) { throw new RuntimeException("Password login removed. Use Continue with Google."); }
    public String resetPassword(String email, String otp, String newPassword) { throw new RuntimeException("Password login removed. Use Continue with Google."); }
    public AuthResponse login(String email, String password) { throw new RuntimeException("Password login removed. Use Continue with Google."); }
}
