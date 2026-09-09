package com.example.medy.patient.internal.mapper;

import com.example.medy.patient.internal.dto.AddressRequestDTO;
import com.example.medy.patient.internal.dto.PatientRequestDTO;
import com.example.medy.patient.internal.dto.PatientResponseDTO;
import com.example.medy.patient.internal.entity.Address;
import com.example.medy.patient.internal.entity.Patient;
import com.example.medy.patient.internal.enums.Gender;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PatientMapperTest {

    private final PatientMapper mapper = new PatientMapperImpl();

    @Test
    void toEntity_mapsAddressFields_whenAddressProvided() {
        AddressRequestDTO address = new AddressRequestDTO("Str. Florilor 12", "Cluj-Napoca", "Cluj", "400001", "Romania");
        PatientRequestDTO request = new PatientRequestDTO(
                "Ana", "Popescu", LocalDate.of(1990, 1, 1), Gender.FEMALE,
                null, null, null, address, null, null, null, null);

        Patient patient = mapper.toEntity(request);

        assertThat(patient.getAddress()).isNotNull();
        assertThat(patient.getAddress().getAddressLine()).isEqualTo("Str. Florilor 12");
        assertThat(patient.getAddress().getCity()).isEqualTo("Cluj-Napoca");
        assertThat(patient.getAddress().getCounty()).isEqualTo("Cluj");
        assertThat(patient.getAddress().getPostalCode()).isEqualTo("400001");
        assertThat(patient.getAddress().getCountry()).isEqualTo("Romania");
    }

    @Test
    void toEntity_leavesAddressNull_whenNotProvided() {
        PatientRequestDTO request = new PatientRequestDTO(
                "Ana", "Popescu", LocalDate.of(1990, 1, 1), null,
                null, null, null, null, null, null, null, null);

        Patient patient = mapper.toEntity(request);

        assertThat(patient.getAddress()).isNull();
    }

    @Test
    void toEntity_leavesIdUnset_soPersistenceGeneratesIt() {
        PatientRequestDTO request = new PatientRequestDTO(
                "Ana", "Popescu", LocalDate.of(1990, 1, 1), null,
                null, null, null, null, null, null, null, null);

        Patient patient = mapper.toEntity(request);

        assertThat(patient.getId()).isNull();
    }

    @Test
    void toResponse_mapsAddressFields_whenPatientHasAddress() {
        Patient patient = new Patient();
        patient.setFirstName("Ana");
        Address address = new Address();
        address.setCity("Cluj-Napoca");
        address.setCountry("Romania");
        patient.setAddress(address);

        PatientResponseDTO response = mapper.toResponse(patient);

        assertThat(response.address()).isNotNull();
        assertThat(response.address().city()).isEqualTo("Cluj-Napoca");
        assertThat(response.address().country()).isEqualTo("Romania");
    }

    @Test
    void toResponse_leavesAddressNull_whenPatientHasNoAddress() {
        Patient patient = new Patient();
        patient.setFirstName("Ana");

        PatientResponseDTO response = mapper.toResponse(patient);

        assertThat(response.address()).isNull();
    }
}
