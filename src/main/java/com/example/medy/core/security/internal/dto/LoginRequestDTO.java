package com.example.medy.core.security.internal.dto;

import jakarta.validation.constraints.NotBlank;

/** {@code orgSlug} is omitted for a SUPER_ADMIN login. */
public record LoginRequestDTO(String orgSlug, @NotBlank String email, @NotBlank String password) {
}
