package com.easyfish.backend3.controller;

import com.easyfish.backend3.dto.PresignedUploadRequest;
import com.easyfish.backend3.dto.PresignedUploadResponse;
import com.easyfish.backend3.entity.Product;
import com.easyfish.backend3.service.ImageService;
import com.easyfish.backend3.service.ProductService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;
    private final ImageService imageService;

    public ProductController(ProductService service, ImageService imageService) {
        this.service = service;
        this.imageService = imageService;
    }

    @PostMapping("/admin/add")
    public Product add(@RequestBody Product product) { return service.add(product); }

    @GetMapping
    public List<Product> getAll() { return service.getAll(); }

    @GetMapping("/search")
    public List<Product> search(@RequestParam String name) { return service.searchByName(name); }

    @PutMapping("/admin/update/{id}")
    public Product update(@PathVariable Long id, @RequestBody Product product) { return service.update(id, product); }

    @DeleteMapping("/admin/delete/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        service.delete(id);
        Map<String, String> map = new HashMap<>();
        map.put("message", "Deleted");
        return map;
    }

    // Production direct upload flow: frontend asks backend for a signed S3 PUT URL,
    // uploads compressed image directly to S3, and stores only publicUrl in RDS.
    @PostMapping("/admin/upload/presign")
    public PresignedUploadResponse presignUpload(@RequestBody PresignedUploadRequest request) {
        return imageService.createPresignedUpload(
                request.getFolder(),
                request.getFileName(),
                request.getContentType()
        );
    }

    // Backward compatible fallback upload endpoint. Keep this for older admin builds.
    @PostMapping(value = "/admin/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("url", imageService.upload(file));
        return map;
    }
}
