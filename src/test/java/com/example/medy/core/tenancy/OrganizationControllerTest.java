package com.example.medy.core.tenancy;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrganizationControllerTest {

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

    private Organization org;
    private User superAdmin;
    private User clinicAdmin;

    @BeforeEach
    void setUp() {
        org = organizationRepository.save(newOrganization("Test Clinic", "test-org-listing"));
        superAdmin = userRepository.save(newUser(null, "root2@test.com", Role.SUPER_ADMIN));
        clinicAdmin = userRepository.save(newUser(org.getId(), "admin@test.com", Role.CLINIC_ADMIN));
    }

    @AfterEach
    void tearDown() {
        userRepository.delete(superAdmin);
        userRepository.delete(clinicAdmin);
        organizationRepository.delete(org);
    }

    @Test
    void noToken_isRejected() throws Exception {
        mockMvc.perform(get("/organizations"))
                .andExpect(status().isForbidden());
    }

    @Test
    void clinicAdmin_isDenied() throws Exception {
        String token = login("test-org-listing", "admin@test.com");

        mockMvc.perform(get("/organizations").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void superAdmin_seesOrganizations() throws Exception {
        String token = login(null, "root2@test.com");

        mockMvc.perform(get("/organizations").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + org.getId() + "')]").exists());
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
