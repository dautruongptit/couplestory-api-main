package com.couplestory.controller;

import com.couplestory.entity.User;
import com.couplestory.entity.Website;
import com.couplestory.repository.UserRepository;
import com.couplestory.repository.WebsiteRepository;
import com.couplestory.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression coverage for the Phase 1 + Phase 1b authorization fixes on
 * /api/stories/**: every mutating (and the owner-only read) endpoint must reject a
 * caller who is not the website's owner.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebsiteControllerSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private WebsiteRepository websiteRepository;

    private User owner;
    private User intruder;
    private Website ownerWebsite;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder()
                .email("owner-" + UUID.randomUUID() + "@test.com")
                .passwordHash("irrelevant-for-these-tests")
                .role("USER").build());
        intruder = userRepository.save(User.builder()
                .email("intruder-" + UUID.randomUUID() + "@test.com")
                .passwordHash("irrelevant-for-these-tests")
                .role("USER").build());
        ownerWebsite = websiteRepository.save(Website.builder()
                .userId(owner.getId())
                .subdomain("story-" + UUID.randomUUID())
                .status("draft")
                .planType("TRIAL")
                .templateCode("minimal-couple")
                .templateConfig("{}")
                .coupleName1("A")
                .coupleName2("B")
                .build());
    }

    private RequestPostProcessor as(User user) {
        return SecurityMockMvcRequestPostProcessors.user(UserDetailsImpl.build(user));
    }

    @Test
    void ownerCanReadTheirOwnStory() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/stories/" + ownerWebsite.getId()).with(as(owner)))
                .andExpect(status().isOk());
    }

    @Test
    void nonOwnerCannotReadTheStory() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/stories/" + ownerWebsite.getId()).with(as(intruder)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanUpdateTheirOwnStory() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/stories/" + ownerWebsite.getId())
                        .with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coupleName1\":\"Changed\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void nonOwnerCannotUpdateTheStory() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/stories/" + ownerWebsite.getId())
                        .with(as(intruder))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coupleName1\":\"Hacked\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonOwnerCannotDeleteTheStory() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/stories/" + ownerWebsite.getId()).with(as(intruder)))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonOwnerCannotPublishTheStory() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/stories/" + ownerWebsite.getId() + "/publish").with(as(intruder)))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonOwnerCannotUnpublishTheStory() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/stories/" + ownerWebsite.getId() + "/unpublish").with(as(intruder)))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonOwnerCannotSwitchTemplate() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/stories/" + ownerWebsite.getId() + "/template")
                        .with(as(intruder))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateCode\":\"eternal-love\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updatingAWebsiteThatDoesNotExistReturns404() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/stories/does-not-exist")
                        .with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coupleName1\":\"X\"}"))
                .andExpect(status().isNotFound());
    }
}
