package com.example.medy.core.tenancy.internal.repository;

import com.example.medy.core.tenancy.internal.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
}
