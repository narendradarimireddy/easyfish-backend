package com.easyfish.backend3.service;

import com.easyfish.backend3.entity.Product;
import com.easyfish.backend3.repository.ProductRepository;
import com.easyfish.backend3.repository.ReviewRepository;
import com.easyfish.backend3.repository.OrderItemRepository;
import com.easyfish.backend3.repository.StockHistoryRepository;
import com.easyfish.backend3.repository.CartItemRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class ProductService {
    private final ProductRepository repo;
    private final ReviewRepository reviewRepository;
    private final ImageService imageService;
    private final StockHistoryRepository stockHistoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;

    public ProductService(ProductRepository repo, ReviewRepository reviewRepository, ImageService imageService, StockHistoryRepository stockHistoryRepository, OrderItemRepository orderItemRepository, CartItemRepository cartItemRepository) {
        this.repo = repo;
        this.reviewRepository = reviewRepository;
        this.imageService = imageService;
        this.stockHistoryRepository = stockHistoryRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartItemRepository = cartItemRepository;
    }

    public Product add(Product p) {
        normalizePrice(p);
        normalizeStock(p);
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
        existing.setStockQuantity(p.getStockQuantity());
        existing.setStockUnit(p.getStockUnit());
        existing.setLowStockLimit(p.getLowStockLimit());
        existing.setStockStatus(p.getStockStatus());
        normalizeStock(existing);
        existing.setTag(p.getTag());
        existing.setUnit(p.getUnit());
        existing.setQuantity(p.getQuantity());
        String oldImageUrl = existing.getImageUrl();
        String oldAdditionalImages = existing.getAdditionalImagesText();
        existing.setImageUrl(p.getImageUrl());
        existing.setAdditionalImagesText(p.getAdditionalImagesText());
        normalizePrice(existing);
        Product saved = repo.save(existing);
        deleteIfChanged(oldImageUrl, saved.getImageUrl());
        deleteRemovedGalleryImages(oldAdditionalImages, saved.getAdditionalImagesText());
        populateReviewSummary(saved);
        return saved;
    }

    private void normalizeStock(Product p) {
        double stock = p.getStockQuantity() == null ? 0 : p.getStockQuantity();
        double low = p.getLowStockLimit() == null ? 0 : p.getLowStockLimit();
        if (p.getStockUnit() == null || p.getStockUnit().isBlank()) p.setStockUnit("kg");
        p.setInStock(stock > 0);
        if (stock <= 0) p.setStockStatus("OUT_OF_STOCK");
        else if (low > 0 && stock <= low) p.setStockStatus("LOW_STOCK");
        else p.setStockStatus("IN_STOCK");
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

    @Transactional
    public void delete(Long id) {
        Product selected = repo.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
        String baseName = selected.getName() == null ? "" : selected.getName().trim().toLowerCase();
        String baseLocal = selected.getLocalName() == null ? "" : selected.getLocalName().trim().toLowerCase();
        List<Product> productsToDelete = repo.findAll().stream()
                .filter(item -> (item.getName() == null ? "" : item.getName().trim().toLowerCase()).equals(baseName)
                        && (item.getLocalName() == null ? "" : item.getLocalName().trim().toLowerCase()).equals(baseLocal))
                .toList();
        if (productsToDelete.isEmpty()) productsToDelete = List.of(selected);

        for (Product product : productsToDelete) {
            Long productId = product.getId();
            String oldImageUrl = product.getImageUrl();
            String oldAdditionalImages = product.getAdditionalImagesText();
            stockHistoryRepository.deleteByProductId(productId);
            reviewRepository.deleteByProductId(productId);
            cartItemRepository.deleteByProductId(productId);
            cartItemRepository.flush();
            orderItemRepository.detachProduct(productId);
            orderItemRepository.flush();
            repo.delete(product);
            repo.flush();
            imageService.deleteByUrl(oldImageUrl);
            imageUrls(oldAdditionalImages).forEach(imageService::deleteByUrl);
        }
    }

    private void deleteIfChanged(String oldUrl, String newUrl) {
        if (oldUrl != null && !oldUrl.isBlank() && (newUrl == null || !oldUrl.equals(newUrl))) {
            imageService.deleteByUrl(oldUrl);
        }
    }

    private void deleteRemovedGalleryImages(String oldText, String newText) {
        List<String> newUrls = imageUrls(newText);
        imageUrls(oldText).stream()
                .filter(oldUrl -> !newUrls.contains(oldUrl))
                .forEach(imageService::deleteByUrl);
    }

    private List<String> imageUrls(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }
}
