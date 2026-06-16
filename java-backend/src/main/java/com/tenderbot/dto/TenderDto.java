package com.tenderbot.dto;

import com.tenderbot.entity.TenderStatus;
import java.time.LocalDateTime;
import java.util.List;

public record TenderDto(
    Long id,
    String tenderNumber,
    String title,
    String url,
    String organizer,
    LocalDateTime publishDate,
    LocalDateTime deadlineDate,
    TenderStatus status,
    Double totalAmount,
    String currency,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<TenderItemDto> items
) {}
