package com.easyfish.backend3.repository;

import com.easyfish.backend3.entity.DeliveryAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, Long> {
    List<DeliveryAddress> findByUserPhoneOrderByIdDesc(String phone);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from DeliveryAddress a where a.id = :id")
    int hardDeleteById(@Param("id") Long id);
}

