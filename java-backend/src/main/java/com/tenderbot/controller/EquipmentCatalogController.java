package com.tenderbot.controller;

import com.tenderbot.dto.*;
import com.tenderbot.service.EquipmentCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment-catalog")
public class EquipmentCatalogController {

    private final EquipmentCatalogService catalogService;

    public EquipmentCatalogController(EquipmentCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // Types
    @GetMapping("/types")
    public List<EquipmentTypeDto> getAllTypes() {
        return catalogService.getAllTypes();
    }

    @GetMapping("/types/{id}")
    public ResponseEntity<EquipmentTypeDto> getTypeById(@PathVariable Long id) {
        return catalogService.getTypeById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/types")
    public ResponseEntity<EquipmentTypeDto> createType(@RequestBody EquipmentTypeDto dto) {
        return ResponseEntity.ok(catalogService.createType(dto.name(), dto.code()));
    }

    @DeleteMapping("/types/{id}")
    public ResponseEntity<Void> deleteType(@PathVariable Long id) {
        catalogService.deleteType(id);
        return ResponseEntity.ok().build();
    }

    // Manufacturers
    @GetMapping("/manufacturers")
    public List<ManufacturerDto> getAllManufacturers() {
        return catalogService.getAllManufacturers();
    }

    @PostMapping("/manufacturers")
    public ResponseEntity<ManufacturerDto> createManufacturer(@RequestBody ManufacturerDto dto) {
        return ResponseEntity.ok(catalogService.createManufacturer(dto.name(), dto.country(), dto.website()));
    }

    @DeleteMapping("/manufacturers/{id}")
    public ResponseEntity<Void> deleteManufacturer(@PathVariable Long id) {
        catalogService.deleteManufacturer(id);
        return ResponseEntity.ok().build();
    }

    // Catalog Items
    @GetMapping("/items")
    public List<EquipmentCatalogItemDto> getAllItems(
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) Long manufacturerId) {
        if (typeId != null && manufacturerId != null) {
            return catalogService.getItemsByType(typeId).stream()
                    .filter(i -> i.manufacturer() != null && i.manufacturer().id().equals(manufacturerId))
                    .toList();
        }
        if (typeId != null) return catalogService.getItemsByType(typeId);
        if (manufacturerId != null) return catalogService.getItemsByManufacturer(manufacturerId);
        return catalogService.getAllItems();
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<EquipmentCatalogItemDto> getItemById(@PathVariable Long id) {
        return catalogService.getItemById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/items")
    public ResponseEntity<EquipmentCatalogItemDto> createItem(@RequestBody CreateCatalogItemRequest request) {
        return ResponseEntity.ok(catalogService.createItem(request));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<EquipmentCatalogItemDto> updateItem(@PathVariable Long id, @RequestBody CreateCatalogItemRequest request) {
        return ResponseEntity.ok(catalogService.updateItem(id, request));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        catalogService.deleteItem(id);
        return ResponseEntity.ok().build();
    }
}
