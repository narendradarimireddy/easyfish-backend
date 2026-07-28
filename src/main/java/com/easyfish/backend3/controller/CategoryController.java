package com.easyfish.backend3.controller;

import com.easyfish.backend3.entity.Category;
import com.easyfish.backend3.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<Category> active() {
        return service.getActive();
    }

    @GetMapping("/admin")
    public List<Category> admin() {
        return service.getAdmin();
    }

    @PostMapping("/admin")
    public Category add(@RequestBody Category category) {
        return service.save(category);
    }

    @PutMapping("/admin/{id}")
    public Category update(@PathVariable Long id, @RequestBody Category category) {
        return service.update(id, category);
    }

    @GetMapping("/admin/{id}/delete-preview")
    public Map<String, Object> deletePreview(@PathVariable Long id) {
        CategoryService.CategoryDeleteSummary summary = service.previewDelete(id);
        Map<String, Object> res = new HashMap<>();
        res.put("categoryName", summary.categoryName());
        res.put("productCount", summary.productCount());
        res.put("message", "Deleting this category will permanently delete related stock/products and remove those items from all user carts.");
        return res;
    }

    @DeleteMapping("/admin/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        CategoryService.CategoryDeleteSummary summary = service.delete(id);
        Map<String, Object> res = new HashMap<>();
        res.put("message", "Deleted");
        res.put("categoryName", summary.categoryName());
        res.put("deletedProducts", summary.productCount());
        return res;
    }
}
