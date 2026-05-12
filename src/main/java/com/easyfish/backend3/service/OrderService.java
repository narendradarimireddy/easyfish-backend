package com.easyfish.backend3.service;

import com.easyfish.backend3.dto.CheckoutRequest;
import com.easyfish.backend3.dto.CheckoutResponse;
import com.easyfish.backend3.dto.DeliveryAddressDto;
import com.easyfish.backend3.entity.DeliveryAddress;
import com.easyfish.backend3.entity.DeliveryNotification;
import com.easyfish.backend3.entity.Order;
import com.easyfish.backend3.entity.OrderItem;
import com.easyfish.backend3.entity.Product;
import com.easyfish.backend3.entity.User;
import com.easyfish.backend3.repository.DeliveryAddressRepository;
import com.easyfish.backend3.repository.DeliveryNotificationRepository;
import com.easyfish.backend3.repository.OrderItemRepository;
import com.easyfish.backend3.repository.OrderRepository;
import com.easyfish.backend3.repository.ProductRepository;
import com.easyfish.backend3.repository.UserRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final OrderRepository orderRepo;
    private final OrderItemRepository itemRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final DeliveryAddressRepository addressRepo;
    private final DeliveryNotificationRepository notificationRepo;
    private final RazorpayService razorpayService;
    private final Fast2SmsService fast2SmsService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${razorpay.enabled:false}")
    private boolean razorpayEnabled;

    public OrderService(OrderRepository orderRepo, OrderItemRepository itemRepo, UserRepository userRepo, ProductRepository productRepo, DeliveryAddressRepository addressRepo, DeliveryNotificationRepository notificationRepo, RazorpayService razorpayService, Fast2SmsService fast2SmsService) {
        this.orderRepo = orderRepo;
        this.itemRepo = itemRepo;
        this.userRepo = userRepo;
        this.productRepo = productRepo;
        this.addressRepo = addressRepo;
        this.notificationRepo = notificationRepo;
        this.razorpayService = razorpayService;
        this.fast2SmsService = fast2SmsService;
    }

    public CheckoutResponse checkout(CheckoutRequest request) throws Exception {
        User user = userRepo.findById(request.getUserId()).orElseThrow(() -> new RuntimeException("User not found"));
        Product product = productRepo.findById(request.getProductId()).orElseThrow(() -> new RuntimeException("Product not found"));
        int qty = request.getQuantity() == null || request.getQuantity() < 1 ? 1 : Math.min(request.getQuantity(), 4);

        DeliveryAddress savedAddress = saveAddress(user, request.getAddress());
        Double unitPrice = product.getFinalPrice() == null ? product.getPrice() : product.getFinalPrice();

        double productSubtotal = unitPrice * qty;
        double deliveryCharge = qty * 1.0; // ₹1 delivery charge per product quantity
        double finalAmount = productSubtotal + deliveryCharge;

        Order order = new Order();
        order.setUser(user);
        order.setAddress(buildAddressLine(savedAddress));
        order.setLocationMapLink(savedAddress.getMapLink());
        order.setDeliveryLatitude(savedAddress.getLatitude());
        order.setDeliveryLongitude(savedAddress.getLongitude());
        order.setPhone(savedAddress.getPhoneNumber());
        order.setProductId(product.getId());
        order.setProductName(product.getName());
        order.setPrimaryProductName(product.getName());
        order.setStatus("ORDER_PLACED");
        order.setPaymentStatus("COD".equalsIgnoreCase(request.getPaymentMode()) ? "UNPAID" : "PAYMENT_PENDING");
        order.setProductSubtotal(productSubtotal);
        order.setDeliveryCharge(deliveryCharge);
        order.setTotalAmount(finalAmount);
        Order savedOrder = orderRepo.save(order);

        OrderItem item = new OrderItem();
        item.setOrder(savedOrder);
        item.setProduct(product);
        item.setQuantity(qty);
        item.setPrice(unitPrice);
        item.setPackQuantity(resolvePackQuantity(product, request));
        item.setPackUnit(resolvePackUnit(product, request));
        item.setTotalGrams(resolveTotalGrams(product, request, qty));
        itemRepo.save(item);
        createOrderNotification(savedOrder, "ORDER_PLACED");

        String razorpayOrderId = null;
        if ("ONLINE".equalsIgnoreCase(request.getPaymentMode()) && razorpayEnabled) {
            JSONObject orderJson = razorpayService.createOrder(savedOrder.getTotalAmount());
            razorpayOrderId = orderJson.optString("id", null);
            savedOrder.setRazorpayOrderId(razorpayOrderId);
            savedOrder = orderRepo.save(savedOrder);
        }

        return new CheckoutResponse(
                savedOrder.getId(),
                savedOrder.getStatus(),
                savedOrder.getPaymentStatus(),
                request.getPaymentMode(),
                razorpayOrderId,
                savedOrder.getProductSubtotal(),
                savedOrder.getDeliveryCharge(),
                savedOrder.getTotalAmount(),
                savedOrder.getLocationMapLink()
        );
    }

    public List<Order> getOrdersByUser(String userId) {
        return orderRepo.findByUserPhoneOrderByIdDesc(userId)
                .stream()
                .filter(this::shouldExposeOrder)
                .collect(Collectors.toList());
    }

    public List<Order> getAllOrders() {
        return orderRepo.findAllByOrderByIdDesc()
                .stream()
                .filter(this::shouldExposeOrder)
                .collect(Collectors.toList());
    }

    public Order updateStatus(Long orderId, String status) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        String current = order.getStatus() == null ? "ORDER_PLACED" : order.getStatus().trim().toUpperCase();
        String next = status == null ? "ORDER_PLACED" : status.trim().toUpperCase();
        int currentRank = statusRank(current);
        int nextRank = statusRank(next);
        if (currentRank >= nextRank) {
            return order;
        }
        order.setStatus(next);
        Order saved = orderRepo.save(order);
        createOrderNotification(saved, next);
        return saved;
    }

    private int statusRank(String status) {
        return switch (status) {
            case "PACKED" -> 1;
            case "SHIPPED" -> 2;
            case "OUT_FOR_DELIVERY" -> 3;
            case "DELIVERED" -> 4;
            default -> 0;
        };
    }

    public Order markPaymentSuccess(Long orderId, String transactionId, String gatewayOrderId) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        order.setPaymentStatus("PAID");
        if (transactionId != null && !transactionId.isBlank()) order.setTransactionId(transactionId);
        if (gatewayOrderId != null && !gatewayOrderId.isBlank()) order.setRazorpayOrderId(gatewayOrderId);
        return orderRepo.save(order);
    }


    public Order sendDeliveryOtp(Long orderId) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        String paymentStatus = order.getPaymentStatus() == null ? "" : order.getPaymentStatus().trim().toUpperCase();
        if (!paymentStatus.equals("PAID") && !paymentStatus.equals("SUCCESS") && !paymentStatus.equals("COMPLETED") && !paymentStatus.equals("CAPTURED")) {
            throw new RuntimeException("OTP is required only for paid orders. Use Delivered / Cash for unpaid orders.");
        }
        User user = order.getUser();
        if (user == null) throw new RuntimeException("Order user not found");
        String otp = String.format("%06d", secureRandom.nextInt(1000000));
        LocalDateTime now = LocalDateTime.now();
        order.setDeliveryOtp(otp);
        order.setDeliveryOtpGeneratedAt(now);
        Order saved = orderRepo.save(order);

        DeliveryNotification notification = new DeliveryNotification();
        notification.setUser(user);
        notification.setOrder(saved);
        notification.setTitle("Delivery OTP");
        notification.setMessage("Share this OTP with the delivery boy only after receiving your order. This OTP expires in 5 minutes.");
        notification.setType("DELIVERY_OTP");
        notification.setTargetUrl("/orders/" + saved.getId());
        notification.setActionLabel("Open tracking");
        notification.setOtp(otp);
        notification.setExpiresAt(now.plusMinutes(5));
        notification.setReadFlag(false);
        notificationRepo.save(notification);

        // Optional SMS fallback: if Fast2SMS is configured and customer phone is present, this also sends SMS.
        String phone = order.getPhone();
        if (phone != null && !phone.isBlank()) {
            try { fast2SmsService.sendDeliveryOtp(phone, otp); } catch (Exception ignored) { }
        }
        return saved;
    }

    public Order verifyDeliveryOtp(Long orderId, String otp) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        if (otp == null || otp.trim().isEmpty()) throw new RuntimeException("OTP is required");
        if (order.getDeliveryOtp() == null || !order.getDeliveryOtp().equals(otp.trim())) throw new RuntimeException("Invalid delivery OTP");
        if (order.getDeliveryOtpGeneratedAt() == null || order.getDeliveryOtpGeneratedAt().isBefore(LocalDateTime.now().minusMinutes(5))) {
            order.setDeliveryOtp(null);
            order.setDeliveryOtpGeneratedAt(null);
            orderRepo.save(order);
            throw new RuntimeException("Delivery OTP expired. Send OTP again.");
        }
        order.setStatus("DELIVERED");
        order.setPaymentStatus("PAID");
        order.setDeliveryOtp(null);
        order.setDeliveryOtpGeneratedAt(null);
        Order saved = orderRepo.save(order);
        createOrderNotification(saved, "DELIVERED");
        return saved;
    }

    public Order markDeliveredCash(Long orderId) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus("DELIVERED");
        order.setPaymentStatus("CASH");
        order.setDeliveryOtp(null);
        order.setDeliveryOtpGeneratedAt(null);
        Order saved = orderRepo.save(order);
        createOrderNotification(saved, "DELIVERED");
        return saved;
    }


    private void createOrderNotification(Order order, String status) {
        if (order == null || order.getUser() == null || order.getId() == null) return;
        String normalized = status == null ? "ORDER_PLACED" : status.trim().toUpperCase();
        DeliveryNotification notification = new DeliveryNotification();
        notification.setUser(order.getUser());
        notification.setOrder(order);
        notification.setType(normalized);
        notification.setTargetUrl("/orders/" + order.getId());
        notification.setActionLabel("Open tracking");
        notification.setReadFlag(false);

        String item = order.getProductName() == null || order.getProductName().isBlank() ? "your seafood item" : order.getProductName();
        switch (normalized) {
            case "PACKED" -> {
                notification.setTitle("Order packed");
                notification.setMessage("Order #" + order.getId() + " is packed and ready for dispatch: " + item + ".");
            }
            case "SHIPPED" -> {
                notification.setTitle("Order shipped");
                notification.setMessage("Order #" + order.getId() + " has been shipped. Tap to see tracking.");
            }
            case "OUT_FOR_DELIVERY" -> {
                notification.setTitle("Out for delivery");
                notification.setMessage("Order #" + order.getId() + " is out for delivery. Keep your delivery phone nearby.");
            }
            case "DELIVERED" -> {
                notification.setTitle("Order delivered");
                notification.setMessage("Order #" + order.getId() + " has been delivered successfully.");
            }
            default -> {
                notification.setTitle("Order placed");
                notification.setMessage("Order #" + order.getId() + " placed successfully. Tap to view your order and tracking.");
            }
        }
        notificationRepo.save(notification);
    }

    private boolean shouldExposeOrder(Order order) {
        String paymentStatus = order.getPaymentStatus() == null ? "" : order.getPaymentStatus().trim().toUpperCase();
        boolean hasGatewayOrder = order.getRazorpayOrderId() != null && !order.getRazorpayOrderId().isBlank();
        boolean hasTransaction = order.getTransactionId() != null && !order.getTransactionId().isBlank();
        if (hasGatewayOrder && !hasTransaction) {
            return paymentStatus.equals("PAID") || paymentStatus.equals("SUCCESS") || paymentStatus.equals("COMPLETED") || paymentStatus.equals("CAPTURED");
        }
        return true;
    }

    private String resolvePackQuantity(Product product, CheckoutRequest request) {
        if (request.getPackQuantity() != null && !request.getPackQuantity().isBlank()) return request.getPackQuantity().trim();
        if (product.getQuantity() != null && !product.getQuantity().isBlank()) return product.getQuantity().trim();
        return "500";
    }

    private String resolvePackUnit(Product product, CheckoutRequest request) {
        if (request.getPackUnit() != null && !request.getPackUnit().isBlank()) return request.getPackUnit().trim();
        if (product.getUnit() != null && !product.getUnit().isBlank()) return product.getUnit().trim();
        return "g";
    }

    private Integer resolveTotalGrams(Product product, CheckoutRequest request, int qty) {
        if (request.getTotalGrams() != null && request.getTotalGrams() > 0) return request.getTotalGrams();
        double packQty = parseQuantity(resolvePackQuantity(product, request), 500.0);
        String unit = resolvePackUnit(product, request).toLowerCase();
        if (unit.equals("kg") || unit.equals("kgs") || unit.equals("kilogram") || unit.equals("kilograms")) {
            return (int) Math.round(packQty * 1000 * qty);
        }
        if (unit.equals("g") || unit.equals("gram") || unit.equals("grams")) {
            return (int) Math.round(packQty * qty);
        }
        return null;
    }

    private double parseQuantity(String value, double fallback) {
        if (value == null) return fallback;
        String cleaned = value.replaceAll("[^0-9.]", "");
        if (cleaned.isBlank()) return fallback;
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private DeliveryAddress saveAddress(User user, DeliveryAddressDto dto) {
        if (dto == null) {
            throw new RuntimeException("Delivery address or current location is required");
        }

        boolean hasCurrentLocation = dto.getLatitude() != null && dto.getLongitude() != null;

        String country = safeText(dto.getCountry(), "India");
        String state = safeText(dto.getState(), "Andhra Pradesh");
        String city = safeText(dto.getCity(), "Visakhapatnam");
        String area = safeText(dto.getArea(), "");
        String street = safeText(dto.getStreet(), "");
        String landmark = safeText(dto.getLandmark(), "");
        String pincode = safeText(dto.getPincode(), "");
        String phoneNumber = normalizePhone(dto.getPhoneNumber());
        validateRequired(phoneNumber, "Mobile number");

        if (hasCurrentLocation) {
            double lat = dto.getLatitude();
            double lng = dto.getLongitude();

            if (!isVisakhapatnamLocation(lat, lng)) {
                throw new RuntimeException("Currently delivery available only in Visakhapatnam");
            }

            String mapLink = "https://maps.google.com/?q=" + lat + "," + lng;
            String addressText = dto.getAddressText();
            if (addressText == null || addressText.trim().isEmpty()) {
                addressText = "Current location: " + lat + ", " + lng;
            }

            DeliveryAddress address = new DeliveryAddress();
            address.setUser(user);
            address.setCountry("India");
            address.setState("Andhra Pradesh");
            address.setCity("Visakhapatnam");
            address.setArea(area.isBlank() ? "Current Location" : area);
            address.setStreet(street.isBlank() ? addressText : street);
            address.setLandmark(landmark.isBlank() ? "Pinned location" : landmark);
            address.setPincode(pincode);
            address.setPhoneNumber(phoneNumber);
            address.setLatitude(lat);
            address.setLongitude(lng);
            address.setAddressText(addressText);
            address.setMapLink(mapLink);
            return addressRepo.save(address);
        }

        validateRequired(country, "Country");
        validateRequired(state, "State");
        validateRequired(city, "City");
        validateRequired(area, "Area");
        validateRequired(street, "Street");
        validateRequired(landmark, "Landmark");
        validateRequired(pincode, "Pincode");

        if (!city.trim().equalsIgnoreCase("Visakhapatnam") && !city.trim().equalsIgnoreCase("Vizag")) {
            throw new RuntimeException("Currently delivery available only in Visakhapatnam");
        }

        if (!state.trim().equalsIgnoreCase("Andhra Pradesh")) {
            throw new RuntimeException("Currently delivery available only in Andhra Pradesh, Visakhapatnam");
        }

        if (!pincode.trim().matches("\\d{6}")) {
            throw new RuntimeException("Pincode must be 6 digits");
        }

        String addressText = street + ", " + area + ", Landmark: " + landmark + ", " + city + ", " + state + " - " + pincode;

        DeliveryAddress address = new DeliveryAddress();
        address.setUser(user);
        address.setCountry(country.trim());
        address.setState(state.trim());
        address.setCity("Visakhapatnam");
        address.setArea(area.trim());
        address.setStreet(street.trim());
        address.setLandmark(landmark.trim());
        address.setPincode(pincode.trim());
        address.setPhoneNumber(phoneNumber);
        address.setLatitude(null);
        address.setLongitude(null);
        address.setAddressText(addressText);
        address.setMapLink("");
        return addressRepo.save(address);
    }

    private boolean isVisakhapatnamLocation(double lat, double lng) {
        // Approx Visakhapatnam district/city delivery boundary.
        // Tight enough to block other districts, loose enough for suburbs around Vizag.
        return lat >= 17.45 && lat <= 18.25 && lng >= 82.75 && lng <= 83.75;
    }

    private String buildAddressLine(DeliveryAddress savedAddress) {
        String base = savedAddress.getAddressText() == null || savedAddress.getAddressText().isBlank()
                ? "Current location"
                : savedAddress.getAddressText();
        return base + " | Visakhapatnam | Map: " + savedAddress.getMapLink();
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String normalizePhone(String value) {
        String phone = value == null ? "" : value.replaceAll("\\D", "");
        if (phone.startsWith("91") && phone.length() == 12) phone = phone.substring(2);
        if (!phone.matches("[6-9]\\d{9}")) {
            throw new RuntimeException("Enter valid 10 digit delivery mobile number");
        }
        return phone;
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException(fieldName + " is required");
        }
    }
}
