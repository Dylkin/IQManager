package com.tenderbot.dto;

public record UpdatePricingRequest(
    Double deliveryCostByn,
    Double markupPercent
) {}
