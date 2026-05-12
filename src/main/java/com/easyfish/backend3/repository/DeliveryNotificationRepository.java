package com.easyfish.backend3.repository;

import com.easyfish.backend3.entity.DeliveryNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public interface DeliveryNotificationRepository extends JpaRepository<DeliveryNotification, Long> {
    List<DeliveryNotification> findByUserPhoneOrderByIdDesc(String userPhone);

    @Transactional
    long deleteByUserPhone(String userPhone);
}
