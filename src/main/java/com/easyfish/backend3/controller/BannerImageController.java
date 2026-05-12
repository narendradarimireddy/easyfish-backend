package com.easyfish.backend3.controller;

import com.easyfish.backend3.dto.BannerImageRequest;
import com.easyfish.backend3.dto.BannerImageResponse;
import com.easyfish.backend3.dto.PresignedUploadRequest;
import com.easyfish.backend3.dto.PresignedUploadResponse;
import com.easyfish.backend3.service.BannerImageService;
import com.easyfish.backend3.service.ImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/banners")
@CrossOrigin(origins = "*")
public class BannerImageController {
    private final BannerImageService bannerImageService;
    private final ImageService imageService;

    public BannerImageController(BannerImageService bannerImageService, ImageService imageService) {
        this.bannerImageService = bannerImageService;
        this.imageService = imageService;
    }

    @GetMapping("/active")
    public ResponseEntity<List<BannerImageResponse>> getActiveBanners() {
        return ResponseEntity.ok(bannerImageService.getActiveBanners());
    }

    @GetMapping("/admin")
    public ResponseEntity<List<BannerImageResponse>> getAllBanners() {
        return ResponseEntity.ok(bannerImageService.getAllBanners());
    }

    @PostMapping("/admin/upload/presign")
    public PresignedUploadResponse presignBannerUpload(@RequestBody PresignedUploadRequest request) {
        return imageService.createPresignedUpload("banners", request.getFileName(), request.getContentType());
    }

    @PostMapping("/admin/url")
    public ResponseEntity<BannerImageResponse> createBannerFromUrl(@RequestBody BannerImageRequest request) {
        return ResponseEntity.ok(bannerImageService.createBannerFromUrl(request));
    }

    @PutMapping("/admin/url/{id}")
    public ResponseEntity<BannerImageResponse> updateBannerFromUrl(@PathVariable Long id, @RequestBody BannerImageRequest request) {
        return ResponseEntity.ok(bannerImageService.updateBannerFromUrl(id, request));
    }

    @PostMapping(value = "/admin", consumes = {"multipart/form-data"})
    public ResponseEntity<BannerImageResponse> createBanner(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer sortOrder,
            @RequestParam(required = false) Boolean active,
            @RequestParam("image") MultipartFile image
    ) throws IOException {
        BannerImageRequest request = new BannerImageRequest();
        request.setTitle(title);
        request.setSortOrder(sortOrder);
        request.setActive(active);
        return ResponseEntity.ok(bannerImageService.createBanner(request, image));
    }

    @PutMapping(value = "/admin/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<BannerImageResponse> updateBanner(
            @PathVariable Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer sortOrder,
            @RequestParam(required = false) Boolean active,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) throws IOException {
        BannerImageRequest request = new BannerImageRequest();
        request.setTitle(title);
        request.setSortOrder(sortOrder);
        request.setActive(active);
        return ResponseEntity.ok(bannerImageService.updateBanner(id, request, image));
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<String> deleteBanner(@PathVariable Long id) {
        bannerImageService.deleteBanner(id);
        return ResponseEntity.ok("Banner deleted successfully");
    }
}
