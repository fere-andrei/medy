package com.example.medy.appointment.internal.service;

import com.example.medy.appointment.internal.dto.AppointmentRequestDTO;
import com.example.medy.appointment.internal.dto.AppointmentResponseDTO;
import com.example.medy.appointment.internal.entity.Appointment;
import com.example.medy.appointment.internal.enums.AppointmentStatus;
import com.example.medy.appointment.internal.mapper.AppointmentMapper;
import com.example.medy.appointment.internal.repository.AppointmentRepository;
import com.example.medy.core.web.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Boundary that owns the {@code Appointment} entity — callers (the
 * controller) only ever see DTOs in and DTOs out.
 * <p>
 * {@code patientId}/{@code doctorId} aren't validated against the
 * {@code patient}/{@code core.security} modules here — deliberately deferred,
 * see {@link Appointment}'s javadoc.
 */
@Service
public class AppointmentService {

    /** Statuses that actually occupy the doctor's schedule. */
    private static final Set<AppointmentStatus> ACTIVE_STATUSES =
            EnumSet.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN, AppointmentStatus.IN_TREATMENT);

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;

    public AppointmentService(AppointmentRepository appointmentRepository, AppointmentMapper appointmentMapper) {
        this.appointmentRepository = appointmentRepository;
        this.appointmentMapper = appointmentMapper;
    }

    public AppointmentResponseDTO create(AppointmentRequestDTO request) {
        validateTimeRange(request);
        checkForConflict(request, null);

        Appointment appointment = appointmentMapper.toEntity(request);
        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    public List<AppointmentResponseDTO> list() {
        return appointmentRepository.findAll().stream().map(appointmentMapper::toResponse).toList();
    }

    public AppointmentResponseDTO findById(UUID id) {
        return appointmentMapper.toResponse(findAppointmentOrThrow(id));
    }

    public AppointmentResponseDTO update(UUID id, AppointmentRequestDTO request) {
        validateTimeRange(request);
        Appointment appointment = findAppointmentOrThrow(id);
        checkForConflict(request, id);

        appointmentMapper.updateEntityFromRequest(request, appointment);
        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    public void delete(UUID id) {
        appointmentRepository.delete(findAppointmentOrThrow(id));
    }

    private void validateTimeRange(AppointmentRequestDTO request) {
        if (!request.startTime().isBefore(request.endTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startTime must be before endTime");
        }
    }

    private void checkForConflict(AppointmentRequestDTO request, UUID excludeAppointmentId) {
        if (!ACTIVE_STATUSES.contains(request.status())) {
            return;
        }
        boolean conflict = appointmentRepository.hasConflict(
                request.doctorId(), request.startTime(), request.endTime(), ACTIVE_STATUSES, excludeAppointmentId);
        if (conflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Doctor already has an overlapping appointment");
        }
    }

    private Appointment findAppointmentOrThrow(UUID id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment %s not found".formatted(id)));
    }
}
