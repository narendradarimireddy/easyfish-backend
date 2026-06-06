package com.easyfish.backend3.controller;

import com.easyfish.backend3.entity.Order;
import com.easyfish.backend3.entity.OrderItem;
import com.easyfish.backend3.entity.Product;
import com.easyfish.backend3.repository.OrderRepository;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;

@RestController
@RequestMapping("/api/revenue")
@CrossOrigin(origins="*")
public class RevenueController {
    private final OrderRepository repo;
    public RevenueController(OrderRepository repo){ this.repo=repo; }

    @GetMapping("/summary")
    public Map<String,Object> summary(@RequestParam(required=false) String from, @RequestParam(required=false) String to, @RequestParam(required=false) Integer year){
        List<Order> orders = repo.findAll();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        LocalDate fromDate = parseDate(from);
        LocalDate toDate = parseDate(to);
        int selectedYear = year == null ? today.getYear() : year;
        YearMonth thisMonth = YearMonth.from(today);

        double grossRevenue=0, productRevenue=0, todayTotal=0, monthlyTotal=0, yearlyTotal=0, cod=0, online=0, refund=0;
        Map<String, Double> categoryTotals = new LinkedHashMap<>();
        Map<String, Double> productTotals = new LinkedHashMap<>();
        Map<Integer, Double> yearTotals = new TreeMap<>();
        Map<Integer, Double> monthTotals = new TreeMap<>();
        Map<LocalDate, Double> dayTotals = new TreeMap<>();
        long completed=0, pending=0, cancelled=0, validRevenueOrders=0, refundOrders=0;
        List<Map<String,Object>> rows = new ArrayList<>();

        for(Order o: orders){
            LocalDate od = o.getCreatedAt()==null ? null : o.getCreatedAt().toLocalDate();
            String ps = clean(o.getPaymentStatus());
            String st = clean(o.getStatus());
            boolean isDelivered = "DELIVERED".equals(st);
            boolean isCancelled = st.contains("CANCEL");
            boolean isPaidOrCash = isPaidOrCash(ps);
            boolean isValidRevenue = isDelivered && isPaidOrCash;
            boolean isRefund = isRefund(ps) || isRefund(clean(o.getRefundStatus())) || (o.getRefundAmount()!=null && o.getRefundAmount()>0);
            double amt = money(o.getTotalAmount());
            double productsOnly = amt;

            if(isDelivered) completed++; else if(isCancelled) cancelled++; else pending++;
            if(isRefund){
                refundOrders++;
                double refundValue = money(o.getRefundAmount());
                refund += refundValue > 0 ? refundValue : amt;
            }

            if(isValidRevenue && od != null){
                yearTotals.put(od.getYear(), round(yearTotals.getOrDefault(od.getYear(), 0.0) + productsOnly));
                if(od.getYear() == selectedYear) monthTotals.put(od.getMonthValue(), round(monthTotals.getOrDefault(od.getMonthValue(), 0.0) + productsOnly));
                if(!od.isBefore(today.minusDays(4)) && !od.isAfter(today)) dayTotals.put(od, round(dayTotals.getOrDefault(od, 0.0) + productsOnly));
            }

            if(fromDate!=null && (od==null || od.isBefore(fromDate))) continue;
            if(toDate!=null && (od==null || od.isAfter(toDate))) continue;

            if(isValidRevenue){
                validRevenueOrders++;
                grossRevenue += productsOnly;
                productRevenue += productsOnly;
                if("CASH".equals(ps)) cod += productsOnly; else online += productsOnly;
                if(od!=null && od.equals(today)) todayTotal += productsOnly;
                if(od!=null && YearMonth.from(od).equals(thisMonth)) monthlyTotal += productsOnly;
                if(od!=null && od.getYear() == selectedYear) yearlyTotal += productsOnly;

                List<OrderItem> items = o.getItems();
                if(items != null && !items.isEmpty()){
                    double itemTotalSum = items.stream().mapToDouble(item -> money(item.getPrice()) * Math.max(1, item.getQuantity())).sum();
                    double fallbackSplit = productsOnly / Math.max(1, items.size());
                    for(OrderItem item : items){
                        Product p = item.getProduct();
                        String category = p == null || blank(p.getCategory()) ? "" : p.getCategory().trim();
                        String product = p == null || blank(p.getName()) ? (o.getPrimaryProductName()==null?"Product":o.getPrimaryProductName()) : p.getName().trim();
                        double rawItemValue = money(item.getPrice()) * Math.max(1, item.getQuantity());
                        double itemValue = productsOnly > 0 ? (itemTotalSum > 0 ? productsOnly * rawItemValue / itemTotalSum : fallbackSplit) : rawItemValue;
                        if(!blank(category)) categoryTotals.put(category, round(categoryTotals.getOrDefault(category, 0.0) + itemValue));
                        productTotals.put(product, round(productTotals.getOrDefault(product, 0.0) + itemValue));
                    }
                } else {
                    String product = o.getPrimaryProductName()==null ? "Order items" : o.getPrimaryProductName();
                    productTotals.put(product, round(productTotals.getOrDefault(product, 0.0) + productsOnly));
                    // Uncategorized category intentionally hidden from category revenue chart.
                }

                Map<String,Object> row = new LinkedHashMap<>();
                row.put("date", od==null?"-":od.toString());
                row.put("orderId", o.getOrderNumber()!=null ? o.getOrderNumber() : o.getId());
                row.put("product", o.getPrimaryProductName());
                row.put("payment", ps);
                row.put("status", st);
                row.put("amount", round(productsOnly));
                row.put("validRevenue", true);
                rows.add(row);
            }
        }

        Map<String,Object> m=new LinkedHashMap<>();
        m.put("todayRevenue", round(todayTotal));
        m.put("monthlyRevenue", round(monthlyTotal));
        m.put("yearlyRevenue", round(yearlyTotal));
        m.put("totalRevenue", round(grossRevenue));
        m.put("grossRevenue", round(grossRevenue));
        m.put("productRevenue", round(productRevenue));
        m.put("codRevenue", round(cod));
        m.put("onlineRevenue", round(online));
        m.put("refundAmount", round(refund));
        m.put("netRevenue", round(grossRevenue));
        m.put("completedOrders", completed);
        m.put("pendingOrders", pending);
        m.put("cancelledOrders", cancelled);
        m.put("validRevenueOrders", validRevenueOrders);
        m.put("refundOrders", refundOrders);
        m.put("totalOrders", completed+pending+cancelled);
        m.put("from", fromDate==null?"":fromDate.toString());
        m.put("to", toDate==null?"":toDate.toString());
        m.put("categoryRevenue", toRevenueRows(categoryTotals, "category"));
        m.put("productRevenueRows", toRevenueRows(productTotals, "product"));
        m.put("rows", rows);
        m.put("yearComparison", yearComparison(yearTotals, selectedYear));
        m.put("monthComparison", monthComparison(monthTotals));
        m.put("lastFiveDays", lastFiveDays(dayTotals, today));
        return m;
    }

