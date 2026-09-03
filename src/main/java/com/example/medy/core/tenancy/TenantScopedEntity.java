package com.example.medy.core.tenancy;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

/**
 * Base class for entities owned by a single tenant. Hibernate populates
 * {@code tenantId} automatically from the current tenant resolver on
 * insert, and adds {@code WHERE tenant_id = ?} to every query for the
 * subclass — no manual filtering needed anywhere above this class.
 */
@MappedSuperclass
public abstract class TenantScopedEntity {

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    protected UUID getTenantId() {
        return tenantId;
    }
}
