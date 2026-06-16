package com.tenderbot.dto;

public record EmailRequestDto(
    Long tenderItemId,
    String supplierEmail,
    String subject,
    String body
) {}
