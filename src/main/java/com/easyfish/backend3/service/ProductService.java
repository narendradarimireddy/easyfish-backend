package com.easyfish.backend3.service;

import com.easyfish.backend3.entity.Product;
import com.easyfish.backend3.repository.ProductRepository;
import com.easyfish.backend3.repository.ReviewRepository;
import com.easyfish.backend3.repository.OrderItemRepository;
import com.easyfish.backend3.repository.StockHistoryRepository;
import com.easyfish.backend3.repository.CartItemRepository;
import com.easyfish.backend3.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository repo, ReviewRepository reviewRepository, ImageService imageService, StockHistoryRepository stockHistoryRepository, OrderItemRepository orderItemRepository, CartItemRepository cartItemRepository, CategoryRepository categoryRepository) {
        this.repo = repo;
        this.reviewRepository = reviewRepository;
        this.imageService = imageService;
        this.stockHistoryRepository = stockHistoryRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartItemRepository = cartItemRepository;
        this.categoryRepository = categoryRepository;
    }

    public Product add(Product p) {
        if (p.getCategory() == null || p.getCategory().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category is required");
        }
        p.setCategory(p.getCategory().trim());
        normalizePrice(p);
        normalizeStock(p);
        normalizeVisibility(p);
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


    public List<Product> getLiveProducts() {
        List<Product> products = repo.findAll().stream()
                .filter(this::isLiveVariantForStore)
                .toList();
        products.forEach(this::populateReviewSummary);
        return products;
    }

    private boolean isLiveVariantForStore(Product p) {
        double price = p.getFinalPrice() != null ? p.getFinalPrice() : (p.getPrice() == null ? 0 : p.getPrice());
        String qty = p.getQuantity() == null ? "" : p.getQuantity().trim();
        String unit = p.getUnit() == null ? "" : p.getUnit().trim();
        String status = p.getStatus() == null || p.getStatus().isBlank() ? "ACTIVE" : p.getStatus().trim().toUpperCase();
        boolean hasPack = !qty.isBlank() && !"0".equals(qty) && !unit.isBlank();
        boolean inactive = Boolean.FALSE.equals(p.getActive()) || Boolean.FALSE.equals(p.getPublished())
                || status.equals("DRAFT") || status.equals("INACTIVE") || status.equals("DELETED")
                || status.equals("STOCK") || status.equals("STOCK_MASTER");
        double stock = p.getStockQuantity() == null ? 1 : p.getStockQuantity();
        String stockStatus = p.getStockStatus() == null ? "" : p.getStockStatus().trim().toUpperCase();
        boolean explicitlyOutOfStock = Boolean.FALSE.equals(p.getInStock()) || (stockStatus.equals("OUT_OF_STOCK") && stock <= 0);
        boolean activeCategory = p.getCategory() != null && categoryRepository.findByNameIgnoreCase(p.getCategory().trim())
                .map(category -> !Boolean.FALSE.equals(category.getActive()))
                .orElse(false);
        return activeCategory && !inactive && !explicitlyOutOfStock && price > 0 && hasPack;
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
        existing.setActive(p.getActive());
        existing.setPublished(p.getPublished());
        existing.setStatus(p.getStatus());
        normalizePrice(existing);
        normalizeVisibility(existing);
        Product saved = repo.save(existing);
        deleteIfChanged(oldImageUrl, saved.getImageUrl());
        deleteRemovedGalleryImages(oldAdditionalImages, saved.getAdditionalImagesText());
        populateReviewSummary(saved);
        return saved;
    }

    private void normalizeVisibility(Product p) {
        double price = p.getFinalPrice() != null ? p.getFinalPrice() : (p.getPrice() == null ? 0 : p.getPrice());
        String qty = p.getQuantity() == null ? "" : p.getQuantity().trim();
        boolean hasPack = !qty.isBlank() && !"0".equals(qty) && p.getUnit() != null && !p.getUnit().isBlank();
        boolean looksPublished = price > 0 && hasPack;
        if (p.getActive() == null) p.setActive(true);
        if (p.getPublished() == null) p.setPublished(looksPublished);
        if (p.getStatus() == null || p.getStatus().isBlank()) {
            p.setStatus(Boolean.TRUE.equals(p.getPublished()) ? "ACTIVE" : "STOCK");
        }
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
        deleteExactProducts(productsToDelete);
    }

    @Transactional
    public int deleteByCategoryName(String categoryName) {
        String target = categoryName == null ? "" : categoryName.trim().toLowerCase();
        if (target.isBlank()) return 0;
        List<Product> productsToDelete = repo.findByCategoryNameNormalized(categoryName.trim());
        deleteExactProducts(productsToDelete);
        return productsToDelete.size();
    }

    public int countByCategoryName(String categoryName) {
        String target = categoryName == null ? "" : categoryName.trim().toLowerCase();
        if (target.isBlank()) return 0;
        return (int) repo.countByCategoryNameNormalized(categoryName.trim());
    }

    private void deleteExactProducts(List<Product> productsToDelete) {
        for (Product product : productsToDelete) {
            if (product == null || product.getId() == null) continue;
            Product managedProduct = repo.findById(product.getId()).orElse(null);
            if (managedProduct == null) continue;
            product = managedProduct;
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
