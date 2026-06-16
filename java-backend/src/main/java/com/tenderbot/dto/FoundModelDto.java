package com.tenderbot.dto;

public record FoundModelDto(
    Long id,
    String productName,
    String productUrl,
    Double price,
    Double priceByn,
    Double exchangeRate,
    String supplierSite,
    Double matchScore,
    Double semanticScore,
    Double parametricScore,
    Integer rankPosition
) {}
