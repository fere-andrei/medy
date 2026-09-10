package com.example.medy.core.tenancy.internal.service;

import com.example.medy.core.tenancy.internal.dto.OrganizationResponseDTO;
import com.example.medy.core.tenancy.internal.repository.OrganizationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public List<OrganizationResponseDTO> list() {
        return organizationRepository.findAll().stream().map(OrganizationResponseDTO::from).toList();
    }
}
