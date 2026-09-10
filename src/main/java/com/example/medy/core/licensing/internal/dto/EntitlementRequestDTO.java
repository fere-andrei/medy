package com.example.medy.core.licensing.internal.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * {@code validUntil} is optional — omit it (or send null) for an entitlement that never expires.
 */
public record EntitlementRequestDTO(@NotNull Boolean enabled, Instant validUntil) {
}
