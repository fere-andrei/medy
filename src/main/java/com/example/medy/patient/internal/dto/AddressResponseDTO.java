package com.example.medy.patient.internal.dto;

import com.example.medy.patient.internal.entity.Address;

public record AddressResponseDTO(String addressLine, String city, String county, String postalCode, String country) {

    public static AddressResponseDTO from(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressResponseDTO(
                address.getAddressLine(), address.getCity(), address.getCounty(),
                address.getPostalCode(), address.getCountry());
    }
}
