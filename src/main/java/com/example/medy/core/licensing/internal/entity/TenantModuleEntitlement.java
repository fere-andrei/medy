package com.example.medy.core.licensing.internal.entity;

import com.example.medy.core.licensing.internal.enums.ModuleCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Deliberately does NOT extend {@code TenantScopedEntity} — entitlement
 * checks look up a specific (tenantId, moduleCode) pair explicitly rather
 * than relying on the automatic "current tenant" filter, since a super admin
 * managing entitlements needs to read/write across tenants.
 */
@Entity
@Table(name = "tenant_module_entitlements")
@Getter
@Setter
@NoArgsConstructor
public class TenantModuleEntitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "module_code", nullable = false)
    private ModuleCode moduleCode;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    /** Null means "no expiry". */
    @Column(name = "valid_until")
    private Instant validUntil;
}
