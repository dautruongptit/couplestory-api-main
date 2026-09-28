package com.couplestory.controller;

import com.couplestory.entity.User;
import com.couplestory.entity.Website;
import com.couplestory.entity.WebsiteMessage;
import com.couplestory.repository.UserRepository;
import com.couplestory.repository.WebsiteMessageRepository;
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
class WebsiteMessageControllerSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private WebsiteRepository websiteRepository;
    @Autowired private WebsiteMessageRepository messageRepository;

    private User owner;
    private User intruder;
    private Website websiteA;
    private Website websiteB;
    private WebsiteMessage messageOnA;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder()
                .email("owner-" + UUID.randomUUID() + "@test.com").passwordHash("x").role("USER").build());
        intruder = userRepository.save(User.builder()
                .email("intruder-" + UUID.randomUUID() + "@test.com").passwordHash("x").role("USER").build());
        websiteA = websiteRepository.save(newWebsite(owner.getId()));
        websiteB = websiteRepository.save(newWebsite(owner.getId()));
        messageOnA = messageRepository.save(WebsiteMessage.builder()
                .websiteId(websiteA.getId()).type("love_letter").content("Dear you...").build());
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
    void ownerCanSaveAMessage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/stories/" + websiteA.getId() + "/messages")
                        .with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"love_letter\",\"content\":\"Hello\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void nonOwnerCannotSaveAMessage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/stories/" + websiteA.getId() + "/messages")
                        .with(as(intruder))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"love_letter\",\"content\":\"Hacked\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void savingAMessageWithBlankContentIsRejected() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/stories/" + websiteA.getId() + "/messages")
                        .with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"love_letter\",\"content\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonOwnerCannotDeleteAMessage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(
                        "/api/stories/" + websiteA.getId() + "/messages/" + messageOnA.getId())
                        .with(as(intruder)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotDeleteAMessageThroughAWebsiteItDoesNotBelongTo() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(
                        "/api/stories/" + websiteB.getId() + "/messages/" + messageOnA.getId())
                        .with(as(owner)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanDeleteTheirOwnMessage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(
                        "/api/stories/" + websiteA.getId() + "/messages/" + messageOnA.getId())
                        .with(as(owner)))
                .andExpect(status().isOk());
    }

    @Test
    void deletingAMessageThatDoesNotExistReturns404() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(
                        "/api/stories/" + websiteA.getId() + "/messages/does-not-exist")
                        .with(as(owner)))
                .andExpect(status().isNotFound());
    }
}
