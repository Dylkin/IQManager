package com.tenderbot.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Converter(autoApply = true)
public class LogLevelConverter implements AttributeConverter<LogLevel, String> {

    private static final Logger log = LoggerFactory.getLogger(LogLevelConverter.class);

    @Override
    public String convertToDatabaseColumn(LogLevel level) {
        return level == null ? null : level.name();
    }

    @Override
    public LogLevel convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return LogLevel.INFO;
        }
        try {
            return LogLevel.valueOf(dbData.trim());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown LogLevel value in database: '{}', defaulting to INFO", dbData);
            return LogLevel.INFO;
        }
    }
}
