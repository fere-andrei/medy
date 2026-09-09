package com.example.medy.patient.internal.dto;

import jakarta.validation.constraints.Size;

public record AddressRequestDTO(
        @Size(max = 255) String addressLine,
        @Size(max = 255) String city,
        @Size(max = 255) String county,
        @Size(max = 20) String postalCode,
        @Size(max = 100) String country) {
}
