package com.example.medy.core.security.internal.dto;

import com.example.medy.core.security.internal.enums.Role;

import java.util.Map;

public record UserSummaryResponseDTO(Map<Role, Long> userSummary) {
}
