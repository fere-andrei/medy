package com.example.medy.appointment.internal.mapper;

import com.example.medy.appointment.internal.dto.AppointmentRequestDTO;
import com.example.medy.appointment.internal.dto.AppointmentResponseDTO;
import com.example.medy.appointment.internal.entity.Appointment;
import com.example.medy.appointment.internal.enums.AppointmentStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentMapperTest {

    private final AppointmentMapper mapper = new AppointmentMapperImpl();

    @Test
    void toEntity_mapsAllFields_andLeavesIdUnset() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Instant start = Instant.now();
        Instant end = start.plusSeconds(1800);

        AppointmentRequestDTO request =
                new AppointmentRequestDTO(patientId, doctorId, start, end, AppointmentStatus.REQUESTED, "First visit");

        Appointment appointment = mapper.toEntity(request);

        assertThat(appointment.getId()).isNull();
        assertThat(appointment.getPatientId()).isEqualTo(patientId);
        assertThat(appointment.getDoctorId()).isEqualTo(doctorId);
        assertThat(appointment.getStartTime()).isEqualTo(start);
        assertThat(appointment.getEndTime()).isEqualTo(end);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.REQUESTED);
        assertThat(appointment.getNotes()).isEqualTo("First visit");
    }

    @Test
    void toResponse_mapsAllFields() {
        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setPatientId(UUID.randomUUID());
        appointment.setDoctorId(UUID.randomUUID());
        appointment.setStartTime(Instant.now());
        appointment.setEndTime(Instant.now().plusSeconds(900));
        appointment.setStatus(AppointmentStatus.CONFIRMED);

        AppointmentResponseDTO response = mapper.toResponse(appointment);

        assertThat(response.id()).isEqualTo(appointment.getId());
        assertThat(response.patientId()).isEqualTo(appointment.getPatientId());
        assertThat(response.status()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    void updateEntityFromRequest_overwritesFieldsButNeverTheId() {
        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setStatus(AppointmentStatus.REQUESTED);
        UUID originalId = appointment.getId();

        AppointmentRequestDTO request = new AppointmentRequestDTO(
                UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(600),
                AppointmentStatus.CONFIRMED, null);

        mapper.updateEntityFromRequest(request, appointment);

        assertThat(appointment.getId()).isEqualTo(originalId);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }
}