    private List<Map<String,Object>> yearComparison(Map<Integer, Double> totals, int selectedYear){
        List<Map<String,Object>> rows = new ArrayList<>();
        for(int y = selectedYear - 2; y <= selectedYear; y++) rows.add(seriesRow(String.valueOf(y), totals.getOrDefault(y, 0.0)));
        return rows;
    }
    private List<Map<String,Object>> monthComparison(Map<Integer, Double> totals){
        List<Map<String,Object>> rows = new ArrayList<>();
        for(int m=1; m<=12; m++) rows.add(seriesRow(Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH), totals.getOrDefault(m, 0.0)));
        return rows;
    }
    private List<Map<String,Object>> lastFiveDays(Map<LocalDate, Double> totals, LocalDate today){
        List<Map<String,Object>> rows = new ArrayList<>();
        for(int i=4; i>=0; i--){
            LocalDate d = today.minusDays(i);
            String label = d.getDayOfMonth() + " " + d.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            rows.add(seriesRow(label, totals.getOrDefault(d, 0.0)));
        }
        return rows;
    }
    private Map<String,Object> seriesRow(String label, Double value){
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("revenue", round(value == null ? 0 : value));
        return m;
    }

    private List<Map<String,Object>> toRevenueRows(Map<String, Double> totals, String keyName){
        List<Map<String,Object>> result = new ArrayList<>();
        totals.entrySet().stream()
                .filter(e -> e.getKey()!=null && !e.getKey().isBlank() && !"other".equalsIgnoreCase(e.getKey().trim()) && !"uncategorized".equalsIgnoreCase(e.getKey().trim()))
                .sorted((a,b)->Double.compare(b.getValue(), a.getValue()))
                .forEach(entry -> {
                    Map<String,Object> item = new LinkedHashMap<>();
                    item.put(keyName, entry.getKey());
                    item.put("revenue", round(entry.getValue()));
                    result.add(item);
                });
        return result;
    }

    private boolean blank(String value){ return value == null || value.isBlank(); }
    private String clean(String value){ return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
    private boolean isPaidOrCash(String paymentStatus){ return Set.of("PAID", "CASH", "SUCCESS", "COMPLETED", "CAPTURED").contains(paymentStatus); }
    private boolean isRefund(String value){ return value.contains("REFUND"); }
    private double money(Double value){ return value == null ? 0 : value; }
    private double round(double value){ return Math.round(value * 100.0) / 100.0; }
    private LocalDate parseDate(String value){
        if(value==null || value.isBlank()) return null;
        try { return LocalDate.parse(value); } catch(Exception e){ return null; }
    }
}
