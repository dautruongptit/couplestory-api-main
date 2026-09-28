package com.couplestory.controller;

import com.couplestory.entity.User;
import com.couplestory.entity.Website;
import com.couplestory.repository.UserRepository;
import com.couplestory.repository.WebsiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Confirms the framework-level guarantee every other security test relies on:
 * WebSecurityConfig's anyRequest().authenticated() actually rejects unauthenticated
 * callers before any controller/service ownership logic ever runs. One representative
 * check per resource family is enough here - the rule is global, not per-endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UnauthenticatedAccessSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private WebsiteRepository websiteRepository;

    private Website someWebsite;

    @BeforeEach
    void setUp() {
        User owner = userRepository.save(User.builder()
                .email("owner-" + UUID.randomUUID() + "@test.com").passwordHash("x").role("USER").build());
        someWebsite = websiteRepository.save(Website.builder()
                .userId(owner.getId()).subdomain("story-" + UUID.randomUUID())
                .status("draft").planType("TRIAL").templateCode("minimal-couple").templateConfig("{}")
                .coupleName1("A").coupleName2("B").build());
    }

    @Test
    void readingAStoryWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/stories/" + someWebsite.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listingMyStoriesWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/stories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listingPhotosWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/stories/" + someWebsite.getId() + "/photos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listingEventsWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/stories/" + someWebsite.getId() + "/events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listingCollaboratorsWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/stories/" + someWebsite.getId() + "/collaborators"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void notificationsWithoutAuthenticationAreRejected() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicStoryLookupDoesNotRequireAuthentication() throws Exception {
        // Sanity check the other direction: the genuinely public read path must NOT be
        // blocked by the same rule (proves the security config differentiates the two).
        mockMvc.perform(get("/api/public/story").param("subdomain", "does-not-exist"))
                .andExpect(status().isNotFound()); // 404 from the service, not 401 from security
    }
}
