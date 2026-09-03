package com.example.medy.core.security.internal.repository;

import com.example.medy.core.security.internal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByTenantIdAndEmail(UUID tenantId, String email);

    Optional<User> findByTenantIdIsNullAndEmail(String email);
}
