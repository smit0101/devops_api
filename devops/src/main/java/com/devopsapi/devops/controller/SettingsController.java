package com.devopsapi.devops.controller;

import com.devopsapi.devops.model.AppSetting;
import com.devopsapi.devops.repository.AppSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    @Autowired
    private AppSettingRepository appSettingRepository;

    @GetMapping
    public ResponseEntity<Map<String, String>> getSettings() {
        Map<String, String> settings = new HashMap<>();
        
        // Return existing keys with masked values
        appSettingRepository.findAll().forEach(setting -> {
            String masked = setting.getValue();
            if (setting.getKey().contains("TOKEN") || setting.getKey().contains("SECRET")) {
                masked = (setting.getValue() != null && !setting.getValue().isEmpty()) ? "********" : "";
            }
            settings.put(setting.getKey(), masked);
        });

        // Ensure keys exist in response even if not in DB yet
        if (!settings.containsKey("GITHUB_TOKEN")) settings.put("GITHUB_TOKEN", "");
        if (!settings.containsKey("WEBHOOK_SECRET")) settings.put("WEBHOOK_SECRET", "");
        if (!settings.containsKey("PUBLIC_HOST")) settings.put("PUBLIC_HOST", "http://localhost:8080");

        return ResponseEntity.ok(settings);
    }

    @PostMapping
    public ResponseEntity<Void> updateSettings(@RequestBody Map<String, String> newSettings) {
        newSettings.forEach((key, value) -> {
            // Only update if value is provided and not masked
            if (value != null && !value.isEmpty() && !"********".equals(value)) {
                AppSetting setting = appSettingRepository.findById(key).orElse(new AppSetting(key, ""));
                setting.setValue(value);
                appSettingRepository.save(setting);
            }
        });
        return ResponseEntity.ok().build();
    }
}
