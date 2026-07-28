package com.easyfish.backend3.service;

import com.easyfish.backend3.dto.ReviewRequest;
import com.easyfish.backend3.entity.Product;
import com.easyfish.backend3.entity.Review;
import com.easyfish.backend3.entity.User;
import com.easyfish.backend3.repository.ProductRepository;
import com.easyfish.backend3.repository.ReviewRepository;
import com.easyfish.backend3.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public Review save(ReviewRequest request) {
        Product product = productRepository.findById(request.getProductId()).orElseThrow(() -> new RuntimeException("Product not found"));
        User user = userRepository.findById(request.getUserId()).orElseThrow(() -> new RuntimeException("User not found"));

        Review review = reviewRepository.findByProductIdAndUserPhone(product.getId(), user.getPhone()).orElseGet(Review::new);
        review.setProduct(product);
        review.setUser(user);
        review.setStars(Math.max(1, Math.min(5, request.getStars() == null ? 5 : request.getStars())));
        review.setComment(request.getComment());
        Review saved = reviewRepository.save(review);
        refreshProductRating(product.getId());
        return saved;
    }

    public List<Review> getByProduct(Long productId) {
        return reviewRepository.findByProductId(productId);
    }

    public void refreshProductRating(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found"));
        List<Review> reviews = reviewRepository.findByProductId(productId);
        double avg = reviews.stream().mapToInt(r -> r.getStars() == null ? 0 : r.getStars()).average().orElse(0.0);
        product.setRating(Math.round(avg * 10.0) / 10.0);
        product.setReviewCount(reviews.size());
        productRepository.save(product);
    }
}
