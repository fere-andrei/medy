package com.example.medy.core.tenancy.internal.controller;

import com.example.medy.core.tenancy.internal.dto.OrganizationResponseDTO;
import com.example.medy.core.tenancy.internal.service.OrganizationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/organizations")
class OrganizationController {

    private final OrganizationService organizationService;

    OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    List<OrganizationResponseDTO> list() {
        return organizationService.list();
    }
}
