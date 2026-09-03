package com.example.medy.patient.internal.mapper;

import com.example.medy.patient.internal.dto.AddressRequestDTO;
import com.example.medy.patient.internal.dto.PatientRequestDTO;
import com.example.medy.patient.internal.entity.Address;
import com.example.medy.patient.internal.entity.Patient;
import org.springframework.stereotype.Component;

@Component
public class PatientMapper {

    public Patient toEntity(PatientRequestDTO request) {
        Patient patient = new Patient();
        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setGender(request.gender());
        patient.setNationalId(request.nationalId());
        patient.setEmail(request.email());
        patient.setPhoneNumber(request.phoneNumber());
        patient.setAddress(toAddress(request.address()));
        patient.setInsuranceProvider(request.insuranceProvider());
        patient.setInsuranceNumber(request.insuranceNumber());
        patient.setEmergencyContactName(request.emergencyContactName());
        patient.setEmergencyContactPhone(request.emergencyContactPhone());
        return patient;
    }

    private Address toAddress(AddressRequestDTO request) {
        if (request == null) {
            return null;
        }
        Address address = new Address();
        address.setAddressLine(request.addressLine());
        address.setCity(request.city());
        address.setCounty(request.county());
        address.setPostalCode(request.postalCode());
        address.setCountry(request.country());
        return address;
    }
}
