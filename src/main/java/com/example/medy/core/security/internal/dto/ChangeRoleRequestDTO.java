package com.example.medy.core.security.internal.dto;

import com.example.medy.core.security.internal.enums.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequestDTO(@NotNull Role role) {
}
