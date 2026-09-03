package com.example.medy.core.tenancy.internal.dto;

import com.example.medy.core.tenancy.internal.entity.Organization;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponseDTO(UUID id, String name, String slug, Instant createdAt) {

    public static OrganizationResponseDTO from(Organization organization) {
        return new OrganizationResponseDTO(
                organization.getId(), organization.getName(), organization.getSlug(), organization.getCreatedAt());
    }
}
