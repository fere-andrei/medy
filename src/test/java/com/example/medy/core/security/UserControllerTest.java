package com.example.medy.core.security;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

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
    private PasswordEncoder passwordEncoder;

    private Organization orgA;
    private User clinicAdmin;
    private User doctor;

    @BeforeEach
    void setUp() {
        orgA = organizationRepository.save(newOrganization("Test Clinic", "test-users-a"));
        clinicAdmin = userRepository.save(newUser(orgA.getId(), "admin@test.com", Role.CLINIC_ADMIN));
        doctor = userRepository.save(newUser(orgA.getId(), "doctor@test.com", Role.DOCTOR));
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll(userRepository.findAll().stream()
                .filter(u -> orgA.getId().equals(u.getTenantId()))
                .toList());
        organizationRepository.delete(orgA);
    }

    @Test
    void clinicAdmin_canRegisterDoctorStaff() throws Exception {
        String token = login("test-users-a", "admin@test.com");

        String body = """
                {"email":"newdoc@test.com","password":"securePass1","fullName":"New Doc","role":"DOCTOR"}
                """;

        mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("DOCTOR"))
                .andExpect(jsonPath("$.email").value("newdoc@test.com"));

        User persisted = userRepository.findByTenantIdAndEmail(orgA.getId(), "newdoc@test.com").orElseThrow();
        assertThat(persisted.getPasswordHash()).isNotEqualTo("securePass1");
        assertThat(passwordEncoder.matches("securePass1", persisted.getPasswordHash())).isTrue();
    }

    @Test
    void clinicAdmin_cannotAssignAdminRole() throws Exception {
        String token = login("test-users-a", "admin@test.com");

        String body = """
                {"email":"peer@test.com","password":"securePass1","fullName":"Peer Admin","role":"CLINIC_ADMIN"}
                """;

        mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateEmailInSameTenant_isRejectedWithConflict() throws Exception {
        String token = login("test-users-a", "admin@test.com");

        String body = """
                {"email":"doctor@test.com","password":"securePass1","fullName":"Duplicate","role":"DOCTOR"}
                """;

        mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void nonAdminStaff_isDeniedAccess() throws Exception {
        String token = login("test-users-a", "doctor@test.com");

        String body = """
                {"email":"another@test.com","password":"securePass1","fullName":"Another","role":"DOCTOR"}
                """;

        mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void noToken_isRejected() throws Exception {
        String body = """
                {"email":"another@test.com","password":"securePass1","fullName":"Another","role":"DOCTOR"}
                """;

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
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
