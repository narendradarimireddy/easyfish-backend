package com.easyfish.backend3.dto;

public class PresignedUploadRequest {
    private String fileName;
    private String contentType;
    private String folder;
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getFolder() { return folder; }
    public void setFolder(String folder) { this.folder = folder; }
}
