package com.easyfish.backend3.repository;

import com.easyfish.backend3.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByNameContainingIgnoreCaseOrLocalNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(
            String name, String localName, String category);
}
