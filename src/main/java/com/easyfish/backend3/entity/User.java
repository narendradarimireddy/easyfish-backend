package com.easyfish.backend3.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "users")
public class User {

    @Id
    @Column(nullable = false, unique = true, length = 80)
    private String phone;

    private String name;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String googleId;

    @Column(length = 1000)
    private String profilePicture;

    private String otp;
    private LocalDateTime otpGeneratedAt;
    private boolean verified;

    @Enumerated(EnumType.STRING)
    private Role role;

    public String getId() { return phone; }
    public void setId(String id) { this.phone = id; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
    public LocalDateTime getOtpGeneratedAt() { return otpGeneratedAt; }
    public void setOtpGeneratedAt(LocalDateTime otpGeneratedAt) { this.otpGeneratedAt = otpGeneratedAt; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }
    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

    // Kept only for old JSON compatibility. Password login is removed.
    public String getPassword() { return null; }
    public void setPassword(String password) { }
}
