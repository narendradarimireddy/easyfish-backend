package com.easyfish.backend3.controller;

import com.easyfish.backend3.service.AppSettingService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins="*")
public class SettingsController {
    private final AppSettingService service;
    public SettingsController(AppSettingService service){ this.service = service; }
    @GetMapping public Map<String,String> all(){ return service.all(); }
    @PutMapping("/{key}") public Map<String,String> save(@PathVariable String key, @RequestBody Map<String,String> body){ service.save(key, body.getOrDefault("value", "")); return service.all(); }
}
