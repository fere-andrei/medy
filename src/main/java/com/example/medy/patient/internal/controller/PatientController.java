package com.example.medy.patient.internal.controller;

import com.example.medy.patient.internal.dto.PatientRequestDTO;
import com.example.medy.patient.internal.dto.PatientResponseDTO;
import com.example.medy.patient.internal.service.PatientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/patients")
class PatientController {

    private final PatientService patientService;

    PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    PatientResponseDTO create(@RequestBody PatientRequestDTO request) {
        return patientService.create(request);
    }

    @GetMapping
    List<PatientResponseDTO> list() {
        return patientService.list();
    }
}
