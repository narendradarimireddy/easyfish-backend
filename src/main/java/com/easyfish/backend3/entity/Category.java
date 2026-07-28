package com.easyfish.backend3.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "category")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(length = 160)
    private String slug;

    @Column(length = 500)
    private String subtitle;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    private Boolean active = true;

    private Integer sortOrder = 0;
}
