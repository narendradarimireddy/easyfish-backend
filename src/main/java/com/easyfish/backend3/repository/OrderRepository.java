package com.easyfish.backend3.repository;

import com.easyfish.backend3.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserPhoneOrderByIdDesc(String phone);
    List<Order> findAllByOrderByIdDesc();
}
