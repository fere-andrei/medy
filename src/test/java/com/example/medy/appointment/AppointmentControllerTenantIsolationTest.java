package com.example.medy.appointment;

import com.example.medy.appointment.internal.dto.AppointmentRequestDTO;
import com.example.medy.appointment.internal.enums.AppointmentStatus;
import com.example.medy.appointment.internal.repository.AppointmentRepository;
import com.example.medy.core.licensing.internal.entity.TenantModuleEntitlement;
import com.example.medy.core.licensing.internal.enums.ModuleCode;
import com.example.medy.core.licensing.internal.repository.TenantModuleEntitlementRepository;
import com.example.medy.core.security.internal.entity.User;
import com.example.medy.core.security.internal.enums.Role;
import com.example.medy.core.security.internal.repository.UserRepository;
import com.example.medy.core.tenancy.TenantContext;
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

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AppointmentControllerTenantIsolationTest {

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
    private AppointmentRepository appointmentRepository;
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
        orgA = organizationRepository.save(newOrganization("Test Clinic A", "test-appt-a"));
        orgB = organizationRepository.save(newOrganization("Test Clinic B", "test-appt-b"));

        userA = userRepository.save(newUser(orgA.getId(), "staffA@test.com"));
        userB = userRepository.save(newUser(orgB.getId(), "staffB@test.com"));

        entitlementRepository.save(newEntitlement(orgA.getId()));
        entitlementRepository.save(newEntitlement(orgB.getId()));
    }

    @AfterEach
    void tearDown() {
        TenantContext.setCurrentTenant(orgA.getId());
        appointmentRepository.deleteAll();
        TenantContext.setCurrentTenant(orgB.getId());
        appointmentRepository.deleteAll();
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
        mockMvc.perform(get("/appointments"))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffOnlySeesAppointmentsCreatedUnderTheirOwnClinic() throws Exception {
        String tokenA = login("test-appt-a", "staffA@test.com");
        String tokenB = login("test-appt-b", "staffB@test.com");

        createAppointment(tokenA, UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(1800));

        mockMvc.perform(get("/appointments").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/appointments").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void overlappingAppointmentForSameDoctor_isRejectedWithConflict() throws Exception {
        String tokenA = login("test-appt-a", "staffA@test.com");
        UUID doctorId = UUID.randomUUID();
        Instant start = Instant.now();
        Instant end = start.plusSeconds(1800);

        createAppointment(tokenA, doctorId, start, end);

        AppointmentRequestDTO overlapping = new AppointmentRequestDTO(
                UUID.randomUUID(), doctorId, start.plusSeconds(600), end.plusSeconds(600),
                AppointmentStatus.CONFIRMED, null);

        mockMvc.perform(post("/appointments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overlapping)))
                .andExpect(status().isConflict());
    }

    @Test
    void nonOverlappingAppointmentForSameDoctor_isAccepted() throws Exception {
        String tokenA = login("test-appt-a", "staffA@test.com");
        UUID doctorId = UUID.randomUUID();
        Instant start = Instant.now();
        Instant end = start.plusSeconds(1800);

        createAppointment(tokenA, doctorId, start, end);

        AppointmentRequestDTO nonOverlapping = new AppointmentRequestDTO(
                UUID.randomUUID(), doctorId, end.plusSeconds(60), end.plusSeconds(1860),
                AppointmentStatus.CONFIRMED, null);

        mockMvc.perform(post("/appointments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonOverlapping)))
                .andExpect(status().isOk());
    }

    @Test
    void overlappingAppointmentForDifferentDoctor_isAccepted() throws Exception {
        String tokenA = login("test-appt-a", "staffA@test.com");
        Instant start = Instant.now();
        Instant end = start.plusSeconds(1800);

        createAppointment(tokenA, UUID.randomUUID(), start, end);

        AppointmentRequestDTO sameTimeDifferentDoctor = new AppointmentRequestDTO(
                UUID.randomUUID(), UUID.randomUUID(), start, end, AppointmentStatus.CONFIRMED, null);

        mockMvc.perform(post("/appointments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sameTimeDifferentDoctor)))
                .andExpect(status().isOk());
    }

    @Test
    void overlappingCancelledAppointment_doesNotBlockTheSlot() throws Exception {
        String tokenA = login("test-appt-a", "staffA@test.com");
        UUID doctorId = UUID.randomUUID();
        Instant start = Instant.now();
        Instant end = start.plusSeconds(1800);

        AppointmentRequestDTO cancelled = new AppointmentRequestDTO(
                UUID.randomUUID(), doctorId, start, end, AppointmentStatus.CANCELLED, null);
        mockMvc.perform(post("/appointments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelled)))
                .andExpect(status().isOk());

        AppointmentRequestDTO newBooking = new AppointmentRequestDTO(
                UUID.randomUUID(), doctorId, start, end, AppointmentStatus.CONFIRMED, null);
        mockMvc.perform(post("/appointments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newBooking)))
                .andExpect(status().isOk());
    }

    @Test
    void endTimeBeforeStartTime_isRejected() throws Exception {
        String tokenA = login("test-appt-a", "staffA@test.com");
        Instant start = Instant.now();

        AppointmentRequestDTO invalid = new AppointmentRequestDTO(
                UUID.randomUUID(), UUID.randomUUID(), start, start.minusSeconds(600),
                AppointmentStatus.REQUESTED, null);

        mockMvc.perform(post("/appointments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findById_returnsNotFound_whenAppointmentBelongsToAnotherTenant() throws Exception {
        String tokenA = login("test-appt-a", "staffA@test.com");
        String tokenB = login("test-appt-b", "staffB@test.com");

        String response = createAppointment(tokenA, UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(1800));
        UUID appointmentId = UUID.fromString(objectMapper.readTree(response).get("id").asString());

        mockMvc.perform(get("/appointments/" + appointmentId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void deniesAccess_whenTenantLacksTheAppointmentsEntitlement() throws Exception {
        String tokenA = login("test-appt-a", "staffA@test.com");

        TenantModuleEntitlement entitlement = entitlementRepository.findAll().stream()
                .filter(e -> e.getTenantId().equals(orgA.getId()))
                .findFirst().orElseThrow();
        entitlement.setEnabled(false);
        entitlementRepository.save(entitlement);

        mockMvc.perform(get("/appointments").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());
    }

    private String createAppointment(String token, UUID doctorId, Instant start, Instant end) throws Exception {
        AppointmentRequestDTO request = new AppointmentRequestDTO(
                UUID.randomUUID(), doctorId, start, end, AppointmentStatus.CONFIRMED, null);

        return mockMvc.perform(post("/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
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

    private User newUser(UUID tenantId, String email) {
        User user = new User();
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setFullName("Test Staff");
        user.setRole(Role.CLINIC_ADMIN);
        return user;
    }

    private TenantModuleEntitlement newEntitlement(UUID tenantId) {
        TenantModuleEntitlement entitlement = new TenantModuleEntitlement();
        entitlement.setTenantId(tenantId);
        entitlement.setModuleCode(ModuleCode.APPOINTMENTS);
        entitlement.setEnabled(true);
        return entitlement;
    }
}
