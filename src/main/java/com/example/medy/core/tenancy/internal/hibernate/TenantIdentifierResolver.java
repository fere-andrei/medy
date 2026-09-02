package com.example.medy.core.tenancy.internal.hibernate;

import com.example.medy.core.tenancy.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Tells Hibernate which tenant to filter {@code @TenantId} entities by, for
 * every query and insert on the current session. Delegates to
 * {@link TenantContext}, which the {@code TenantResolutionFilter} populates
 * per request.
 * <p>
 * When no tenant is set (e.g. a session opened outside of a tenant-scoped
 * request), a sentinel id that matches no real tenant is returned rather than
 * throwing — tenant-scoped queries then simply return zero rows instead of
 * failing, while entities that aren't tenant-scoped (like {@code Organization}
 * itself) are unaffected either way.
 */
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<UUID> {

    private static final UUID NO_TENANT = new UUID(0, 0);

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        UUID tenantId = TenantContext.getCurrentTenant();
        return tenantId != null ? tenantId : NO_TENANT;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
