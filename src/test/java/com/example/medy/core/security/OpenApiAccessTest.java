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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiAccessTest {

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
    private User staff;

    @BeforeEach
    void setUp() {
        org = organizationRepository.save(newOrganization("Test Clinic", "test-openapi"));
        staff = userRepository.save(newUser(org.getId(), "staff@test.com", Role.DOCTOR));
    }

    @AfterEach
    void tearDown() {
        userRepository.delete(staff);
        organizationRepository.delete(org);
    }

    @Test
    void apiDocs_withoutToken_isRejected() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiDocs_withAnyAuthenticatedRole_isAccessible() throws Exception {
        String token = login("test-openapi", "staff@test.com");

        mockMvc.perform(get("/v3/api-docs").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
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
        user.setFullName("Test Staff");
        user.setRole(role);
        return user;
    }
}
