package com.example.medy.patient.internal.dto;

import com.example.medy.patient.internal.entity.Patient;
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

    public static PatientResponseDTO from(Patient patient) {
        return new PatientResponseDTO(
                patient.getId(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getDateOfBirth(),
                patient.getGender(),
                patient.getNationalId(),
                patient.getEmail(),
                patient.getPhoneNumber(),
                AddressResponseDTO.from(patient.getAddress()),
                patient.getInsuranceProvider(),
                patient.getInsuranceNumber(),
                patient.getEmergencyContactName(),
                patient.getEmergencyContactPhone());
    }
}
