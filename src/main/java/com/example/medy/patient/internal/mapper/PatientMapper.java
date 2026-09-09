package com.example.medy.patient.internal.mapper;

import com.example.medy.patient.internal.dto.AddressRequestDTO;
import com.example.medy.patient.internal.dto.AddressResponseDTO;
import com.example.medy.patient.internal.dto.PatientRequestDTO;
import com.example.medy.patient.internal.dto.PatientResponseDTO;
import com.example.medy.patient.internal.entity.Address;
import com.example.medy.patient.internal.entity.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PatientMapper {

    @Mapping(target = "id", ignore = true)
    Patient toEntity(PatientRequestDTO request);

    Address toEntity(AddressRequestDTO request);

    PatientResponseDTO toResponse(Patient patient);

    AddressResponseDTO toResponse(Address address);

    /** Updates an existing entity in place — {@code id} is never touched. */
    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(PatientRequestDTO request, @MappingTarget Patient patient);
}
