package com.example.medy.appointment.internal.dto;

import com.example.medy.appointment.internal.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record AppointmentRequestDTO(
        @NotNull UUID patientId,
        @NotNull UUID doctorId,
        @NotNull Instant startTime,
        @NotNull Instant endTime,
        @NotNull AppointmentStatus status,
        @Size(max = 1000) String notes) {
}
