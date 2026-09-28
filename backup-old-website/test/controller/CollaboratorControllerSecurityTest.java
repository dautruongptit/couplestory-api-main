package com.couplestory.controller;

import com.couplestory.entity.User;
import com.couplestory.entity.Website;
import com.couplestory.entity.WebsiteCollaborator;
import com.couplestory.repository.UserRepository;
import com.couplestory.repository.WebsiteCollaboratorRepository;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CollaboratorControllerSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private WebsiteRepository websiteRepository;
    @Autowired private WebsiteCollaboratorRepository collaboratorRepository;

    private User owner;
    private User intruder;
    private Website websiteA;
    private Website websiteB;
    private WebsiteCollaborator pendingInviteOnA;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder()
                .email("owner-" + UUID.randomUUID() + "@test.com").passwordHash("x").role("USER").build());
        intruder = userRepository.save(User.builder()
                .email("intruder-" + UUID.randomUUID() + "@test.com").passwordHash("x").role("USER").build());
        websiteA = websiteRepository.save(newWebsite(owner.getId()));
        websiteB = websiteRepository.save(newWebsite(owner.getId()));
        pendingInviteOnA = collaboratorRepository.save(WebsiteCollaborator.builder()
                .websiteId(websiteA.getId()).email("partner@test.com").role("PARTNER").status("pending")
                .inviteToken(UUID.randomUUID().toString())
                .invitedAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusDays(7))
                .build());
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
    void ownerCanInviteAPartner() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/stories/" + websiteA.getId() + "/collaborators/invite")
                        .with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"friend@test.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void nonOwnerCannotInviteAPartner() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/stories/" + websiteA.getId() + "/collaborators/invite")
                        .with(as(intruder))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"friend@test.com\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invitingWithAnInvalidEmailIsRejected() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/stories/" + websiteA.getId() + "/collaborators/invite")
                        .with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonOwnerCannotListCollaborators() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/stories/" + websiteA.getId() + "/collaborators")
                        .with(as(intruder)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanListTheirCollaborators() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/stories/" + websiteA.getId() + "/collaborators")
                        .with(as(owner)))
                .andExpect(status().isOk());
    }

    @Test
    void nonOwnerCannotRemoveAPartner() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(
                        "/api/stories/" + websiteA.getId() + "/collaborators/" + pendingInviteOnA.getId())
                        .with(as(intruder)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotRemoveACollaboratorThroughAWebsiteItDoesNotBelongTo() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(
                        "/api/stories/" + websiteB.getId() + "/collaborators/" + pendingInviteOnA.getId())
                        .with(as(owner)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanRemoveAPartnerFromTheirOwnWebsite() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(
                        "/api/stories/" + websiteA.getId() + "/collaborators/" + pendingInviteOnA.getId())
                        .with(as(owner)))
                .andExpect(status().isOk());
    }

    @Test
    void acceptingAnInvalidInviteTokenReturns404() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/collaborators/accept")
                        .with(as(intruder))
                        .param("token", "does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void acceptingAnInviteWithAMismatchedEmailIsRejected() throws Exception {
        // pendingInviteOnA.email = "partner@test.com"; intruder's email is random/different
        mockMvc.perform(MockMvcRequestBuilders.post("/api/collaborators/accept")
                        .with(as(intruder))
                        .param("token", pendingInviteOnA.getInviteToken()))
                .andExpect(status().isBadRequest());
    }
}
