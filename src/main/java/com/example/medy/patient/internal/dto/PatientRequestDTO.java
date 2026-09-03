package com.example.medy.patient.internal.dto;

import com.example.medy.patient.internal.enums.Gender;

import java.time.LocalDate;

public record PatientRequestDTO(
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        Gender gender,
        String nationalId,
        String email,
        String phoneNumber,
        AddressRequestDTO address,
        String insuranceProvider,
        String insuranceNumber,
        String emergencyContactName,
        String emergencyContactPhone) {
}
