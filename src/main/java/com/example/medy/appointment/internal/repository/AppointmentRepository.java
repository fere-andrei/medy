package com.example.medy.appointment.internal.repository;

import com.example.medy.appointment.internal.entity.Appointment;
import com.example.medy.appointment.internal.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    /**
     * True if the doctor already has an overlapping appointment in one of
     * the given (active) statuses. {@code excludeId} lets an update check
     * for conflicts against every *other* appointment without flagging
     * itself — pass {@code null} for a new appointment.
     */
    @Query("""
            select case when count(a) > 0 then true else false end
            from Appointment a
            where a.doctorId = :doctorId
              and a.status in :activeStatuses
              and a.startTime < :endTime
              and :startTime < a.endTime
              and (:excludeId is null or a.id <> :excludeId)
            """)
    boolean hasConflict(
            @Param("doctorId") UUID doctorId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("activeStatuses") Collection<AppointmentStatus> activeStatuses,
            @Param("excludeId") UUID excludeId);
}
