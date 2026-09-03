package com.example.medy.patient.internal.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The one place that defines which roles may touch patient records. Callers
 * (service, controller, anywhere else) only ever see this name — never the
 * raw role list.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAnyRole('CLINIC_ADMIN','DOCTOR','RECEPTIONIST','ASSISTANT')")
public @interface RequirePatientStaffAccess {
}
