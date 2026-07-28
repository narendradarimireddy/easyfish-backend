package com.easyfish.backend3.service;

import com.easyfish.backend3.entity.Product;
import com.easyfish.backend3.entity.StockHistory;
import com.easyfish.backend3.repository.ProductRepository;
import com.easyfish.backend3.repository.StockHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class StockService {
    private final ProductRepository productRepo;
    private final StockHistoryRepository historyRepo;
    public StockService(ProductRepository productRepo, StockHistoryRepository historyRepo){ this.productRepo=productRepo; this.historyRepo=historyRepo; }
    @Transactional
    public Product addStock(Long id, Double qty, String reason){
        Product p = productRepo.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
        double change = Math.max(0, qty==null?0:qty);
        List<Product> sameStockProducts = sameStockProducts(p);
        double selectedOld = p.getStockQuantity()==null?0:p.getStockQuantity();
        double selectedNew = selectedOld + change;
        Product selectedSaved = null;
        for(Product item: sameStockProducts){
            double old = item.getStockQuantity()==null?0:item.getStockQuantity();
            item.setStockQuantity(old + change);
            if(p.getStockUnit()!=null && !p.getStockUnit().isBlank()) item.setStockUnit(p.getStockUnit());
            normalizeStatus(item);
            Product saved = productRepo.save(item);
            if(Objects.equals(saved.getId(), id)) selectedSaved = saved;
        }
        Product logProduct = selectedSaved != null ? selectedSaved : productRepo.findById(id).orElse(p);
        log(logProduct, selectedOld, change, selectedNew, "ADD", reason);
        return logProduct;
    }
    @Transactional
    public Product editStock(Long id, Double qty, String reason){
        Product p = productRepo.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
        double next = Math.max(0, qty==null?0:qty);
        List<Product> sameStockProducts = sameStockProducts(p);
        double selectedOld = p.getStockQuantity()==null?0:p.getStockQuantity();
        double delta = next - selectedOld;
        Product selectedSaved = null;
        for(Product item: sameStockProducts){
            item.setStockQuantity(next);
            if(p.getStockUnit()!=null && !p.getStockUnit().isBlank()) item.setStockUnit(p.getStockUnit());
            normalizeStatus(item);
            Product saved = productRepo.save(item);
            if(Objects.equals(saved.getId(), id)) selectedSaved = saved;
        }
        Product logProduct = selectedSaved != null ? selectedSaved : productRepo.findById(id).orElse(p);
        log(logProduct, selectedOld, delta, next, "EDIT", reason);
        return logProduct;
    }
    private List<Product> sameStockProducts(Product p){
        String baseNameRaw = p.getName() == null ? "" : p.getName().trim();
        String baseLocalRaw = p.getLocalName() == null ? "" : p.getLocalName().trim();
        List<Product> locked = productRepo.findSameStockProductsForUpdate(baseNameRaw, baseLocalRaw);
        if (locked != null && !locked.isEmpty()) return locked;
        String baseName = clean(p.getName());
        String baseLocal = clean(p.getLocalName());
        List<Product> sameStockProducts = productRepo.findAll().stream().filter(item ->
                clean(item.getName()).equals(baseName) && clean(item.getLocalName()).equals(baseLocal)
        ).toList();
        return sameStockProducts.isEmpty() ? List.of(p) : sameStockProducts;
    }
    private String clean(String value){ return value==null?"":value.trim().toLowerCase(); }
    @Transactional
    public synchronized void deductStock(Product p, double kg){
        if(p==null || kg<=0) return;
        Product locked = productRepo.findByIdForUpdate(p.getId()).orElseThrow(() -> new RuntimeException("Product not found"));
        List<Product> sameStockProducts = sameStockProducts(locked);
        double available = locked.getStockQuantity()==null?0:locked.getStockQuantity();
        if(available + 0.00001 < kg){
            throw new RuntimeException((locked.getName()==null?"Product":locked.getName()) + " has only " + available + " " + (locked.getStockUnit()==null?"kg":locked.getStockUnit()) + " available");
        }
        Product logProduct = locked;
        for(Product item: sameStockProducts){
            double old = item.getStockQuantity()==null?0:item.getStockQuantity();
            double next = Math.max(0, old - kg);
            item.setStockQuantity(next);
            normalizeStatus(item);
            Product saved = productRepo.save(item);
            if(saved.getId().equals(locked.getId())) logProduct = saved;
        }
        double old = available;
        double next = Math.max(0, old - kg);
        log(logProduct, old, next-old, next, "SOLD", "Order placed");
    }

    @Transactional
    public synchronized void restoreStock(Product p, double kg, String reason){
        if(p==null || kg<=0) return;
        Product locked = productRepo.findByIdForUpdate(p.getId()).orElseThrow(() -> new RuntimeException("Product not found"));
        List<Product> sameStockProducts = sameStockProducts(locked);
        double available = locked.getStockQuantity()==null?0:locked.getStockQuantity();
        Product logProduct = locked;
        for(Product item: sameStockProducts){
            double old = item.getStockQuantity()==null?0:item.getStockQuantity();
            double next = old + kg;
            item.setStockQuantity(next);
            normalizeStatus(item);
            Product saved = productRepo.save(item);
            if(saved.getId().equals(locked.getId())) logProduct = saved;
        }
        log(logProduct, available, kg, available + kg, "RESTORE", reason == null || reason.isBlank() ? "Order cancelled" : reason);
    }
    public void normalizeStatus(Product p){ double stock = p.getStockQuantity()==null?0:p.getStockQuantity(); double low = p.getLowStockLimit()==null?0:p.getLowStockLimit(); p.setInStock(stock > 0); if(stock <= 0) p.setStockStatus("OUT_OF_STOCK"); else if(low > 0 && stock <= low) p.setStockStatus("LOW_STOCK"); else p.setStockStatus("IN_STOCK"); if(p.getStockUnit()==null || p.getStockUnit().isBlank()) p.setStockUnit("kg"); }
    private void log(Product p, double oldStock, double change, double newStock, String action, String reason){ StockHistory h = new StockHistory(); h.setProduct(p); h.setOldStock(oldStock); h.setChangeQuantity(change); h.setNewStock(newStock); h.setActionType(action); h.setReason(reason); historyRepo.save(h); }
    public List<StockHistory> history(){ return historyRepo.findAllByOrderByIdDesc(); }
    public Map<String,Object> report(){ return report(null, null); }
    public Map<String,Object> report(String from, String to){
        List<Product> products = productRepo.findAll();
        List<StockHistory> allHist = history();
        java.time.ZoneId zone = java.time.ZoneId.of("Asia/Kolkata");
        java.time.LocalDate today = java.time.LocalDate.now(zone);
        java.time.LocalDate fromDate = parseDate(from);
        java.time.LocalDate toDate = parseDate(to);
        List<StockHistory> hist = allHist.stream().filter(h -> {
            if(h.getCreatedAt()==null) return true;
            java.time.LocalDate d = h.getCreatedAt().toLocalDate();
            if(fromDate!=null && d.isBefore(fromDate)) return false;
            if(toDate!=null && d.isAfter(toDate)) return false;
            return true;
        }).toList();
        Map<String, Product> masters = new LinkedHashMap<>();
        for(Product p: products){
            String key = (String.valueOf(p.getName()).trim().toLowerCase()+"|"+String.valueOf(p.getLocalName()).trim().toLowerCase());
            Product current = masters.get(key);
            if(current==null || (p.getStockQuantity()!=null && (current.getStockQuantity()==null || p.getStockQuantity()>current.getStockQuantity()))) masters.put(key, p);
        }
        Collection<Product> uniqueProducts = masters.values();
        double totalStock = uniqueProducts.stream().mapToDouble(p -> p.getStockQuantity()==null?0:p.getStockQuantity()).sum();
        long low = uniqueProducts.stream().filter(p -> "LOW_STOCK".equalsIgnoreCase(String.valueOf(p.getStockStatus()))).count();
        long out = uniqueProducts.stream().filter(p -> "OUT_OF_STOCK".equalsIgnoreCase(String.valueOf(p.getStockStatus())) || Boolean.FALSE.equals(p.getInStock())).count();
        double todayAdded = allHist.stream().filter(h -> isPositiveStockAddition(h) && h.getCreatedAt()!=null && h.getCreatedAt().toLocalDate().equals(today)).mapToDouble(h -> h.getChangeQuantity()==null?0:h.getChangeQuantity()).sum();
        double monthAdded = allHist.stream().filter(h -> isPositiveStockAddition(h) && h.getCreatedAt()!=null && java.time.YearMonth.from(h.getCreatedAt().toLocalDate()).equals(java.time.YearMonth.from(today))).mapToDouble(h -> h.getChangeQuantity()==null?0:h.getChangeQuantity()).sum();
        double yearAdded = allHist.stream().filter(h -> isPositiveStockAddition(h) && h.getCreatedAt()!=null && h.getCreatedAt().toLocalDate().getYear() == today.getYear()).mapToDouble(h -> h.getChangeQuantity()==null?0:h.getChangeQuantity()).sum();
        double todaySold = allHist.stream().filter(h -> h.getCreatedAt()!=null && h.getCreatedAt().toLocalDate().equals(today) && "SOLD".equalsIgnoreCase(String.valueOf(h.getActionType()))).mapToDouble(h -> Math.abs(h.getChangeQuantity()==null?0:h.getChangeQuantity())).sum();
        Map<String, Map<String, Object>> productAddedStats = new LinkedHashMap<>();
        for(Product p: uniqueProducts){
            String key = clean(p.getName()) + "|" + clean(p.getLocalName());
            Map<String,Object> itemStats = new LinkedHashMap<>();
            itemStats.put("today", roundStock(allHist.stream().filter(h -> sameHistoryProduct(h, key) && isPositiveStockAddition(h) && h.getCreatedAt()!=null && h.getCreatedAt().toLocalDate().equals(today)).mapToDouble(h -> h.getChangeQuantity()==null?0:h.getChangeQuantity()).sum()));
            itemStats.put("month", roundStock(allHist.stream().filter(h -> sameHistoryProduct(h, key) && isPositiveStockAddition(h) && h.getCreatedAt()!=null && java.time.YearMonth.from(h.getCreatedAt().toLocalDate()).equals(java.time.YearMonth.from(today))).mapToDouble(h -> h.getChangeQuantity()==null?0:h.getChangeQuantity()).sum()));
            itemStats.put("year", roundStock(allHist.stream().filter(h -> sameHistoryProduct(h, key) && isPositiveStockAddition(h) && h.getCreatedAt()!=null && h.getCreatedAt().toLocalDate().getYear() == today.getYear()).mapToDouble(h -> h.getChangeQuantity()==null?0:h.getChangeQuantity()).sum()));
            productAddedStats.put(key, itemStats);
        }
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("totalProducts", uniqueProducts.size());
        m.put("publishedVariants", products.size());
        m.put("totalStockKg", totalStock);
        m.put("lowStockProducts", low);
        m.put("outOfStockProducts", out);
        m.put("todayAddedKg", roundStock(todayAdded));
        m.put("monthAddedKg", roundStock(monthAdded));
        m.put("yearAddedKg", roundStock(yearAdded));
        m.put("todaySoldKg", roundStock(todaySold));
        m.put("productAddedStats", productAddedStats);
        m.put("history", hist);
        return m;
    }
    private boolean isPositiveStockAddition(StockHistory h){
        double change = h.getChangeQuantity()==null ? 0 : h.getChangeQuantity();
        String action = String.valueOf(h.getActionType());
        return change > 0 && ("ADD".equalsIgnoreCase(action) || "EDIT".equalsIgnoreCase(action) || "RESTORE".equalsIgnoreCase(action));
    }
    private boolean sameHistoryProduct(StockHistory h, String key){
        Product p = h.getProduct();
        if(p == null) return false;
        return (clean(p.getName()) + "|" + clean(p.getLocalName())).equals(key);
    }
    private double roundStock(double value){ return Math.round(value * 100.0) / 100.0; }
    private java.time.LocalDate parseDate(String value){ try { return value==null || value.isBlank() ? null : java.time.LocalDate.parse(value); } catch(Exception e){ return null; } }
}
