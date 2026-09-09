package com.example.medy.patient.internal.controller;

import com.example.medy.patient.internal.dto.PatientRequestDTO;
import com.example.medy.patient.internal.dto.PatientResponseDTO;
import com.example.medy.patient.internal.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patients")
class PatientController {

    private final PatientService patientService;

    PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    PatientResponseDTO create(@Valid @RequestBody PatientRequestDTO request) {
        return patientService.create(request);
    }

    @GetMapping
    List<PatientResponseDTO> list() {
        return patientService.list();
    }

    @GetMapping("/{id}")
    PatientResponseDTO findById(@PathVariable UUID id) {
        return patientService.findById(id);
    }

    @PutMapping("/{id}")
    PatientResponseDTO update(@PathVariable UUID id, @Valid @RequestBody PatientRequestDTO request) {
        return patientService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        patientService.delete(id);
    }
}
