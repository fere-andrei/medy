package com.example.medy.core.security.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminPasswordResetRequestDTO(@NotBlank @Size(min = 8, message = "must be at least 8 characters") String newPassword) {
}
