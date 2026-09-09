package com.example.medy.core.licensing.internal.security;

import com.example.medy.core.licensing.internal.enums.ModuleCode;
import com.example.medy.core.licensing.internal.repository.TenantModuleEntitlementRepository;
import com.example.medy.core.tenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDecision;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModuleEntitlementAuthorizationManagerTest {

    @Mock
    private TenantModuleEntitlementRepository entitlementRepository;

    private ModuleEntitlementAuthorizationManager manager;

    @BeforeEach
    void setUp() {
        manager = new ModuleEntitlementAuthorizationManager(ModuleCode.PATIENT_MANAGEMENT, entitlementRepository);
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void grants_whenTenantHasTheModuleEnabled() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);
        when(entitlementRepository.isModuleEnabled(eq(tenantId), eq(ModuleCode.PATIENT_MANAGEMENT), any(Instant.class)))
                .thenReturn(true);

        AuthorizationDecision decision = (AuthorizationDecision) manager.authorize(() -> null, null);

        assertThat(decision.isGranted()).isTrue();
    }

    @Test
    void denies_whenTenantDoesNotHaveTheModuleEnabled() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);
        when(entitlementRepository.isModuleEnabled(eq(tenantId), eq(ModuleCode.PATIENT_MANAGEMENT), any(Instant.class)))
                .thenReturn(false);

        AuthorizationDecision decision = (AuthorizationDecision) manager.authorize(() -> null, null);

        assertThat(decision.isGranted()).isFalse();
    }

    @Test
    void denies_withoutQueryingTheDatabase_whenNoTenantContextIsSet() {
        TenantContext.clear();

        AuthorizationDecision decision = (AuthorizationDecision) manager.authorize(() -> null, null);

        assertThat(decision.isGranted()).isFalse();
        verifyNoInteractions(entitlementRepository);
    }
}
