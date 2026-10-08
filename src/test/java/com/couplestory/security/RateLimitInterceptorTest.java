package com.couplestory.security;

import com.couplestory.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitInterceptorTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RateLimiter rateLimiter;

    @AfterEach
    void reset() {
        rateLimiter.clear();
    }

    private static RequestPostProcessor fromIp(String ip) {
        return request -> {
            request.addHeader("CF-Connecting-IP", ip);
            return request;
        };
    }

    private static RequestPostProcessor asUser(UUID id) {
        return user(UserDetailsImpl.build(User.builder().id(id).email(id + "@test.local").passwordHash("x").build()));
    }

    private int register(String ip) throws Exception {
        return mockMvc.perform(post("/api/auth/register").with(fromIp(ip))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void registrationIsLimitedPerIpWith429AndRetryAfter() throws Exception {
        for (int i = 0; i < RatePolicies.AUTH_REGISTER.maxRequests(); i++) {
            assertThat(register("203.0.113.7")).isEqualTo(400); // reaches validation, not blocked
        }

        mockMvc.perform(post("/api/auth/register").with(fromIp("203.0.113.7"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.retryAfterSeconds").isNumber());
    }

    @Test
    void anotherIpIsNotAffected() throws Exception {
        for (int i = 0; i < RatePolicies.AUTH_REGISTER.maxRequests() + 1; i++) register("203.0.113.8");

        assertThat(register("203.0.113.9")).isEqualTo(400);
    }

    @Test
    void orderCreationIsLimitedPerUserNotPerIp() throws Exception {
        UUID busy = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        for (int i = 0; i < RatePolicies.ORDER_CREATE.maxRequests(); i++) {
            mockMvc.perform(post("/api/orders").with(asUser(busy)).with(fromIp("198.51.100.5"))
                    .contentType(MediaType.APPLICATION_JSON).content("{}"));
        }

        mockMvc.perform(post("/api/orders").with(asUser(busy)).with(fromIp("198.51.100.5"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isTooManyRequests());

        // Same shared IP, different account: still allowed through to the controller.
        int otherStatus = mockMvc.perform(post("/api/orders").with(asUser(other)).with(fromIp("198.51.100.5"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus();
        assertThat(otherStatus).isNotEqualTo(429);
    }

    @Test
    void uploadsAreLimitedPerUser() throws Exception {
        UUID uploader = UUID.randomUUID();
        String url = "/api/stories/" + UUID.randomUUID() + "/photos";
        for (int i = 0; i < RatePolicies.MEDIA_UPLOAD.maxRequests(); i++) {
            mockMvc.perform(post(url).with(asUser(uploader)).contentType(MediaType.MULTIPART_FORM_DATA));
        }

        mockMvc.perform(post(url).with(asUser(uploader)).contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void unlistedEndpointsAreNeverLimited() throws Exception {
        for (int i = 0; i < 100; i++) {
            int s = mockMvc.perform(post("/api/auth/logout").with(fromIp("203.0.113.50"))).andReturn().getResponse().getStatus();
            assertThat(s).isNotEqualTo(429);
        }
    }
}
