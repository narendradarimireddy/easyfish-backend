package com.easyfish.backend3.service;

import com.easyfish.backend3.entity.Category;
import com.easyfish.backend3.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository repo;
    private final ImageService imageService;
    private final ProductService productService;

    public CategoryService(CategoryRepository repo, ImageService imageService, ProductService productService) {
        this.repo = repo;
        this.imageService = imageService;
        this.productService = productService;
    }

    public List<Category> getActive() {
        return repo.findByActiveTrueOrderBySortOrderAscNameAsc();
    }

    public List<Category> getAdmin() {
        return repo.findAll().stream()
                .sorted((a, b) -> {
                    int ao = a.getSortOrder() == null ? 0 : a.getSortOrder();
                    int bo = b.getSortOrder() == null ? 0 : b.getSortOrder();
                    if (ao != bo) return Integer.compare(ao, bo);
                    return String.valueOf(a.getName()).compareToIgnoreCase(String.valueOf(b.getName()));
                })
                .toList();
    }

    public Category save(Category category) {
        normalize(category);
        return repo.save(category);
    }

    public Category update(Long id, Category incoming) {
        Category existing = repo.findById(id).orElseThrow(() -> new RuntimeException("Category not found"));
        existing.setName(incoming.getName());
        existing.setSlug(incoming.getSlug());
        existing.setSubtitle(incoming.getSubtitle());
        String oldImageUrl = existing.getImageUrl();
        existing.setImageUrl(incoming.getImageUrl());
        existing.setActive(incoming.getActive());
        existing.setSortOrder(incoming.getSortOrder());
        normalize(existing);
        Category saved = repo.save(existing);
        deleteIfChanged(oldImageUrl, saved.getImageUrl());
        return saved;
    }

    @Transactional
    public CategoryDeleteSummary delete(Long id) {
        Category existing = repo.findById(id).orElseThrow(() -> new RuntimeException("Category not found"));
        String oldImageUrl = existing.getImageUrl();
        int deletedProducts = productService.deleteByCategoryName(existing.getName());
        repo.delete(existing);
        repo.flush();
        imageService.deleteByUrl(oldImageUrl);
        return new CategoryDeleteSummary(existing.getName(), deletedProducts);
    }

    public CategoryDeleteSummary previewDelete(Long id) {
        Category existing = repo.findById(id).orElseThrow(() -> new RuntimeException("Category not found"));
        int productCount = productService.countByCategoryName(existing.getName());
        return new CategoryDeleteSummary(existing.getName(), productCount);
    }

    public record CategoryDeleteSummary(String categoryName, int productCount) {}

    private void deleteIfChanged(String oldUrl, String newUrl) {
        if (oldUrl != null && !oldUrl.isBlank() && (newUrl == null || !oldUrl.equals(newUrl))) {
            imageService.deleteByUrl(oldUrl);
        }
    }

    private void normalize(Category c) {
        if (c.getName() != null) c.setName(c.getName().trim());
        if (c.getSlug() == null || c.getSlug().trim().isEmpty()) c.setSlug(slugify(c.getName()));
        else c.setSlug(slugify(c.getSlug()));
        if (c.getActive() == null) c.setActive(true);
        if (c.getSortOrder() == null) c.setSortOrder(0);
    }

    private String slugify(String value) {
        return String.valueOf(value == null ? "category" : value)
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
