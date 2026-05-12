package com.easyfish.backend3.service;

import com.easyfish.backend3.entity.Product;
import com.easyfish.backend3.repository.ProductRepository;
import com.easyfish.backend3.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository repo;
    private final ReviewRepository reviewRepository;

    public ProductService(ProductRepository repo, ReviewRepository reviewRepository) {
        this.repo = repo;
        this.reviewRepository = reviewRepository;
    }

    public Product add(Product p) {
        normalizePrice(p);
        if (p.getRating() == null) {
            p.setRating(0.0);
        }
        Product saved = repo.save(p);
        populateReviewSummary(saved);
        return saved;
    }

    public List<Product> getAll() {
        List<Product> products = repo.findAll();
        products.forEach(this::populateReviewSummary);
        return products;
    }

    public List<Product> searchByName(String name) {
        List<Product> products = repo.findByNameContainingIgnoreCaseOrLocalNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(name, name, name);
        products.forEach(this::populateReviewSummary);
        return products;
    }

    public Product update(Long id, Product p) {
        Product existing = repo.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
        existing.setName(p.getName());
        existing.setLocalName(p.getLocalName());
        existing.setCategory(p.getCategory());
        existing.setDescription(p.getDescription());
        existing.setPrice(p.getPrice());
        existing.setDiscount(p.getDiscount());
        existing.setInStock(p.getInStock());
        existing.setTag(p.getTag());
        existing.setUnit(p.getUnit());
        existing.setQuantity(p.getQuantity());
        existing.setImageUrl(p.getImageUrl());
        existing.setAdditionalImagesText(p.getAdditionalImagesText());
        normalizePrice(existing);
        Product saved = repo.save(existing);
        populateReviewSummary(saved);
        return saved;
    }

    private void normalizePrice(Product p) {
        double price = p.getPrice() == null ? 0 : p.getPrice();
        double discount = p.getDiscount() == null ? 0 : p.getDiscount();
        p.setFinalPrice(price - (price * discount / 100.0));
    }

    private void populateReviewSummary(Product product) {
        int count = reviewRepository.findByProductId(product.getId()).size();
        product.setReviewCount(count);
        if (product.getRating() == null) {
            product.setRating(0.0);
        }
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}
