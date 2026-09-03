package com.example.medy.patient.internal.service;

import com.example.medy.patient.internal.dto.PatientRequestDTO;
import com.example.medy.patient.internal.dto.PatientResponseDTO;
import com.example.medy.patient.internal.entity.Patient;
import com.example.medy.patient.internal.mapper.PatientMapper;
import com.example.medy.patient.internal.repository.PatientRepository;
import com.example.medy.patient.internal.security.RequirePatientStaffAccess;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Boundary that owns the {@code Patient} entity — callers (the controller)
 * only ever see DTOs in and DTOs out.
 * <p>
 * {@code tenant_id} is never set explicitly here — Hibernate's
 * {@code @TenantId} mechanism (TenantScopedEntity) populates it automatically
 * from the tenant carried in the caller's JWT, and filters every query the
 * same way.
 * <p>
 * The access-control annotation lives here, not on the controller — it
 * protects the business operation itself, so it still applies to any future
 * caller that isn't HTTP, not just requests that happen to come through
 * {@code PatientController}.
 */
@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    public PatientService(PatientRepository patientRepository, PatientMapper patientMapper) {
        this.patientRepository = patientRepository;
        this.patientMapper = patientMapper;
    }

    @RequirePatientStaffAccess
    public PatientResponseDTO create(PatientRequestDTO request) {
        Patient patient = patientMapper.toEntity(request);
        return PatientResponseDTO.from(patientRepository.save(patient));
    }

    @RequirePatientStaffAccess
    public List<PatientResponseDTO> list() {
        return patientRepository.findAll().stream().map(PatientResponseDTO::from).toList();
    }
}
