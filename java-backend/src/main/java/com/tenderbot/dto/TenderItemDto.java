package com.tenderbot.dto;

import com.tenderbot.entity.ItemStatus;
import java.util.List;

public record TenderItemDto(
    Long id,
    String lotNumber,
    String description,
    String originalDescription,
    Double quantity,
    String unit,
    Double estimatedPrice,
    String currency,
    String okpd2Code,
    String foundModelName,
    String foundModelUrl,
    Double foundModelPrice,
    Double foundModelPriceByn,
    Double foundModelExchangeRate,
    Double deliveryCostByn,
    Double markupPercent,
    Double finalPriceByn,
    String supplierSite,
    ItemStatus status,
    String documentDescription,
    String documentUrl,
    String documentFileName,
    String extractedParams,
    Double matchScore,
    List<FoundModelDto> foundModels
) {}
