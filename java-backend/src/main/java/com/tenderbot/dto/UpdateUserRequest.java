package com.tenderbot.dto;

import com.tenderbot.entity.UserRole;
import com.tenderbot.entity.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(
    @NotBlank @Email String email,
    String fullName,
    UserRole role,
    UserStatus status
) {}
