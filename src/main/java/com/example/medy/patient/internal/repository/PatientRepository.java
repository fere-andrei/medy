package com.example.medy.patient.internal.repository;

import com.example.medy.patient.internal.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {
}
