package com.tenderbot.dto;

import com.tenderbot.entity.LogLevel;
import java.time.LocalDateTime;

public record LogDto(
    Long id,
    Long tenderId,
    String tenderNumber,
    String step,
    String message,
    LogLevel level,
    String details,
    LocalDateTime createdAt
) {}
