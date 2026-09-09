package com.example.medy.patient.internal.dto;

import com.example.medy.patient.internal.enums.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PatientRequestDTO(
        @NotBlank @Size(max = 255) String firstName,
        @NotBlank @Size(max = 255) String lastName,
        @NotNull @Past LocalDate dateOfBirth,
        Gender gender,
        @Size(max = 20) String nationalId,
        @Email @Size(max = 255) String email,
        @Size(max = 30) String phoneNumber,
        @Valid AddressRequestDTO address,
        @Size(max = 255) String insuranceProvider,
        @Size(max = 100) String insuranceNumber,
        @Size(max = 255) String emergencyContactName,
        @Size(max = 30) String emergencyContactPhone) {
}
