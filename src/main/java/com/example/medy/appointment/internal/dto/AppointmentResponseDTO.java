package com.example.medy.appointment.internal.dto;

import com.example.medy.appointment.internal.enums.AppointmentStatus;

import java.time.Instant;
import java.util.UUID;

public record AppointmentResponseDTO(
        UUID id,
        UUID patientId,
        UUID doctorId,
        Instant startTime,
        Instant endTime,
        AppointmentStatus status,
        String notes) {
}
