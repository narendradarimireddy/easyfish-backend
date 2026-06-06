package com.easyfish.backend3.controller;

import com.easyfish.backend3.entity.DeliveryAddress;
import com.easyfish.backend3.entity.User;
import com.easyfish.backend3.repository.DeliveryAddressRepository;
import com.easyfish.backend3.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@CrossOrigin(origins = "*")
public class DeliveryAddressController {
    private final DeliveryAddressRepository addressRepo;
    private final UserRepository userRepo;

    public DeliveryAddressController(DeliveryAddressRepository addressRepo, UserRepository userRepo) {
        this.addressRepo = addressRepo;
        this.userRepo = userRepo;
    }

    @GetMapping("/user/{userId}")
    public List<DeliveryAddress> byUser(@PathVariable String userId) {
        return addressRepo.findByUserPhoneOrderByIdDesc(userId).stream().limit(5).toList();
    }

    @PostMapping("/user/{userId}")
    public DeliveryAddress save(@PathVariable String userId, @RequestBody DeliveryAddress address) {
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        address.setId(null);
        address.setUser(user);
        normalize(address);
        List<DeliveryAddress> existing = addressRepo.findByUserPhoneOrderByIdDesc(userId);
        if (existing.size() >= 5) {
            throw new RuntimeException("Maximum 5 saved addresses allowed. Please delete an address before adding a new one.");
        }
        DeliveryAddress saved = addressRepo.save(address);
        trimToFive(userId);
        return saved;
    }

    @PutMapping("/{id}")
    public DeliveryAddress update(@PathVariable Long id, @RequestBody DeliveryAddress next) {
        DeliveryAddress existing = addressRepo.findById(id).orElseThrow(() -> new RuntimeException("Address not found"));
        existing.setCountry(next.getCountry());
        existing.setState(next.getState());
        existing.setCity(next.getCity());
        existing.setArea(next.getArea());
        existing.setStreet(next.getStreet());
        existing.setLandmark(next.getLandmark());
        existing.setPincode(next.getPincode());
        existing.setPhoneNumber(next.getPhoneNumber());
        existing.setLatitude(next.getLatitude());
        existing.setLongitude(next.getLongitude());
        existing.setAddressText(next.getAddressText());
        existing.setMapLink(next.getMapLink());
        normalize(existing);
        return addressRepo.save(existing);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return hardDeleteAddress(id);
    }

    @PostMapping("/{id}/delete")
    @Transactional
    public ResponseEntity<?> deleteFallback(@PathVariable Long id) {
        return hardDeleteAddress(id);
    }

    private ResponseEntity<?> hardDeleteAddress(Long id) {
        int deleted = addressRepo.hardDeleteById(id);
        addressRepo.flush();
        if (deleted == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(java.util.Map.of("deleted", true, "id", id));
    }

    private void trimToFive(String userId) {
        List<DeliveryAddress> all = addressRepo.findByUserPhoneOrderByIdDesc(userId);
        if (all.size() <= 5) return;
        all.stream().skip(5).forEach(addressRepo::delete);
    }

    private void normalize(DeliveryAddress address) {
        if (address.getCountry() == null || address.getCountry().isBlank()) address.setCountry("India");
        if (address.getState() == null || address.getState().isBlank()) address.setState("Andhra Pradesh");
        if (address.getCity() == null || address.getCity().isBlank()) address.setCity("Visakhapatnam");
        if (address.getAddressText() == null || address.getAddressText().isBlank()) {
            String text = String.join(", ",
                    safe(address.getStreet()),
                    safe(address.getArea()),
                    safe(address.getLandmark()),
                    safe(address.getCity()),
                    safe(address.getState()),
                    safe(address.getPincode())
            ).replaceAll("(,\\s*)+", ", ").replaceAll("^, |, $", "");
            address.setAddressText(text);
        }
        if (address.getLatitude() != null && address.getLongitude() != null) {
            address.setMapLink("https://maps.google.com/?q=" + address.getLatitude() + "," + address.getLongitude());
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
