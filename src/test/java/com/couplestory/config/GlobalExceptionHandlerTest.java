package com.couplestory.config;

import com.couplestory.entity.User;
import com.couplestory.repository.UserRepository;
import com.couplestory.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A genuinely unmapped route must return 404, not 500. Found by actually hitting
 * /api/public/seed against a non-dev profile (where SeedController's bean doesn't
 * exist) and discovering Spring's NoResourceFoundException (Spring Framework 6.1+)
 * was falling through to GlobalExceptionHandler's generic Exception.class handler.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;

    @Test
    void aRouteThatMatchesNoControllerReturns404NotAGeneric500() throws Exception {
        User user = userRepository.save(User.builder()
                .email("nf-" + UUID.randomUUID() + "@test.com").passwordHash("x").build());

        mockMvc.perform(get("/api/this-route-does-not-exist")
                        .with(SecurityMockMvcRequestPostProcessors.user(UserDetailsImpl.build(user))))
                .andExpect(status().isNotFound());
    }
}
