package com.easyfish.backend3.repository;

import com.easyfish.backend3.entity.StockHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {
    List<StockHistory> findByCreatedAtBetweenOrderByIdDesc(LocalDateTime from, LocalDateTime to);
    List<StockHistory> findAllByOrderByIdDesc();
    void deleteByProductId(Long productId);
}
