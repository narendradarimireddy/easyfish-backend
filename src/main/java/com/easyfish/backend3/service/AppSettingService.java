package com.easyfish.backend3.service;

import com.easyfish.backend3.entity.AppSetting;
import com.easyfish.backend3.repository.AppSettingRepository;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class AppSettingService {
    private final AppSettingRepository repo;
    public AppSettingService(AppSettingRepository repo){ this.repo = repo; }
    public String get(String key, String fallback){ return repo.findBySettingKey(key).map(AppSetting::getSettingValue).orElse(fallback); }
    public double getDouble(String key, double fallback){ try { return Double.parseDouble(get(key, String.valueOf(fallback))); } catch(Exception e){ return fallback; } }
    public Map<String,String> all(){ Map<String,String> map = new LinkedHashMap<>(); repo.findAll().forEach(s -> map.put(s.getSettingKey(), s.getSettingValue())); map.putIfAbsent("delivery_charge", "1"); return map; }
    public AppSetting save(String key, String value){ AppSetting s = repo.findBySettingKey(key).orElse(new AppSetting()); s.setSettingKey(key); s.setSettingValue(value); return repo.save(s); }
}
