package com.easyfish.backend3.service;

import com.easyfish.backend3.dto.CheckoutRequest;
import com.easyfish.backend3.dto.CheckoutResponse;
import com.easyfish.backend3.dto.CheckoutItemRequest;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.List;
import java.util.ArrayList;
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
    private final StockService stockService;
    private final AppSettingService appSettingService;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${razorpay.enabled:false}")
    private boolean razorpayEnabled;

    @Value("${easyfish.admin-order-email.enabled:true}")
    private boolean adminOrderEmailEnabled;

    @Value("${easyfish.admin-order-email.to:}")
    private String adminOrderEmailTo;

    public OrderService(OrderRepository orderRepo, OrderItemRepository itemRepo, UserRepository userRepo, ProductRepository productRepo, DeliveryAddressRepository addressRepo, DeliveryNotificationRepository notificationRepo, RazorpayService razorpayService, Fast2SmsService fast2SmsService, StockService stockService, AppSettingService appSettingService, EmailService emailService) {
        this.orderRepo = orderRepo;
        this.itemRepo = itemRepo;
        this.userRepo = userRepo;
        this.productRepo = productRepo;
        this.addressRepo = addressRepo;
        this.notificationRepo = notificationRepo;
        this.razorpayService = razorpayService;
        this.fast2SmsService = fast2SmsService;
        this.stockService = stockService;
        this.appSettingService = appSettingService;
        this.emailService = emailService;
    }

    @Transactional
    public CheckoutResponse checkout(CheckoutRequest request) throws Exception {
        User user = userRepo.findById(request.getUserId()).orElseThrow(() -> new RuntimeException("User not found"));
        List<CheckoutItemRequest> lines = request.getItems();
        if (lines == null || lines.isEmpty()) {
            CheckoutItemRequest single = new CheckoutItemRequest();
            single.setProductId(request.getProductId());
            single.setQuantity(request.getQuantity());
            single.setPackQuantity(request.getPackQuantity());
            single.setPackUnit(request.getPackUnit());
            single.setTotalGrams(request.getTotalGrams());
            lines = List.of(single);
        }
        if (lines.isEmpty()) throw new RuntimeException("No products selected");

        DeliveryAddress savedAddress = saveAddress(user, request.getAddress());
        double productSubtotal = 0.0;
        String primaryName = null;
        Product firstProduct = null;
        List<OrderItem> createdItems = new ArrayList<>();

        Order order = new Order();
        order.setUser(user);
        order.setAddress(buildAddressLine(savedAddress));
        order.setLocationMapLink(savedAddress.getMapLink());
        order.setDeliveryLatitude(savedAddress.getLatitude());
        order.setDeliveryLongitude(savedAddress.getLongitude());
        order.setPhone(savedAddress.getPhoneNumber());
        order.setStatus("ORDER_PLACED");
        order.setPaymentStatus("COD".equalsIgnoreCase(request.getPaymentMode()) ? "UNPAID" : "PAYMENT_PENDING");
        order.setStockDeducted(false);
        Order savedOrder = orderRepo.save(order);
        if (savedOrder.getOrderNumber() == null && savedOrder.getId() != null) {
            savedOrder.setOrderNumber(100000L + savedOrder.getId());
            savedOrder = orderRepo.save(savedOrder);
        }

        for (CheckoutItemRequest line : lines) {
            Product product = productRepo.findById(line.getProductId()).orElseThrow(() -> new RuntimeException("Product not found"));
            int qty = line.getQuantity() == null || line.getQuantity() < 1 ? 1 : Math.min(line.getQuantity(), 4);
            Double unitPrice = product.getFinalPrice() == null ? product.getPrice() : product.getFinalPrice();
            productSubtotal += (unitPrice == null ? 0 : unitPrice) * qty;
            if (firstProduct == null) firstProduct = product;
            if (primaryName == null) primaryName = product.getName(); else primaryName += ", " + product.getName();

            OrderItem item = new OrderItem();
            item.setOrder(savedOrder);
            item.setProduct(product);
            item.setQuantity(qty);
            item.setPrice(unitPrice);
            item.setPackQuantity(resolvePackQuantity(product, line));
            item.setPackUnit(resolvePackUnit(product, line));
            item.setTotalGrams(resolveTotalGrams(product, line, qty));
            itemRepo.save(item);
            createdItems.add(item);
        }

        double deliveryCharge = 0.0;
        savedOrder.setProductSubtotal(productSubtotal);
        savedOrder.setDeliveryCharge(0.0);
        savedOrder.setTotalAmount(productSubtotal);
        if (firstProduct != null) savedOrder.setProductId(firstProduct.getId());
        savedOrder.setProductName(primaryName);
        savedOrder.setPrimaryProductName(primaryName);
        savedOrder.setStockDeducted(false);
        savedOrder = orderRepo.save(savedOrder);
        if ("COD".equalsIgnoreCase(request.getPaymentMode())) {
            deductOrderStock(createdItems);
            savedOrder.setStockDeducted(true);
            savedOrder = orderRepo.save(savedOrder);
            savedOrder.setItems(createdItems);
            sendAdminOrderPlacedEmail(savedOrder, createdItems);
        }
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

    @Transactional
    public Order updateStatus(Long orderId, String status) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        String current = order.getStatus() == null ? "ORDER_PLACED" : order.getStatus().trim().toUpperCase();
        if (current.contains("CANCEL")) {
            throw new RuntimeException("Cancelled order cannot be updated");
        }
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

    @Transactional
    public Order markPaymentSuccess(Long orderId, String transactionId, String gatewayOrderId) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        boolean wasPaid = isPaidOrder(order);
        order.setPaymentStatus("PAID");
        if (transactionId != null && !transactionId.isBlank()) { order.setTransactionId(transactionId); order.setRazorpayPaymentId(transactionId); }
        if (gatewayOrderId != null && !gatewayOrderId.isBlank()) order.setRazorpayOrderId(gatewayOrderId);
        if (!Boolean.TRUE.equals(order.getStockDeducted())) {
            deductOrderStock(order);
            order.setStockDeducted(true);
        }
        Order saved = orderRepo.save(order);
        if (!wasPaid) {
            sendAdminOrderPlacedEmail(saved, saved.getItems());
        }
        return saved;
    }


    public Order sendDeliveryOtp(Long orderId) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        if ((order.getStatus() == null ? "" : order.getStatus().trim().toUpperCase()).contains("CANCEL")) {
            throw new RuntimeException("Cancelled order cannot be delivered");
        }
        String paymentStatus = order.getPaymentStatus() == null ? "" : order.getPaymentStatus().trim().toUpperCase();
        if (!paymentStatus.equals("PAID") && !paymentStatus.equals("SUCCESS") && !paymentStatus.equals("COMPLETED") && !paymentStatus.equals("CAPTURED")) {
            throw new RuntimeException("OTP is required only for paid orders. Use Delivered / Cash for unpaid orders.");
        }
        User user = order.getUser();
        if (user == null) throw new RuntimeException("Order user not found");
        String otp = String.format("%06d", secureRandom.nextInt(1000000));
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
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
        if ((order.getStatus() == null ? "" : order.getStatus().trim().toUpperCase()).contains("CANCEL")) {
            throw new RuntimeException("Cancelled order cannot be delivered");
        }
        if (otp == null || otp.trim().isEmpty()) throw new RuntimeException("OTP is required");
        if (order.getDeliveryOtp() == null || !order.getDeliveryOtp().equals(otp.trim())) throw new RuntimeException("Invalid delivery OTP");
        if (order.getDeliveryOtpGeneratedAt() == null || order.getDeliveryOtpGeneratedAt().isBefore(LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata")).minusMinutes(5))) {
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
        if ((order.getStatus() == null ? "" : order.getStatus().trim().toUpperCase()).contains("CANCEL")) {
            throw new RuntimeException("Cancelled order cannot be delivered");
        }
        order.setStatus("DELIVERED");
        order.setPaymentStatus("CASH");
        order.setDeliveryOtp(null);
        order.setDeliveryOtpGeneratedAt(null);
        Order saved = orderRepo.save(order);
        createOrderNotification(saved, "DELIVERED");
        return saved;
    }

    @Transactional
    public Order cancelOrder(Long orderId, String reason) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        String status = order.getStatus() == null ? "" : order.getStatus().trim().toUpperCase();
        if (status.equals("DELIVERED") || status.equals("PACKED") || status.equals("SHIPPED") || status.equals("OUT_FOR_DELIVERY")) {
            throw new RuntimeException("Order cannot be cancelled after packing has started");
        }
        if (status.contains("CANCEL")) {
            return order;
        }
        order.setCancelReason(reason == null || reason.isBlank() ? "Cancelled" : reason);
        boolean paid = isPaidOrder(order);
        if (paid && order.getRazorpayPaymentId() != null && !order.getRazorpayPaymentId().isBlank()) {
            try {
                org.json.JSONObject refund = razorpayService.refundPayment(order.getRazorpayPaymentId(), order.getTotalAmount());
                order.setRefundId(refund.optString("id", ""));
                order.setRefundStatus("REFUND_INITIATED");
                order.setRefundAmount(order.getTotalAmount());
                order.setPaymentStatus("REFUND_INITIATED");
            } catch (Exception ex) {
                order.setRefundStatus("REFUND_FAILED");
                order.setPaymentStatus("REFUND_FAILED");
            }
        } else {
            order.setRefundStatus("NOT_REQUIRED");
        }
        if (Boolean.TRUE.equals(order.getStockDeducted())) {
            restoreOrderStock(order);
            order.setStockDeducted(false);
        }
        order.setStatus("CANCELLED");
        Order saved = orderRepo.save(order);
        createOrderNotification(saved, paid ? "CANCELLED_REFUND" : "CANCELLED");
        return saved;
    }

    private boolean isPaidOrder(Order order) {
        String paymentStatus = order.getPaymentStatus() == null ? "" : order.getPaymentStatus().trim().toUpperCase();
        return paymentStatus.equals("PAID") || paymentStatus.equals("SUCCESS") || paymentStatus.equals("COMPLETED") || paymentStatus.equals("CAPTURED") ||
                (order.getRazorpayPaymentId() != null && !order.getRazorpayPaymentId().isBlank()) ||
                (order.getTransactionId() != null && !order.getTransactionId().isBlank());
    }


    private void sendAdminOrderPlacedEmail(Order order, List<OrderItem> items) {
        if (!adminOrderEmailEnabled || order == null || adminOrderEmailTo == null || adminOrderEmailTo.isBlank()) return;
        try {
            User user = order.getUser();
            String customerName = user == null || user.getName() == null || user.getName().isBlank() ? "Customer" : user.getName();
            String phone = order.getPhone();
            if ((phone == null || phone.isBlank()) && user != null) phone = user.getPhone();
            StringBuilder body = new StringBuilder();
            body.append("Easyfish\n");
            body.append("New order placed\n\n");
            body.append("Order ID: ").append(order.getOrderNumber() != null ? order.getOrderNumber() : order.getId()).append("\n");
            body.append("Customer name: ").append(customerName).append("\n");
            body.append("Phone number: ").append(phone == null || phone.isBlank() ? "Not provided" : phone).append("\n");
            if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
                body.append("Email: ").append(user.getEmail()).append("\n");
            }
            body.append("Payment: ").append(order.getPaymentStatus() == null ? "" : order.getPaymentStatus()).append("\n");
            body.append("Total amount: ₹").append(order.getTotalAmount() == null ? 0 : order.getTotalAmount()).append("\n");
            body.append("Address: ").append(order.getAddress() == null ? "" : order.getAddress()).append("\n\n");
            body.append("Items:\n");
            List<OrderItem> sourceItems = items != null ? items : order.getItems();
            if (sourceItems == null || sourceItems.isEmpty()) {
                body.append("- ").append(order.getProductName() == null ? "Order items" : order.getProductName()).append("\n");
            } else {
                for (OrderItem item : sourceItems) {
                    Product product = item.getProduct();
                    String name = product == null ? "Product" : product.getName();
                    String local = product == null || product.getLocalName() == null || product.getLocalName().isBlank() ? "" : " (" + product.getLocalName() + ")";
                    String packQty = item.getPackQuantity() == null ? "" : item.getPackQuantity().trim();
                    String packUnit = item.getPackUnit() == null ? "" : item.getPackUnit().trim();
                    String pack = packQty.isBlank() ? "" : packQty + (packUnit.isBlank() ? "" : " " + packUnit);
                    String grams = item.getTotalGrams() == null ? "" : " | Total grams: " + item.getTotalGrams() + "g";
                    body.append("- ").append(name).append(local)
                            .append(" | Qty: ").append(item.getQuantity())
                            .append(pack.isBlank() ? "" : " | Pack: " + pack)
                            .append(grams)
                            .append("\n");
                }
            }
            emailService.sendPlainText(adminOrderEmailTo, "Easyfish - New Order Placed", body.toString());
        } catch (Exception ex) {
            System.out.println("Admin order email failed: " + ex.getMessage());
        }
    }


    private void createOrderNotification(Order order, String status) {
        if (order == null || order.getUser() == null || order.getId() == null) return;
        String normalized = status == null ? "ORDER_PLACED" : status.trim().toUpperCase();
        DeliveryNotification notification = new DeliveryNotification();
        notification.setUser(order.getUser());
        notification.setOrder(order);
        notification.setType(normalized);
        notification.setTargetUrl("/orders/" + order.getId());
        notification.setActionLabel(normalized.contains("CANCEL") ? "View order" : "Open tracking");
        notification.setReadFlag(false);

        String item = order.getPrimaryProductName() == null || order.getPrimaryProductName().isBlank() ? (order.getProductName() == null || order.getProductName().isBlank() ? "your seafood item" : order.getProductName()) : order.getPrimaryProductName();
        switch (normalized) {
            case "PACKED" -> {
                notification.setTitle("Order packed");
                notification.setMessage(item + " is packed and ready for dispatch.");
            }
            case "SHIPPED" -> {
                notification.setTitle("Order shipped");
                notification.setMessage(item + " has been shipped. Tap to see tracking.");
            }
            case "OUT_FOR_DELIVERY" -> {
                notification.setTitle("Out for delivery");
                notification.setMessage(item + " is out for delivery. Keep your delivery phone nearby.");
            }
            case "DELIVERED" -> {
                notification.setTitle("Order delivered");
                notification.setMessage(item + " has been delivered successfully.");
            }
            case "CANCELLED_REFUND", "REFUND_INITIATED" -> {
                notification.setTitle("Refund Initiated");
                notification.setMessage("Refund initiated successfully. Amount is being processed and may take 2–5 working days depending on your bank or UPI provider.");
            }
            case "CANCELLED" -> {
                notification.setTitle("Order cancelled");
                notification.setMessage(item + " cancelled successfully. No refund is required for cash or unpaid orders.");
            }
            default -> {
                notification.setTitle("Order placed");
                notification.setMessage(item + " placed successfully. Tap to view tracking.");
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

    private String resolvePackQuantity(Product product, CheckoutItemRequest request) {
        if (request.getPackQuantity() != null && !request.getPackQuantity().isBlank()) return request.getPackQuantity().trim();
        if (product.getQuantity() != null && !product.getQuantity().isBlank()) return product.getQuantity().trim();
        return "500";
    }

    private String resolvePackUnit(Product product, CheckoutItemRequest request) {
        if (request.getPackUnit() != null && !request.getPackUnit().isBlank()) return request.getPackUnit().trim();
        if (product.getUnit() != null && !product.getUnit().isBlank()) return product.getUnit().trim();
        return "g";
    }

    private Integer resolveTotalGrams(Product product, CheckoutItemRequest request, int qty) {
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




    private void deductOrderStock(List<OrderItem> items) {
        if (items == null) return;
        for (OrderItem item : items) {
            if (item.getProduct() != null) {
                stockService.deductStock(item.getProduct(), gramsToKg(item.getTotalGrams()));
            }
        }
    }

    private void deductOrderStock(Order order) {
        if (order == null || order.getItems() == null) return;
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null) {
                stockService.deductStock(item.getProduct(), gramsToKg(item.getTotalGrams()));
            }
        }
    }

    private void restoreOrderStock(Order order) {
        if (order == null || order.getItems() == null) return;
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null) {
                stockService.restoreStock(item.getProduct(), gramsToKg(item.getTotalGrams()), "Order cancelled");
            }
        }
    }

    private double gramsToKg(Integer grams) {
        if (grams == null || grams <= 0) return 0.0;
        return grams / 1000.0;
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

        if (dto.getId() != null) {
            DeliveryAddress existing = addressRepo.findById(dto.getId()).orElse(null);
            if (existing != null && existing.getUser() != null && user.getPhone().equals(existing.getUser().getPhone())) {
                return existing;
            }
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
            return saveAddressAndLimit(user, address);
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
        return saveAddressAndLimit(user, address);
    }

    private DeliveryAddress saveAddressAndLimit(User user, DeliveryAddress address) {
        DeliveryAddress saved = addressRepo.save(address);
        List<DeliveryAddress> all = addressRepo.findByUserPhoneOrderByIdDesc(user.getPhone());
        if (all.size() > 5) {
            all.stream().skip(5).forEach(addressRepo::delete);
        }
        return saved;
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
