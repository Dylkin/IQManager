package com.tenderbot.dto;

import com.tenderbot.entity.UserRole;
import com.tenderbot.entity.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6) String password,
    String fullName,
    UserRole role,
    UserStatus status
) {}
