package com.example.medy.appointment.internal.controller;

import com.example.medy.appointment.internal.dto.AppointmentRequestDTO;
import com.example.medy.appointment.internal.dto.AppointmentResponseDTO;
import com.example.medy.appointment.internal.service.AppointmentService;
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
@RequestMapping("/appointments")
class AppointmentController {

    private final AppointmentService appointmentService;

    AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    AppointmentResponseDTO create(@Valid @RequestBody AppointmentRequestDTO request) {
        return appointmentService.create(request);
    }

    @GetMapping
    List<AppointmentResponseDTO> list() {
        return appointmentService.list();
    }

    @GetMapping("/{id}")
    AppointmentResponseDTO findById(@PathVariable UUID id) {
        return appointmentService.findById(id);
    }

    @PutMapping("/{id}")
    AppointmentResponseDTO update(@PathVariable UUID id, @Valid @RequestBody AppointmentRequestDTO request) {
        return appointmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        appointmentService.delete(id);
    }
}
