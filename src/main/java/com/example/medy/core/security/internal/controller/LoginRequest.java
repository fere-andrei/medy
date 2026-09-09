package com.example.medy.core.security.internal.controller;

import jakarta.validation.constraints.NotBlank;

/** {@code orgSlug} is omitted for a SUPER_ADMIN login. */
public record LoginRequest(String orgSlug, @NotBlank String email, @NotBlank String password) {
}
