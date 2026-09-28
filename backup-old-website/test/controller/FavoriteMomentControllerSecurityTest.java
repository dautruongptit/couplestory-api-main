package com.couplestory.controller;

import com.couplestory.entity.FavoriteMoment;
import com.couplestory.entity.User;
import com.couplestory.entity.Website;
import com.couplestory.repository.FavoriteMomentRepository;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FavoriteMomentControllerSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private WebsiteRepository websiteRepository;
    @Autowired private FavoriteMomentRepository momentRepository;

    private User owner;
    private User intruder;
    private Website websiteA;
    private Website websiteB;
    private FavoriteMoment momentOnA;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder()
                .email("owner-" + UUID.randomUUID() + "@test.com").passwordHash("x").role("USER").build());
        intruder = userRepository.save(User.builder()
                .email("intruder-" + UUID.randomUUID() + "@test.com").passwordHash("x").role("USER").build());
        websiteA = websiteRepository.save(newWebsite(owner.getId()));
        websiteB = websiteRepository.save(newWebsite(owner.getId()));
        momentOnA = momentRepository.save(FavoriteMoment.builder()
                .websiteId(websiteA.getId()).title("First kiss").sortOrder(0).build());
    }

    private Website newWebsite(String userId) {
        return Website.builder()
                .userId(userId).subdomain("story-" + UUID.randomUUID())
                .status("draft").planType("TRIAL").templateCode("minimal-couple").templateConfig("{}")
                .coupleName1("A").coupleName2("B").build();
    }

    private RequestPostProcessor as(User user) {
        return SecurityMockMvcRequestPostProcessors.user(UserDetailsImpl.build(user));
    }

    @Test
    void nonOwnerCannotCreateAMoment() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/stories/" + websiteA.getId() + "/moments")
                        .with(as(intruder))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hacked moment\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void creatingAMomentWithoutTitleIsRejected() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/stories/" + websiteA.getId() + "/moments")
                        .with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonOwnerCannotUpdateAMoment() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put(
                        "/api/stories/" + websiteA.getId() + "/moments/" + momentOnA.getId())
                        .with(as(intruder))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hacked\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotUpdateAMomentThroughAWebsiteItDoesNotBelongTo() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put(
                        "/api/stories/" + websiteB.getId() + "/moments/" + momentOnA.getId())
                        .with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Moved\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanDeleteTheirOwnMoment() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(
                        "/api/stories/" + websiteA.getId() + "/moments/" + momentOnA.getId())
                        .with(as(owner)))
                .andExpect(status().isOk());
    }

    @Test
    void deletingAMomentThatDoesNotExistReturns404() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(
                        "/api/stories/" + websiteA.getId() + "/moments/does-not-exist")
                        .with(as(owner)))
                .andExpect(status().isNotFound());
    }
}
