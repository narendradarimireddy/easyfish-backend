package com.easyfish.backend3.repository;

import com.easyfish.backend3.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByNameContainingIgnoreCaseOrLocalNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(
            String name, String localName, String category);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where lower(trim(coalesce(p.name, ''))) = lower(trim(:name)) and lower(trim(coalesce(p.localName, ''))) = lower(trim(:localName))")
    List<Product> findSameStockProductsForUpdate(@Param("name") String name, @Param("localName") String localName);
}
