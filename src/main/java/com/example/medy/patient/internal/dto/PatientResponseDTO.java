package com.example.medy.patient.internal.dto;

import com.example.medy.patient.internal.enums.Gender;

import java.time.LocalDate;
import java.util.UUID;

public record PatientResponseDTO(
        UUID id,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        Gender gender,
        String nationalId,
        String email,
        String phoneNumber,
        AddressResponseDTO address,
        String insuranceProvider,
        String insuranceNumber,
        String emergencyContactName,
        String emergencyContactPhone) {
}
