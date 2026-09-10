package com.example.medy.core.licensing.internal.controller;

import com.example.medy.core.licensing.internal.dto.EntitlementRequestDTO;
import com.example.medy.core.licensing.internal.dto.EntitlementResponseDTO;
import com.example.medy.core.licensing.internal.enums.ModuleCode;
import com.example.medy.core.licensing.internal.service.EntitlementService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/organizations/{tenantId}/entitlements")
class EntitlementController {

    private final EntitlementService entitlementService;

    EntitlementController(EntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    @GetMapping
    List<EntitlementResponseDTO> list(@PathVariable UUID tenantId) {
        return entitlementService.list(tenantId);
    }

    @PutMapping("/{moduleCode}")
    EntitlementResponseDTO set(
            @PathVariable UUID tenantId,
            @PathVariable ModuleCode moduleCode,
            @Valid @RequestBody EntitlementRequestDTO request) {
        return entitlementService.set(tenantId, moduleCode, request);
    }
}
