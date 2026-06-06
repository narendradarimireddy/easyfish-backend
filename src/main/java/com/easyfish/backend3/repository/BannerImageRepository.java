package com.easyfish.backend3.repository;

import com.easyfish.backend3.entity.BannerImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BannerImageRepository extends JpaRepository<BannerImage, Long> {
    List<BannerImage> findByActiveTrueOrderBySortOrderAscIdAsc();
}