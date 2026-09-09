package com.example.medy.core.tenancy.internal.hibernate;

import com.example.medy.core.tenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantIdentifierResolverTest {

    private final TenantIdentifierResolver resolver = new TenantIdentifierResolver();

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void resolvesToCurrentTenant_whenOneIsSet() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);

        assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo(tenantId);
    }

    @Test
    void resolvesToSentinel_neverNull_whenNoTenantIsSet() {
        TenantContext.clear();

        UUID resolved = resolver.resolveCurrentTenantIdentifier();

        // Must never be null (Hibernate would fail using it as a query
        // parameter) and must never coincide with a real tenant id — this is
        // the fail-closed behavior @TenantId queries rely on when no tenant
        // context exists.
        assertThat(resolved).isNotNull().isEqualTo(new UUID(0, 0));
    }

    @Test
    void validateExistingCurrentSessions_isEnabled() {
        assertThat(resolver.validateExistingCurrentSessions()).isTrue();
    }
}
