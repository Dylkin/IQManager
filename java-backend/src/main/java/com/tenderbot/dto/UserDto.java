package com.tenderbot.dto;

import com.tenderbot.entity.UserRole;
import com.tenderbot.entity.UserStatus;

public record UserDto(
    Long id,
    String email,
    String fullName,
    UserRole role,
    UserStatus status
) {}
