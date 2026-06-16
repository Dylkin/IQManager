package com.tenderbot.dto;

public record EquipmentCatalogSpecDto(
    Long id,
    Long catalogItemId,
    EquipmentCharacteristicDto characteristic,
    String value
) {}
