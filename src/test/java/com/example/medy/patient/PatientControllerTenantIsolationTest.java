package com.example.medy.patient;

import com.example.medy.core.licensing.internal.entity.TenantModuleEntitlement;
import com.example.medy.core.licensing.internal.enums.ModuleCode;
import com.example.medy.core.licensing.internal.repository.TenantModuleEntitlementRepository;
import com.example.medy.core.security.internal.entity.User;
import com.example.medy.core.security.internal.enums.Role;
import com.example.medy.core.security.internal.repository.UserRepository;
import com.example.medy.core.tenancy.TenantContext;
import com.example.medy.core.tenancy.internal.entity.Organization;
import com.example.medy.core.tenancy.internal.repository.OrganizationRepository;
import com.example.medy.patient.internal.dto.PatientRequestDTO;
import com.example.medy.patient.internal.repository.PatientRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PatientControllerTenantIsolationTest {

    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TenantModuleEntitlementRepository entitlementRepository;

    private Organization orgA;
    private Organization orgB;
    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        orgA = organizationRepository.save(newOrganization("Test Clinic A", "test-controller-a"));
        orgB = organizationRepository.save(newOrganization("Test Clinic B", "test-controller-b"));

        userA = userRepository.save(newUser(orgA.getId(), "staffA@test.com"));
        userB = userRepository.save(newUser(orgB.getId(), "staffB@test.com"));

        entitlementRepository.save(newEntitlement(orgA.getId(), true));
        entitlementRepository.save(newEntitlement(orgB.getId(), true));
    }

    @AfterEach
    void tearDown() {
        TenantContext.setCurrentTenant(orgA.getId());
        patientRepository.deleteAll();
        TenantContext.setCurrentTenant(orgB.getId());
        patientRepository.deleteAll();
        TenantContext.clear();

        entitlementRepository.deleteAll(entitlementRepository.findAll().stream()
                .filter(e -> e.getTenantId().equals(orgA.getId()) || e.getTenantId().equals(orgB.getId()))
                .toList());
        userRepository.delete(userA);
        userRepository.delete(userB);
        organizationRepository.delete(orgA);
        organizationRepository.delete(orgB);
    }

    @Test
    void noToken_isRejected() throws Exception {
        mockMvc.perform(get("/patients"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deniesAccess_whenTenantLacksThePatientManagementEntitlement() throws Exception {
        String tokenA = login("test-controller-a", "staffA@test.com");

        TenantModuleEntitlement entitlement = entitlementRepository
                .findAll().stream()
                .filter(e -> e.getTenantId().equals(orgA.getId()))
                .findFirst().orElseThrow();
        entitlement.setEnabled(false);
        entitlementRepository.save(entitlement);

        mockMvc.perform(get("/patients").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    void deniesAccess_whenEntitlementHasExpired() throws Exception {
        String tokenA = login("test-controller-a", "staffA@test.com");

        TenantModuleEntitlement entitlement = entitlementRepository
                .findAll().stream()
                .filter(e -> e.getTenantId().equals(orgA.getId()))
                .findFirst().orElseThrow();
        entitlement.setValidUntil(java.time.Instant.now().minusSeconds(60));
        entitlementRepository.save(entitlement);

        mockMvc.perform(get("/patients").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffOnlySeesPatientsCreatedUnderTheirOwnClinic() throws Exception {
        String tokenA = login("test-controller-a", "staffA@test.com");
        String tokenB = login("test-controller-b", "staffB@test.com");

        PatientRequestDTO newPatient = new PatientRequestDTO(
                "Ana", "Popescu", LocalDate.of(1990, 1, 1),
                null, null, null, null, null, null, null, null, null);

        mockMvc.perform(post("/patients")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPatient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ana"));

        mockMvc.perform(get("/patients").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Ana"));

        mockMvc.perform(get("/patients").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void findById_returnsPatient_whenItBelongsToCallersTenant() throws Exception {
        String tokenA = login("test-controller-a", "staffA@test.com");
        UUID patientId = createPatient(tokenA, "Ana", "Popescu");

        mockMvc.perform(get("/patients/" + patientId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ana"));
    }

    @Test
    void findById_returnsNotFound_whenPatientDoesNotExist() throws Exception {
        String tokenA = login("test-controller-a", "staffA@test.com");

        mockMvc.perform(get("/patients/" + UUID.randomUUID()).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_returnsNotFound_whenPatientBelongsToAnotherTenant() throws Exception {
        String tokenA = login("test-controller-a", "staffA@test.com");
        String tokenB = login("test-controller-b", "staffB@test.com");
        UUID patientId = createPatient(tokenA, "Ana", "Popescu");

        mockMvc.perform(get("/patients/" + patientId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_changesPatientFields() throws Exception {
        String tokenA = login("test-controller-a", "staffA@test.com");
        UUID patientId = createPatient(tokenA, "Ana", "Popescu");

        PatientRequestDTO updated = new PatientRequestDTO(
                "Ana-Maria", "Popescu", LocalDate.of(1990, 1, 1),
                null, null, null, null, null, null, null, null, null);

        mockMvc.perform(put("/patients/" + patientId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ana-Maria"));
    }

    @Test
    void update_returnsNotFound_whenPatientBelongsToAnotherTenant() throws Exception {
        String tokenA = login("test-controller-a", "staffA@test.com");
        String tokenB = login("test-controller-b", "staffB@test.com");
        UUID patientId = createPatient(tokenA, "Ana", "Popescu");

        PatientRequestDTO updated = new PatientRequestDTO(
                "Hacked", "Name", LocalDate.of(1990, 1, 1),
                null, null, null, null, null, null, null, null, null);

        mockMvc.perform(put("/patients/" + patientId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_removesPatient_andSubsequentFindByIdReturnsNotFound() throws Exception {
        String tokenA = login("test-controller-a", "staffA@test.com");
        UUID patientId = createPatient(tokenA, "Ana", "Popescu");

        mockMvc.perform(delete("/patients/" + patientId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/patients/" + patientId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returnsNotFound_whenPatientBelongsToAnotherTenant() throws Exception {
        String tokenA = login("test-controller-a", "staffA@test.com");
        String tokenB = login("test-controller-b", "staffB@test.com");
        UUID patientId = createPatient(tokenA, "Ana", "Popescu");

        mockMvc.perform(delete("/patients/" + patientId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    private UUID createPatient(String token, String firstName, String lastName) throws Exception {
        PatientRequestDTO newPatient = new PatientRequestDTO(
                firstName, lastName, LocalDate.of(1990, 1, 1),
                null, null, null, null, null, null, null, null, null);

        String response = mockMvc.perform(post("/patients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPatient)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return UUID.fromString(objectMapper.readTree(response).get("id").asString());
    }

    private String login(String orgSlug, String email) throws Exception {
        String body = """
                {"orgSlug":"%s","email":"%s","password":"%s"}
                """.formatted(orgSlug, email, PASSWORD);

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asString();
    }

    private Organization newOrganization(String name, String slug) {
        Organization organization = new Organization();
        organization.setName(name);
        organization.setSlug(slug);
        return organization;
    }

    private TenantModuleEntitlement newEntitlement(UUID tenantId, boolean enabled) {
        TenantModuleEntitlement entitlement = new TenantModuleEntitlement();
        entitlement.setTenantId(tenantId);
        entitlement.setModuleCode(ModuleCode.PATIENT_MANAGEMENT);
        entitlement.setEnabled(enabled);
        return entitlement;
    }

    private User newUser(java.util.UUID tenantId, String email) {
        User user = new User();
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setFullName("Test Staff");
        user.setRole(Role.CLINIC_ADMIN);
        return user;
    }
}
