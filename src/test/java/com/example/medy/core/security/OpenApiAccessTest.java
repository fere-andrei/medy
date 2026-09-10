package com.example.medy.core.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Docs are intentionally public: a JWT is header-based and stateless, so a
 * plain browser navigation to the Swagger UI can't attach one — gating the
 * docs page itself behind auth would make it unopenable. This only locks in
 * that access rule; the endpoints it documents stay behind their own checks.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocs_isAccessibleWithoutAToken() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerUi_isAccessibleWithoutAToken() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
