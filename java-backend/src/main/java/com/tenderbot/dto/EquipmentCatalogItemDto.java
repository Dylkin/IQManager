package com.tenderbot.dto;

import java.util.List;

public record EquipmentCatalogItemDto(
    Long id,
    EquipmentTypeDto equipmentType,
    ManufacturerDto manufacturer,
    String modelName,
    String modelNumber,
    List<EquipmentCatalogSpecDto> specs,
    String createdAt
) {}
