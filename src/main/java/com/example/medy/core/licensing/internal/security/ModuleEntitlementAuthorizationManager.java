package com.example.medy.core.licensing.internal.security;

import com.example.medy.core.licensing.internal.enums.ModuleCode;
import com.example.medy.core.licensing.internal.repository.TenantModuleEntitlementRepository;
import com.example.medy.core.tenancy.TenantContext;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Live, per-request check of whether the caller's tenant currently has a
 * given module enabled. Unlike role checks (baked into the JWT at login),
 * this can't be cached in the token — a subscription can be revoked
 * mid-session, so every relevant request re-checks the database.
 */
public class ModuleEntitlementAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final ModuleCode moduleCode;
    private final TenantModuleEntitlementRepository entitlementRepository;

    public ModuleEntitlementAuthorizationManager(
            ModuleCode moduleCode, TenantModuleEntitlementRepository entitlementRepository) {
        this.moduleCode = moduleCode;
        this.entitlementRepository = entitlementRepository;
    }

    @Override
    public AuthorizationResult authorize(Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            return new AuthorizationDecision(false);
        }

        boolean granted = entitlementRepository.isModuleEnabled(tenantId, moduleCode, Instant.now());
        return new AuthorizationDecision(granted);
    }
}
