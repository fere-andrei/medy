package com.example.medy.appointment.internal.mapper;

import com.example.medy.appointment.internal.dto.AppointmentRequestDTO;
import com.example.medy.appointment.internal.dto.AppointmentResponseDTO;
import com.example.medy.appointment.internal.entity.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {

    @Mapping(target = "id", ignore = true)
    Appointment toEntity(AppointmentRequestDTO request);

    AppointmentResponseDTO toResponse(Appointment appointment);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(AppointmentRequestDTO request, @MappingTarget Appointment appointment);
}
