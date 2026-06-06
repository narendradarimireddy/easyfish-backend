package com.easyfish.backend3.repository;

import com.easyfish.backend3.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByActiveTrueOrderBySortOrderAscNameAsc();
    Optional<Category> findBySlug(String slug);
    Optional<Category> findByNameIgnoreCase(String name);
}
