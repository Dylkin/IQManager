package com.tenderbot.controller;

import com.tenderbot.dto.ConfigDto;
import com.tenderbot.entity.ConfigGroup;
import com.tenderbot.service.ConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/configs")

public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    public List<ConfigDto> getAll() {
        return configService.getAll();
    }

    @GetMapping("/group/{group}")
    public List<ConfigDto> getByGroup(@PathVariable String group) {
        return configService.getByGroup(ConfigGroup.valueOf(group.toUpperCase()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConfigDto> getById(@PathVariable Long id) {
        ConfigDto dto = configService.getById(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @GetMapping("/key/{key}")
    public ResponseEntity<ConfigDto> getByKey(@PathVariable String key) {
        ConfigDto dto = configService.getByKey(key);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ConfigDto create(@RequestBody ConfigDto dto) {
        return configService.create(dto);
    }

    @PutMapping("/{id}")
    public ConfigDto update(@PathVariable Long id, @RequestBody ConfigDto dto) {
        return configService.update(id, dto);
    }

    @PutMapping("/key/{key}")
    public ConfigDto updateByKey(@PathVariable String key, @RequestBody Map<String, String> body) {
        return configService.updateByKey(key, body.get("value"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        configService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/groups")
    public ConfigGroup[] getGroups() {
        return ConfigGroup.values();
    }
}
