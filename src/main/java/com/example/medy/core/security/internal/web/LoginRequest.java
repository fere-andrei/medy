package com.example.medy.core.security.internal.web;

/** {@code orgSlug} is omitted for a SUPER_ADMIN login. */
public record LoginRequest(String orgSlug, String email, String password) {
}
