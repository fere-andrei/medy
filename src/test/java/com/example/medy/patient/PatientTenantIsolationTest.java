package com.example.medy.patient;

import com.example.medy.core.tenancy.TenantContext;
import com.example.medy.core.tenancy.internal.entity.Organization;
import com.example.medy.core.tenancy.internal.repository.OrganizationRepository;
import com.example.medy.patient.internal.entity.Patient;
import com.example.medy.patient.internal.repository.PatientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the tenancy plumbing (TenantContext + TenantIdentifierResolver +
 * TenantScopedEntity) actually isolates data for a real entity, end to end
 * against Postgres.
 */
@SpringBootTest
class PatientTenantIsolationTest {

    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private PatientRepository patientRepository;

    private Organization orgA;
    private Organization orgB;

    @BeforeEach
    void setUp() {
        orgA = organizationRepository.save(newOrganization("Test Clinic A", "test-clinic-a"));
        orgB = organizationRepository.save(newOrganization("Test Clinic B", "test-clinic-b"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.setCurrentTenant(orgA.getId());
        patientRepository.deleteAll();
        TenantContext.setCurrentTenant(orgB.getId());
        patientRepository.deleteAll();

        TenantContext.clear();
        organizationRepository.delete(orgA);
        organizationRepository.delete(orgB);
    }

    @Test
    void eachTenantOnlySeesItsOwnPatients() {
        TenantContext.setCurrentTenant(orgA.getId());
        Patient patientA = patientRepository.save(newPatient("Ana", "Popescu"));

        TenantContext.setCurrentTenant(orgB.getId());
        Patient patientB = patientRepository.save(newPatient("Ion", "Ionescu"));

        TenantContext.setCurrentTenant(orgA.getId());
        List<Patient> visibleToA = patientRepository.findAll();

        TenantContext.setCurrentTenant(orgB.getId());
        List<Patient> visibleToB = patientRepository.findAll();

        assertThat(visibleToA).extracting(Patient::getId).containsExactly(patientA.getId());
        assertThat(visibleToB).extracting(Patient::getId).containsExactly(patientB.getId());
    }

    @Test
    void withNoTenantContext_noPatientsAreVisible() {
        TenantContext.setCurrentTenant(orgA.getId());
        patientRepository.save(newPatient("Ana", "Popescu"));

        TenantContext.clear();

        assertThat(patientRepository.findAll()).isEmpty();
    }

    private Organization newOrganization(String name, String slug) {
        Organization organization = new Organization();
        organization.setName(name);
        organization.setSlug(slug);
        return organization;
    }

    private Patient newPatient(String firstName, String lastName) {
        Patient patient = new Patient();
        patient.setFirstName(firstName);
        patient.setLastName(lastName);
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
        return patient;
    }
}
