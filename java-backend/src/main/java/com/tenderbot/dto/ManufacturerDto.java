package com.tenderbot.dto;

public record ManufacturerDto(
    Long id,
    String name,
    String country,
    String website,
    String createdAt
) {}
