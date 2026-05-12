package com.easyfish.backend3.service;

import com.easyfish.backend3.dto.PresignedUploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class ImageService {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket:}")
    private String bucket;
    @Value("${aws.s3.region:ap-south-1}")
    private String region;
    @Value("${aws.s3.public-url:}")
    private String publicUrl;
    @Value("${aws.s3.presign-expiry-seconds:900}")
    private int presignExpirySeconds;

    private static final List<String> ALLOWED_EXT = List.of(".png", ".jpg", ".jpeg", ".webp");
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/png", "image/jpeg", "image/webp");

    public ImageService(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    public PresignedUploadResponse createPresignedUpload(String folder, String fileName, String contentType) {
        ensureBucket();
        String safeFolder = sanitizeFolder(folder);
        String safeName = safeOriginalName(fileName == null || fileName.isBlank() ? "image.webp" : fileName);
        String finalContentType = normalizeContentType(contentType, safeName);
        validateContentType(finalContentType);
        String extension = extensionFor(finalContentType, safeName);
        String key = safeFolder + "/" + UUID.randomUUID() + extension;

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(finalContentType)
                .cacheControl("public, max-age=31536000, immutable")
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(presignExpirySeconds))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        return new PresignedUploadResponse(presigned.url().toString(), buildPublicUrl(key), key, finalContentType, presignExpirySeconds);
    }

    public String upload(MultipartFile file) throws IOException {
        validateImage(file);
        String original = safeOriginalName(file.getOriginalFilename());
        String key = "products/" + UUID.randomUUID() + "_" + original;
        return uploadMultipartFile(file, key);
    }

    public String uploadBannerBytes(byte[] bytes, String contentType) {
        String key = "banners/" + UUID.randomUUID() + ".jpg";
        return uploadBytes(bytes, key, contentType == null || contentType.isBlank() ? "image/jpeg" : contentType);
    }

    public String uploadBannerOriginal(MultipartFile file) throws IOException {
        validateImage(file);
        String original = safeOriginalName(file.getOriginalFilename());
        String key = "banners/" + UUID.randomUUID() + "_" + original;
        return uploadMultipartFile(file, key);
    }

    public String uploadMultipartFile(MultipartFile file, String key) throws IOException {
        String contentType = normalizeContentType(file.getContentType(), file.getOriginalFilename());
        return uploadBytes(file.getBytes(), key, contentType);
    }

    public String uploadBytes(byte[] bytes, String key, String contentType) {
        ensureBucket();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .cacheControl("public, max-age=31536000, immutable")
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(bytes));
        return buildPublicUrl(key);
    }

    public void deleteByKey(String key) {
        ensureBucket();
        if (key == null || key.isBlank()) return;
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new RuntimeException("File is empty");
        String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename().toLowerCase();
        boolean validExt = ALLOWED_EXT.stream().anyMatch(original::endsWith);
        String contentType = normalizeContentType(file.getContentType(), original);
        if (!validExt || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new RuntimeException("Only PNG, JPG, JPEG and WEBP images are allowed");
        }
    }

    private void validateContentType(String contentType) {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new RuntimeException("Only PNG, JPG, JPEG and WEBP images are allowed");
        }
    }

    private String sanitizeFolder(String folder) {
        String value = folder == null ? "products" : folder.toLowerCase().replaceAll("[^a-z0-9/_-]", "");
        if (value.isBlank()) value = "products";
        if (!value.equals("products") && !value.equals("banners") && !value.startsWith("products/") && !value.startsWith("banners/")) {
            value = "products";
        }
        return value.replaceAll("^/+|/+$", "");
    }

    private String safeOriginalName(String originalFilename) {
        String original = originalFilename == null ? "image.webp" : originalFilename.toLowerCase();
        return original.replaceAll("[^a-z0-9._-]", "_");
    }

    private String normalizeContentType(String contentType, String filename) {
        if (contentType != null && !contentType.isBlank()) {
            if (contentType.equals("image/jpg")) return "image/jpeg";
            return contentType.toLowerCase();
        }
        String name = filename == null ? "" : filename.toLowerCase();
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private String extensionFor(String contentType, String fallbackName) {
        if ("image/png".equals(contentType)) return ".png";
        if ("image/webp".equals(contentType)) return ".webp";
        String name = fallbackName == null ? "" : fallbackName.toLowerCase();
        if (name.endsWith(".jpg")) return ".jpg";
        return ".jpeg";
    }

    private void ensureBucket() {
        if (bucket == null || bucket.isBlank()) throw new RuntimeException("AWS S3 bucket is not configured. Set AWS_S3_BUCKET.");
    }

    private String buildPublicUrl(String key) {
        String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8).replace("+", "%20").replace("%2F", "/");
        if (publicUrl != null && !publicUrl.isBlank()) return publicUrl.replaceAll("/+$", "") + "/" + encodedKey;
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + encodedKey;
    }
}
