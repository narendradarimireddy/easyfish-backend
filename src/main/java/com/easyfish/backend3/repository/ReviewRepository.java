package com.easyfish.backend3.repository;

import com.easyfish.backend3.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByProductIdAndUserPhone(Long productId, String phone);
    List<Review> findByProductId(Long productId);
}
