package com.easyfish.backend3.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Data
public class StockHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne private Product product;
    private Double oldStock;
    private Double changeQuantity;
    private Double newStock;
    private String actionType;
    private String reason;
    private LocalDateTime createdAt;
    @PrePersist protected void onCreate(){ if(createdAt==null) createdAt=LocalDateTime.now(ZoneId.of("Asia/Kolkata")); }
}
