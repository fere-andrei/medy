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
import com.example.medy.patient.internal.repository.PatientRepository;
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

/**
 * A SUPER_ADMIN has no home tenant, so /patients is only reachable when it
 * says which tenant to act on via {@code X-Tenant-Id} — see
 * {@code JwtAuthenticationFilter}'s javadoc for why.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SuperAdminTenantAccessTest {

    private static final String PASSWORD = "password123";
    private static final String TENANT_HEADER = "X-Tenant-Id";

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

    private Organization org;
    private User superAdmin;

    @BeforeEach
    void setUp() {
        org = organizationRepository.save(newOrganization("Test Clinic", "test-super-admin"));
        superAdmin = userRepository.save(newSuperAdmin("root@test.com"));
        entitlementRepository.save(newEntitlement(org.getId(), true));
    }

    @AfterEach
    void tearDown() {
        TenantContext.setCurrentTenant(org.getId());
        patientRepository.deleteAll();
        TenantContext.clear();

        entitlementRepository.deleteAll(entitlementRepository.findAll().stream()
                .filter(e -> e.getTenantId().equals(org.getId()))
                .toList());
        userRepository.delete(superAdmin);
        organizationRepository.delete(org);
    }

    @Test
    void withoutTenantHeader_isDenied() throws Exception {
        String token = login();

        mockMvc.perform(get("/patients").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void withTenantHeader_actsOnThatTenant() throws Exception {
        String token = login();

        mockMvc.perform(get("/patients")
                        .header("Authorization", "Bearer " + token)
                        .header(TENANT_HEADER, org.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void withTenantHeaderForTenantLackingEntitlement_isDenied() throws Exception {
        String token = login();

        TenantModuleEntitlement entitlement = entitlementRepository.findAll().stream()
                .filter(e -> e.getTenantId().equals(org.getId()))
                .findFirst().orElseThrow();
        entitlement.setEnabled(false);
        entitlementRepository.save(entitlement);

        mockMvc.perform(get("/patients")
                        .header("Authorization", "Bearer " + token)
                        .header(TENANT_HEADER, org.getId().toString()))
                .andExpect(status().isForbidden());
    }

    private String login() throws Exception {
        String body = """
                {"email":"root@test.com","password":"%s"}
                """.formatted(PASSWORD);

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

    private User newSuperAdmin(String email) {
        User user = new User();
        user.setTenantId(null);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setFullName("Root Admin");
        user.setRole(Role.SUPER_ADMIN);
        return user;
    }
}
