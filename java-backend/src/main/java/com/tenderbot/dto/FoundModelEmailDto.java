package com.tenderbot.dto;

import com.tenderbot.entity.EmailDirection;
import com.tenderbot.entity.EmailStatus;
import com.tenderbot.entity.FoundModelEmail;

import java.time.LocalDateTime;

public record FoundModelEmailDto(
        Long id,
        Long foundModelId,
        String direction,
        String subject,
        String body,
        String fromEmail,
        String toEmail,
        String status,
        String errorMessage,
        String messageId,
        LocalDateTime createdAt
) {
    public static FoundModelEmailDto fromEntity(FoundModelEmail email) {
        return new FoundModelEmailDto(
                email.getId(),
                email.getFoundModel() != null ? email.getFoundModel().getId() : null,
                email.getDirection() != null ? email.getDirection().name() : null,
                email.getSubject(),
                email.getBody(),
                email.getFromEmail(),
                email.getToEmail(),
                email.getStatus() != null ? email.getStatus().name() : null,
                email.getErrorMessage(),
                email.getMessageId(),
                email.getCreatedAt()
        );
    }
}
