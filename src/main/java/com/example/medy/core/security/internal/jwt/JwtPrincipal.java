package com.example.medy.core.security.internal.jwt;

import com.example.medy.core.security.internal.enums.Role;

import java.util.UUID;

/** The identity carried by a validated JWT — null tenantId means a SUPER_ADMIN. */
public record JwtPrincipal(UUID userId, UUID tenantId, Role role) {
}
