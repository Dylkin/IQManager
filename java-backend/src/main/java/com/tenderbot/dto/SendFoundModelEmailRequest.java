package com.tenderbot.dto;

public record SendFoundModelEmailRequest(
        String toEmail,
        String subject,
        String body
) {}
