package com.example.medy.core.tenancy;

import java.util.UUID;

/**
 * Holds the tenant id for the thread handling the current request.
 * Populated by a servlet filter early in the request lifecycle; must be
 * cleared once the request finishes since Tomcat reuses threads across
 * requests via a pool.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setCurrentTenant(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
