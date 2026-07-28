package com.easyfish.backend3.repository;

import com.easyfish.backend3.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserIdOrderByUpdatedAtDesc(String userId);
    @Transactional
    void deleteByUserId(String userId);
    @Transactional
    @Modifying
    @Query("delete from CartItem ci where ci.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);
}
