package com.easyfish.backend3.dto;

public class PresignedUploadResponse {
    private String uploadUrl;
    private String publicUrl;
    private String key;
    private String contentType;
    private int expiresInSeconds;
    public PresignedUploadResponse(String uploadUrl, String publicUrl, String key, String contentType, int expiresInSeconds) {
        this.uploadUrl = uploadUrl;
        this.publicUrl = publicUrl;
        this.key = key;
        this.contentType = contentType;
        this.expiresInSeconds = expiresInSeconds;
    }
    public String getUploadUrl() { return uploadUrl; }
    public String getPublicUrl() { return publicUrl; }
    public String getKey() { return key; }
    public String getContentType() { return contentType; }
    public int getExpiresInSeconds() { return expiresInSeconds; }
}
