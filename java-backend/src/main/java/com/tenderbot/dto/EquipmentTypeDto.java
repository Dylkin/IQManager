package com.tenderbot.dto;

import java.util.List;

public record EquipmentTypeDto(
    Long id,
    String name,
    String code,
    List<EquipmentCharacteristicDto> characteristics,
    String createdAt
) {}
