package com.tenderbot.dto;

import java.util.Map;

public record CreateCatalogItemRequest(
    Long equipmentTypeId,
    Long manufacturerId,
    String modelName,
    String modelNumber,
    Map<Long, String> specValues
) {}
