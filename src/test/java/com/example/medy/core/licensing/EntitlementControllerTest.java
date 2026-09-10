package com.example.medy.core.licensing;

import com.example.medy.core.licensing.internal.entity.TenantModuleEntitlement;
import com.example.medy.core.licensing.internal.enums.ModuleCode;
import com.example.medy.core.licensing.internal.repository.TenantModuleEntitlementRepository;
import com.example.medy.core.security.internal.entity.User;
import com.example.medy.core.security.internal.enums.Role;
import com.example.medy.core.security.internal.repository.UserRepository;
import com.example.medy.core.tenancy.internal.entity.Organization;
import com.example.medy.core.tenancy.internal.repository.OrganizationRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EntitlementControllerTest {

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
    private TenantModuleEntitlementRepository entitlementRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Organization org;
    private User superAdmin;
    private User clinicAdmin;

    @BeforeEach
    void setUp() {
        org = organizationRepository.save(newOrganization("Test Clinic", "test-entitlements"));
        superAdmin = userRepository.save(newUser(null, "root3@test.com", Role.SUPER_ADMIN));
        clinicAdmin = userRepository.save(newUser(org.getId(), "admin3@test.com", Role.CLINIC_ADMIN));
    }

    @AfterEach
    void tearDown() {
        entitlementRepository.deleteAll(entitlementRepository.findAllByTenantId(org.getId()));
        userRepository.delete(superAdmin);
        userRepository.delete(clinicAdmin);
        organizationRepository.delete(org);
    }

    @Test
    void list_reportsEveryModule_evenWithoutAnExistingRow() throws Exception {
        String token = login(null, "root3@test.com");

        mockMvc.perform(get("/organizations/" + org.getId() + "/entitlements")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(ModuleCode.values().length))
                .andExpect(jsonPath("$[?(@.moduleCode == 'PATIENT_MANAGEMENT')].enabled").value(false));
    }

    @Test
    void set_enablesAModule_andPersistsIt() throws Exception {
        String token = login(null, "root3@test.com");

        mockMvc.perform(put("/organizations/" + org.getId() + "/entitlements/PATIENT_MANAGEMENT")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        TenantModuleEntitlement persisted = entitlementRepository
                .findByTenantIdAndModuleCode(org.getId(), ModuleCode.PATIENT_MANAGEMENT).orElseThrow();
        assertThat(persisted.isEnabled()).isTrue();
    }

    @Test
    void set_thenDisable_actuallyBlocksAccessToTheGatedEndpoint() throws Exception {
        String adminToken = login(null, "root3@test.com");
        String staffToken = login("test-entitlements", "admin3@test.com");

        mockMvc.perform(put("/organizations/" + org.getId() + "/entitlements/PATIENT_MANAGEMENT")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":true}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/patients").header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/organizations/" + org.getId() + "/entitlements/PATIENT_MANAGEMENT")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":false}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/patients").header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void clinicAdmin_isDenied() throws Exception {
        String token = login("test-entitlements", "admin3@test.com");

        mockMvc.perform(get("/organizations/" + org.getId() + "/entitlements")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownTenant_returnsNotFound() throws Exception {
        String token = login(null, "root3@test.com");

        mockMvc.perform(get("/organizations/" + UUID.randomUUID() + "/entitlements")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidModuleCode_returnsBadRequest() throws Exception {
        String token = login(null, "root3@test.com");

        mockMvc.perform(put("/organizations/" + org.getId() + "/entitlements/NOT_A_REAL_MODULE")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":true}
                                """))
                .andExpect(status().isBadRequest());
    }

    private String login(String orgSlug, String email) throws Exception {
        String body = orgSlug != null
                ? """
                {"orgSlug":"%s","email":"%s","password":"%s"}
                """.formatted(orgSlug, email, PASSWORD)
                : """
                {"email":"%s","password":"%s"}
                """.formatted(email, PASSWORD);

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

    private User newUser(UUID tenantId, String email, Role role) {
        User user = new User();
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setFullName("Test User");
        user.setRole(role);
        return user;
    }
}
