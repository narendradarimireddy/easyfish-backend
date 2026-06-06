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

    @DeleteMapping("/admin/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        service.delete(id);
        Map<String, String> res = new HashMap<>();
        res.put("message", "Deleted");
        return res;
    }
}
