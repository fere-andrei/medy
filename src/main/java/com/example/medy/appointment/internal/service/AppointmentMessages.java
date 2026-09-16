package com.example.medy.appointment.internal.service;

/** Every user-facing error message thrown by {@link AppointmentService}, in one place. */
final class AppointmentMessages {

    static final String INVALID_TIME_RANGE = "startTime must be before endTime";
    static final String DOCTOR_SCHEDULE_CONFLICT = "Doctor already has an overlapping appointment";
    static final String APPOINTMENT_NOT_FOUND = "Appointment %s not found";

    private AppointmentMessages() {
    }
}
