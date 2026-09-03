package com.example.medy.core.security.internal.dto;

import com.example.medy.core.security.internal.entity.User;
import com.example.medy.core.security.internal.enums.Role;

import java.util.UUID;

/** Deliberately excludes {@code passwordHash} — never serialize that. */
public record UserResponseDTO(UUID id, String email, String fullName, Role role, UUID tenantId) {

    public static UserResponseDTO from(User user) {
        return new UserResponseDTO(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getTenantId());
    }
}
