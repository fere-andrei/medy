package com.example.medy.patient.internal.service;

import com.example.medy.core.web.ResourceNotFoundException;
import com.example.medy.patient.internal.dto.PatientRequestDTO;
import com.example.medy.patient.internal.dto.PatientResponseDTO;
import com.example.medy.patient.internal.entity.Patient;
import com.example.medy.patient.internal.mapper.PatientMapper;
import com.example.medy.patient.internal.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Boundary that owns the {@code Patient} entity — callers (the controller)
 * only ever see DTOs in and DTOs out.
 * <p>
 * {@code tenant_id} is never set explicitly here — Hibernate's
 * {@code @TenantId} mechanism (TenantScopedEntity) populates it automatically
 * from the tenant carried in the caller's JWT, and filters every query the
 * same way. That also means a lookup for another tenant's patient id simply
 * finds nothing — the same {@link ResourceNotFoundException} as a genuinely
 * missing id, never a hint that the record exists elsewhere.
 * <p>
 * Access control is not handled here — see {@code SecurityConfig}'s
 * URL-pattern rule for {@code /patients/**}.
 */
@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    public PatientService(PatientRepository patientRepository, PatientMapper patientMapper) {
        this.patientRepository = patientRepository;
        this.patientMapper = patientMapper;
    }

    public PatientResponseDTO create(PatientRequestDTO request) {
        Patient patient = patientMapper.toEntity(request);
        return patientMapper.toResponse(patientRepository.save(patient));
    }

    public List<PatientResponseDTO> list() {
        return patientRepository.findAll().stream().map(patientMapper::toResponse).toList();
    }

    public PatientResponseDTO findById(UUID id) {
        return patientMapper.toResponse(findPatientOrThrow(id));
    }

    public PatientResponseDTO update(UUID id, PatientRequestDTO request) {
        Patient patient = findPatientOrThrow(id);
        patientMapper.updateEntityFromRequest(request, patient);
        return patientMapper.toResponse(patientRepository.save(patient));
    }

    public void delete(UUID id) {
        patientRepository.delete(findPatientOrThrow(id));
    }

    private Patient findPatientOrThrow(UUID id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient %s not found".formatted(id)));
    }
}
