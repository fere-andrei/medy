package com.example.medy.core.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Confirms application-prod.properties actually turns springdoc off, not just
 * that the property file exists. JWT_SECRET is supplied here because the
 * 'prod' profile's app.jwt.secret=${JWT_SECRET} has no default — see
 * JwtSecretResolutionTest for that behavior on its own.
 */
@SpringBootTest(properties = "JWT_SECRET=a-real-secret-that-was-not-committed")
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class OpenApiProdProfileTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocs_areDisabledUnderProdProfile() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound());
    }

    @Test
    void swaggerUi_isDisabledUnderProdProfile() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isNotFound());
    }
}
