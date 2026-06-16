package com.tenderbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tenderbot.dto.*;
import com.tenderbot.entity.*;
import com.tenderbot.repository.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class EquipmentCatalogService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Logger log = LoggerFactory.getLogger(EquipmentCatalogService.class);

    private final EquipmentTypeRepository typeRepository;
    private final EquipmentTypeCharacteristicRepository characteristicRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final EquipmentCatalogItemRepository catalogItemRepository;
    private final EquipmentCatalogSpecRepository catalogSpecRepository;

    public EquipmentCatalogService(EquipmentTypeRepository typeRepository,
                                   EquipmentTypeCharacteristicRepository characteristicRepository,
                                   ManufacturerRepository manufacturerRepository,
                                   EquipmentCatalogItemRepository catalogItemRepository,
                                   EquipmentCatalogSpecRepository catalogSpecRepository) {
        this.typeRepository = typeRepository;
        this.characteristicRepository = characteristicRepository;
        this.manufacturerRepository = manufacturerRepository;
        this.catalogItemRepository = catalogItemRepository;
        this.catalogSpecRepository = catalogSpecRepository;
    }

    // ========== EquipmentType ==========

    public List<EquipmentTypeDto> getAllTypes() {
        return typeRepository.findAll().stream().map(this::toTypeDto).toList();
    }

    public Optional<EquipmentTypeDto> getTypeById(Long id) {
        return typeRepository.findById(id).map(this::toTypeDto);
    }

    @Transactional
    public EquipmentTypeDto createType(String name, String code) {
        EquipmentType type = new EquipmentType(name, code);
        return toTypeDto(typeRepository.save(type));
    }

    @Transactional
    public void deleteType(Long id) {
        typeRepository.deleteById(id);
    }

    // ========== Manufacturer ==========

    public List<ManufacturerDto> getAllManufacturers() {
        return manufacturerRepository.findAll().stream().map(this::toManufacturerDto).toList();
    }

    @Transactional
    public ManufacturerDto ensureManufacturer(String name) {
        if (name == null || name.isBlank()) return null;
        return manufacturerRepository.findByNameIgnoreCase(name.trim())
                .map(this::toManufacturerDto)
                .orElseGet(() -> toManufacturerDto(manufacturerRepository.save(new Manufacturer(name.trim()))));
    }

    @Transactional
    public ManufacturerDto createManufacturer(String name, String country, String website) {
        Manufacturer m = new Manufacturer(name);
        m.setCountry(country);
        m.setWebsite(website);
        return toManufacturerDto(manufacturerRepository.save(m));
    }

    @Transactional
    public void deleteManufacturer(Long id) {
        manufacturerRepository.deleteById(id);
    }

    // ========== Catalog Items ==========

    public List<EquipmentCatalogItemDto> getAllItems() {
        return catalogItemRepository.findAll().stream().map(this::toItemDto).toList();
    }

    public List<EquipmentCatalogItemDto> getItemsByType(Long typeId) {
        return catalogItemRepository.findByEquipmentTypeId(typeId).stream().map(this::toItemDto).toList();
    }

    public List<EquipmentCatalogItemDto> getItemsByManufacturer(Long manufacturerId) {
        return catalogItemRepository.findByManufacturerId(manufacturerId).stream().map(this::toItemDto).toList();
    }

    public Optional<EquipmentCatalogItemDto> getItemById(Long id) {
        return catalogItemRepository.findById(id).map(this::toItemDto);
    }

    @Transactional
    public EquipmentCatalogItemDto createItem(CreateCatalogItemRequest request) {
        EquipmentType type = typeRepository.findById(request.equipmentTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Equipment type not found"));
        Manufacturer manufacturer = manufacturerRepository.findById(request.manufacturerId())
                .orElseThrow(() -> new IllegalArgumentException("Manufacturer not found"));

        EquipmentCatalogItem item = new EquipmentCatalogItem(type, manufacturer, request.modelName(), request.modelNumber());
        item = catalogItemRepository.save(item);

        if (request.specValues() != null) {
            saveSpecs(item, request.specValues());
        }

        return toItemDto(item);
    }

    @Transactional
    public EquipmentCatalogItemDto updateItem(Long id, CreateCatalogItemRequest request) {
        EquipmentCatalogItem item = catalogItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));

        if (request.equipmentTypeId() != null) {
            item.setEquipmentType(typeRepository.findById(request.equipmentTypeId()).orElseThrow());
        }
        if (request.manufacturerId() != null) {
            item.setManufacturer(manufacturerRepository.findById(request.manufacturerId()).orElseThrow());
        }
        if (request.modelName() != null) {
            item.setModelName(request.modelName());
        }
        if (request.modelNumber() != null) {
            item.setModelNumber(request.modelNumber());
        }

        item = catalogItemRepository.save(item);

        if (request.specValues() != null) {
            catalogSpecRepository.deleteByCatalogItemId(item.getId());
            saveSpecs(item, request.specValues());
        }

        return toItemDto(item);
    }

    @Transactional
    public void deleteItem(Long id) {
        catalogItemRepository.deleteById(id);
    }

    // ========== Auto-population from extracted params ==========

    @Transactional
    public EquipmentTypeDto ensureTypeAndCharacteristics(String typeName, Map<String, String> paramLabels) {
        log.info("ensureTypeAndCharacteristics called for type: {} with {} params", typeName, paramLabels.size());
        if (typeName == null || typeName.isBlank()) return null;

        String rawCode = typeName.toLowerCase().replaceAll("[^a-z0-9]", "_");
        final String code = rawCode.isEmpty() ? "unknown" : rawCode;

        EquipmentType type = typeRepository.findByNameIgnoreCase(typeName.trim())
                .orElseGet(() -> typeRepository.save(new EquipmentType(typeName.trim(), code)));
        log.info("EquipmentType resolved: id={}, name={}", type.getId(), type.getName());

        int sortOrder = 0;
        for (Map.Entry<String, String> entry : paramLabels.entrySet()) {
            String key = entry.getKey();
            String label = entry.getValue();
            if (key == null || key.isBlank() || "equipmentType".equalsIgnoreCase(key) || "model".equalsIgnoreCase(key)) continue;

            final int order = sortOrder++;
            characteristicRepository.findByEquipmentTypeIdAndKeyIgnoreCase(type.getId(), key)
                    .orElseGet(() -> characteristicRepository.save(
                            new EquipmentTypeCharacteristic(type, key, label != null ? label : key, null, order)));
        }

        return toTypeDto(type);
    }

    @Transactional
    public EquipmentCatalogItemDto saveModel(String typeName, String manufacturerName, String modelName, Map<String, String> specValues) {
        if (typeName == null || typeName.isBlank() || modelName == null || modelName.isBlank()) return null;

        EquipmentType type = typeRepository.findByNameIgnoreCase(typeName.trim()).orElse(null);
        if (type == null) return null;

        Manufacturer manufacturer = null;
        if (manufacturerName != null && !manufacturerName.isBlank()) {
            manufacturer = manufacturerRepository.findByNameIgnoreCase(manufacturerName.trim())
                    .orElseGet(() -> manufacturerRepository.save(new Manufacturer(manufacturerName.trim())));
        }
        if (manufacturer == null) {
            manufacturer = manufacturerRepository.findByNameIgnoreCase("Unknown")
                    .orElseGet(() -> manufacturerRepository.save(new Manufacturer("Unknown")));
        }

        final Manufacturer finalManufacturer = manufacturer;
        EquipmentCatalogItem item = catalogItemRepository
                .findByEquipmentTypeIdAndManufacturerIdAndModelNameIgnoreCase(type.getId(), manufacturer.getId(), modelName.trim())
                .orElseGet(() -> catalogItemRepository.save(new EquipmentCatalogItem(type, finalManufacturer, modelName.trim(), null)));

        if (specValues != null && !specValues.isEmpty()) {
            Map<Long, String> specMap = new HashMap<>();
            for (Map.Entry<String, String> entry : specValues.entrySet()) {
                characteristicRepository.findByEquipmentTypeIdAndKeyIgnoreCase(type.getId(), entry.getKey())
                        .ifPresent(ch -> specMap.put(ch.getId(), entry.getValue()));
            }
            catalogSpecRepository.deleteByCatalogItemId(item.getId());
            saveSpecs(item, specMap);
        }

        return toItemDto(item);
    }

    @Transactional
    public EquipmentCatalogItemDto saveDraftModel(String typeName, String modelName, Map<String, String> specValues) {
        log.info("saveDraftModel called for type: {}, model: {}, specs: {}", typeName, modelName, specValues != null ? specValues.size() : 0);
        if (typeName == null || typeName.isBlank() || modelName == null || modelName.isBlank()) return null;

        EquipmentType type = typeRepository.findByNameIgnoreCase(typeName.trim()).orElse(null);
        if (type == null) {
            log.warn("EquipmentType not found for name: {}", typeName);
            return null;
        }

        EquipmentCatalogItem item = catalogItemRepository
                .findByEquipmentTypeIdAndModelNameIgnoreCase(type.getId(), modelName.trim())
                .orElseGet(() -> {
                    Manufacturer unknown = manufacturerRepository.findByNameIgnoreCase("Unknown")
                            .orElseGet(() -> manufacturerRepository.save(new Manufacturer("Unknown")));
                    log.info("Creating new catalog item: typeId={}, model={}", type.getId(), modelName);
                    return catalogItemRepository.save(new EquipmentCatalogItem(type, unknown, modelName.trim(), null));
                });

        if (specValues != null && !specValues.isEmpty()) {
            Map<Long, String> specMap = new HashMap<>();
            for (Map.Entry<String, String> entry : specValues.entrySet()) {
                characteristicRepository.findByEquipmentTypeIdAndKeyIgnoreCase(type.getId(), entry.getKey())
                        .ifPresent(ch -> specMap.put(ch.getId(), entry.getValue()));
            }
            catalogSpecRepository.deleteByCatalogItemId(item.getId());
            saveSpecs(item, specMap);
        }

        log.info("saveDraftModel completed for item id: {}", item.getId());
        return toItemDto(item);
    }

    // ========== Private helpers ==========

    private void saveSpecs(EquipmentCatalogItem item, Map<Long, String> specValues) {
        for (Map.Entry<Long, String> entry : specValues.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) continue;
            EquipmentTypeCharacteristic ch = characteristicRepository.findById(entry.getKey()).orElse(null);
            if (ch == null) continue;
            catalogSpecRepository.save(new EquipmentCatalogSpec(item, ch, entry.getValue()));
        }
    }

    private EquipmentTypeDto toTypeDto(EquipmentType type) {
        List<EquipmentCharacteristicDto> chars = characteristicRepository.findByEquipmentTypeId(type.getId()).stream()
                .map(this::toCharacteristicDto).toList();
        return new EquipmentTypeDto(
                type.getId(), type.getName(), type.getCode(), chars,
                type.getCreatedAt() != null ? type.getCreatedAt().format(DATE_FMT) : null
        );
    }

    private EquipmentCharacteristicDto toCharacteristicDto(EquipmentTypeCharacteristic ch) {
        return new EquipmentCharacteristicDto(
                ch.getId(), ch.getEquipmentType() != null ? ch.getEquipmentType().getId() : null,
                ch.getKey(), ch.getLabel(), ch.getUnit(), ch.getSortOrder()
        );
    }

    private ManufacturerDto toManufacturerDto(Manufacturer m) {
        return new ManufacturerDto(
                m.getId(), m.getName(), m.getCountry(), m.getWebsite(),
                m.getCreatedAt() != null ? m.getCreatedAt().format(DATE_FMT) : null
        );
    }

    private EquipmentCatalogItemDto toItemDto(EquipmentCatalogItem item) {
        List<EquipmentCatalogSpecDto> specs = catalogSpecRepository.findByCatalogItemId(item.getId()).stream()
                .map(this::toSpecDto).toList();
        return new EquipmentCatalogItemDto(
                item.getId(),
                item.getEquipmentType() != null ? toTypeDto(item.getEquipmentType()) : null,
                item.getManufacturer() != null ? toManufacturerDto(item.getManufacturer()) : null,
                item.getModelName(), item.getModelNumber(), specs,
                item.getCreatedAt() != null ? item.getCreatedAt().format(DATE_FMT) : null
        );
    }

    private EquipmentCatalogSpecDto toSpecDto(EquipmentCatalogSpec spec) {
        return new EquipmentCatalogSpecDto(
                spec.getId(),
                spec.getCatalogItem() != null ? spec.getCatalogItem().getId() : null,
                spec.getCharacteristic() != null ? toCharacteristicDto(spec.getCharacteristic()) : null,
                spec.getValue()
        );
    }
}
