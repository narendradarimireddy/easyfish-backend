package com.easyfish.backend3.repository;

import com.easyfish.backend3.entity.DeliveryAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, Long> {
    List<DeliveryAddress> findByUserPhoneOrderByIdDesc(String phone);
}
