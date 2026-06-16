package com.tenderbot.dto;

public record EquipmentCharacteristicDto(
    Long id,
    Long equipmentTypeId,
    String key,
    String label,
    String unit,
    Integer sortOrder
) {}
