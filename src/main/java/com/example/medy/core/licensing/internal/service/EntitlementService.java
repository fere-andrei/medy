package com.example.medy.core.licensing.internal.service;

import com.example.medy.core.licensing.internal.dto.EntitlementRequestDTO;
import com.example.medy.core.licensing.internal.dto.EntitlementResponseDTO;
import com.example.medy.core.licensing.internal.entity.TenantModuleEntitlement;
import com.example.medy.core.licensing.internal.enums.ModuleCode;
import com.example.medy.core.licensing.internal.repository.TenantModuleEntitlementRepository;
import com.example.medy.core.tenancy.internal.repository.OrganizationRepository;
import com.example.medy.core.web.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Lets a SUPER_ADMIN grant or revoke a tenant's access to a licensed module.
 * Deliberately separate from {@link com.example.medy.core.licensing.internal.security.ModuleEntitlementAuthorizationManager}:
 * that class only ever reads an entitlement to gate a request; this one owns
 * writing them.
 */
@Service
public class EntitlementService {

    private final TenantModuleEntitlementRepository entitlementRepository;
    private final OrganizationRepository organizationRepository;

    public EntitlementService(
            TenantModuleEntitlementRepository entitlementRepository, OrganizationRepository organizationRepository) {
        this.entitlementRepository = entitlementRepository;
        this.organizationRepository = organizationRepository;
    }

    public List<EntitlementResponseDTO> list(UUID tenantId) {
        requireTenant(tenantId);

        Map<ModuleCode, TenantModuleEntitlement> byModule = entitlementRepository.findAllByTenantId(tenantId).stream()
                .collect(Collectors.toMap(TenantModuleEntitlement::getModuleCode, Function.identity()));

        return Stream.of(ModuleCode.values())
                .map(moduleCode -> EntitlementResponseDTO.from(moduleCode, byModule.get(moduleCode)))
                .toList();
    }

    @Transactional
    public EntitlementResponseDTO set(UUID tenantId, ModuleCode moduleCode, EntitlementRequestDTO request) {
        requireTenant(tenantId);

        TenantModuleEntitlement entitlement = entitlementRepository
                .findByTenantIdAndModuleCode(tenantId, moduleCode)
                .orElseGet(() -> newEntitlement(tenantId, moduleCode));

        entitlement.setEnabled(request.enabled());
        entitlement.setValidUntil(request.validUntil());

        return EntitlementResponseDTO.from(moduleCode, entitlementRepository.save(entitlement));
    }

    private void requireTenant(UUID tenantId) {
        if (!organizationRepository.existsById(tenantId)) {
            throw new ResourceNotFoundException("Organization " + tenantId + " not found");
        }
    }

    private TenantModuleEntitlement newEntitlement(UUID tenantId, ModuleCode moduleCode) {
        TenantModuleEntitlement entitlement = new TenantModuleEntitlement();
        entitlement.setTenantId(tenantId);
        entitlement.setModuleCode(moduleCode);
        return entitlement;
    }
}
