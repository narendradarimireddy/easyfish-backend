package com.easyfish.backend3.controller;

import com.easyfish.backend3.dto.CartItemRequest;
import com.easyfish.backend3.entity.CartItem;
import com.easyfish.backend3.entity.Product;
import com.easyfish.backend3.repository.CartItemRepository;
import com.easyfish.backend3.repository.ProductRepository;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartItemRepository cartRepository;
    private final ProductRepository productRepository;

    public CartController(CartItemRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/user/{userId}")
    public List<Map<String, Object>> getUserCart(@PathVariable String userId) {
        return cartRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @PutMapping("/user/{userId}")
    public List<Map<String, Object>> replaceUserCart(@PathVariable String userId, @RequestBody List<CartItemRequest> items) {
        cartRepository.deleteByUserId(userId);
        if (items != null) {
            for (CartItemRequest request : items) {
                if (request == null || request.getProductId() == null) continue;
                Optional<Product> productOpt = productRepository.findById(request.getProductId());
                if (productOpt.isEmpty()) continue;
                int qty = Math.max(1, Math.min(4, request.getQuantity() == null ? 1 : request.getQuantity()));
                CartItem item = new CartItem();
                item.setUserId(userId);
                item.setProduct(productOpt.get());
                item.setQuantity(qty);
                item.setSelected(request.getSelected() == null || request.getSelected());
                cartRepository.save(item);
            }
        }
        return getUserCart(userId);
    }

    @DeleteMapping("/user/{userId}/product/{productId}")
    public Map<String, String> deleteProductFromCart(@PathVariable String userId, @PathVariable Long productId) {
        List<CartItem> items = cartRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        for (CartItem item : items) {
            if (item.getProduct() != null && Objects.equals(item.getProduct().getId(), productId)) {
                cartRepository.delete(item);
            }
        }
        return Map.of("message", "Deleted");
    }

    @DeleteMapping("/user/{userId}")
    public Map<String, String> clearCart(@PathVariable String userId) {
        cartRepository.deleteByUserId(userId);
        return Map.of("message", "Cart cleared");
    }

    private Map<String, Object> toResponse(CartItem item) {
        Product p = item.getProduct();
        Map<String, Object> map = new LinkedHashMap<>();
        if (p != null) {
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("localName", p.getLocalName());
            map.put("category", p.getCategory());
            map.put("description", p.getDescription());
            map.put("price", p.getPrice());
            map.put("discount", p.getDiscount());
            map.put("finalPrice", p.getFinalPrice());
            map.put("inStock", p.getInStock());
            map.put("stockQuantity", p.getStockQuantity());
            map.put("stockUnit", p.getStockUnit());
            map.put("stockStatus", p.getStockStatus());
            map.put("tag", p.getTag());
            map.put("rating", p.getRating());
            map.put("unit", p.getUnit());
            map.put("quantity", p.getQuantity());
            map.put("imageUrl", p.getImageUrl());
            map.put("additionalImagesText", p.getAdditionalImagesText());
        }
        map.put("cartQty", item.getQuantity());
        map.put("selected", item.getSelected() == null || item.getSelected());
        return map;
    }
}
