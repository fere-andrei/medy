package com.example.medy.core.security.internal.repository;

import com.example.medy.core.security.internal.entity.User;
import com.example.medy.core.security.internal.repository.projection.RoleCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByTenantIdAndEmail(UUID tenantId, String email);

    Optional<User> findByTenantIdIsNullAndEmail(String email);

    List<User> findAllByTenantId(UUID tenantId);

    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("SELECT u.role AS role, COUNT(u) AS count FROM User u WHERE u.tenantId = :tenantId GROUP BY u.role")
    List<RoleCount> countByTenantIdGroupedByRole(@Param("tenantId") UUID tenantId);
}
