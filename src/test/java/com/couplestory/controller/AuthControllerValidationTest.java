package com.couplestory.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bean Validation is only useful if requests that violate it are actually rejected
 * before reaching business logic. Rate-limiting behavior is covered separately and in
 * isolation by LoginRateLimiterTest - it is deliberately not exercised here, since
 * @SpringBootTest shares one LoginRateLimiter instance across every test method (and
 * every test class reusing this Spring context) and MockMvc requests all originate
 * from the same loopback address, which would make order-dependent, flaky tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerValidationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void registeringWithAShortPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"valid@example.com\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registeringWithAnInvalidEmailIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"longenoughpassword\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginWithBlankCredentialsIsRejectedBeforeHittingTheAuthManager() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
