package com.easyfish.backend3.controller;

import com.easyfish.backend3.entity.Product;
import com.easyfish.backend3.entity.StockHistory;
import com.easyfish.backend3.service.StockService;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/stock")
@CrossOrigin(origins="*")
public class StockController {
    private final StockService service;
    public StockController(StockService service){ this.service=service; }
    @PostMapping("/{productId}/add") public Product add(@PathVariable Long productId, @RequestBody Map<String,String> body){ return service.addStock(productId, Double.valueOf(body.getOrDefault("quantity","0")), body.getOrDefault("reason", "New stock")); }
    @PutMapping("/{productId}/edit") public Product edit(@PathVariable Long productId, @RequestBody Map<String,String> body){ return service.editStock(productId, Double.valueOf(body.getOrDefault("quantity","0")), body.getOrDefault("reason", "Manual correction")); }
    @GetMapping("/history") public List<StockHistory> history(){ return service.history(); }
    @GetMapping("/report") public Map<String,Object> report(@RequestParam(required=false) String from, @RequestParam(required=false) String to){ return service.report(from, to); }
}
