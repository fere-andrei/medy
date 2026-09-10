package com.example.medy.core.licensing.internal.dto;

import com.example.medy.core.licensing.internal.entity.TenantModuleEntitlement;
import com.example.medy.core.licensing.internal.enums.ModuleCode;

import java.time.Instant;

/**
 * {@code entitlement} is null when the tenant has no row for this module yet — reported as disabled, no expiry.
 */
public record EntitlementResponseDTO(ModuleCode moduleCode, boolean enabled, Instant validUntil) {

    public static EntitlementResponseDTO from(ModuleCode moduleCode, TenantModuleEntitlement entitlement) {
        if (entitlement == null) {
            return new EntitlementResponseDTO(moduleCode, false, null);
        }
        return new EntitlementResponseDTO(moduleCode, entitlement.isEnabled(), entitlement.getValidUntil());
    }
}
