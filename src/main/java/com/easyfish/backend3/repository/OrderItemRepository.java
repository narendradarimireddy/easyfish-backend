package com.easyfish.backend3.repository;

import com.easyfish.backend3.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Modifying
    @Query("update OrderItem oi set oi.product = null where oi.product.id = :productId")
    int detachProduct(@Param("productId") Long productId);

    @Modifying
    @Query("delete from OrderItem oi where oi.product.id = :productId")
    int deleteByProductId(@Param("productId") Long productId);
}
