package com.easyfish.backend3.dto;

import com.easyfish.backend3.entity.Role;

public class AuthResponse {
    private String id;
    private String name;
    private String phone;
    private String email;
    private String profilePicture;
    private Role role;
    private String token;
    private boolean profileRequired;

    public AuthResponse() {}

    public AuthResponse(String id, String name, String phone, Role role, String token, boolean profileRequired) {
        this(id, name, phone, null, null, role, token, profileRequired);
    }

    public AuthResponse(String id, String name, String phone, String email, String profilePicture, Role role, String token, boolean profileRequired) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.profilePicture = profilePicture;
        this.role = role;
        this.token = token;
        this.profileRequired = profileRequired;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public boolean isProfileRequired() { return profileRequired; }
    public void setProfileRequired(boolean profileRequired) { this.profileRequired = profileRequired; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }
}
