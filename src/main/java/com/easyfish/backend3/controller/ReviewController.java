package com.easyfish.backend3.controller;

import com.easyfish.backend3.dto.ReviewRequest;
import com.easyfish.backend3.entity.Review;
import com.easyfish.backend3.service.ReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public Review save(@RequestBody ReviewRequest request) {
        return reviewService.save(request);
    }

    @GetMapping("/product/{productId}")
    public List<Review> getByProduct(@PathVariable Long productId) {
        return reviewService.getByProduct(productId);
    }
}
