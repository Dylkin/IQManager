package com.tenderbot.dto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    UserDto user
) {}
