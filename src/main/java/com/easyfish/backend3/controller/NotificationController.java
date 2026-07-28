package com.easyfish.backend3.controller;

import com.easyfish.backend3.entity.DeliveryNotification;
import com.easyfish.backend3.repository.DeliveryNotificationRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {
    private final DeliveryNotificationRepository notificationRepo;

    public NotificationController(DeliveryNotificationRepository notificationRepo) {
        this.notificationRepo = notificationRepo;
    }

    @GetMapping("/user/{userId}")
    public List<DeliveryNotification> byUser(@PathVariable String userId) {
        return notificationRepo.findByUserPhoneOrderByIdDesc(userId);
    }

    @PutMapping("/{id}/read")
    public DeliveryNotification markRead(@PathVariable Long id) {
        DeliveryNotification notification = notificationRepo.findById(id).orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setReadFlag(true);
        return notificationRepo.save(notification);
    }

    @DeleteMapping("/user/{userId}/clear")
    public java.util.Map<String, Object> clearUserNotifications(@PathVariable String userId) {
        long deleted = notificationRepo.deleteByUserPhone(userId);
        return java.util.Map.of("success", true, "deleted", deleted);
    }
}
