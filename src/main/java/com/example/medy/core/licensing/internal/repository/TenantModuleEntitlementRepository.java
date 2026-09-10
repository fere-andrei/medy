package com.example.medy.core.licensing.internal.repository;

import com.example.medy.core.licensing.internal.entity.TenantModuleEntitlement;
import com.example.medy.core.licensing.internal.enums.ModuleCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantModuleEntitlementRepository extends JpaRepository<TenantModuleEntitlement, UUID> {

    List<TenantModuleEntitlement> findAllByTenantId(UUID tenantId);

    Optional<TenantModuleEntitlement> findByTenantIdAndModuleCode(UUID tenantId, ModuleCode moduleCode);

    @Query("""
            select case when count(e) > 0 then true else false end
            from TenantModuleEntitlement e
            where e.tenantId = :tenantId
              and e.moduleCode = :moduleCode
              and e.enabled = true
              and (e.validUntil is null or e.validUntil > :now)
            """)
    boolean isModuleEnabled(
            @Param("tenantId") UUID tenantId,
            @Param("moduleCode") ModuleCode moduleCode,
            @Param("now") Instant now);
}
