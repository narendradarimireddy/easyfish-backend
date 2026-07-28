package com.easyfish.backend3.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class AppSetting {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String settingKey;
    private String settingValue;
}
