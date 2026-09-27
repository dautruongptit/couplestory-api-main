package com.couplestory.controller;

import com.couplestory.entity.User;
import com.couplestory.entity.Website;
import com.couplestory.entity.WebsiteEvent;
import com.couplestory.repository.UserRepository;
import com.couplestory.repository.WebsiteEventRepository;
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
 * Same ownership pattern as Website/Photo, applied to a nested sub-resource
 * (WebsiteEvent). WebsiteMessage and FavoriteMoment share this exact code path
 * (WebsiteAccessService + a "belongs to this website" check) and are covered by the
 * WebsiteAccessServiceTest unit test at the shared-logic level.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebsiteEventControllerSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private WebsiteRepository websiteRepository;
    @Autowired private WebsiteEventRepository eventRepository;

    private User owner;
    private User intruder;
    private Website websiteA;
    private Website websiteB;
    private WebsiteEvent eventOnA;
    private WebsiteEvent secondEventOnA;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder()
                .email("owner-" + UUID.randomUUID() + "@test.com").passwordHash("x").role("USER").build());
        intruder = userRepository.save(User.builder()
                .email("intruder-" + UUID.randomUUID() + "@test.com").passwordHash("x").role("USER").build());

        websiteA = websiteRepository.save(newWebsite(owner.getId()));
        websiteB = websiteRepository.save(newWebsite(owner.getId()));

        eventOnA = eventRepository.save(WebsiteEvent.builder()
                .websiteId(websiteA.getId()).title("First date").sortOrder(0).build());
        secondEventOnA = eventRepository.save(WebsiteEvent.builder()
                .websiteId(websiteA.getId()).title("First kiss").sortOrder(1).build());
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
    void nonOwnerCannotCreateEvent() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/stories/" + websiteA.getId() + "/events")
                        .with(as(intruder))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hacked event\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void creatingEventWithoutTitleIsRejected() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/stories/" + websiteA.getId() + "/events")
                        .with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonOwnerCannotDeleteEvent() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(
                        "/api/stories/" + websiteA.getId() + "/events/" + eventOnA.getId())
                        .with(as(intruder)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotDeleteAnEventThroughAWebsiteItDoesNotBelongTo() throws Exception {
        // owner owns both websites, but eventOnA belongs to websiteA, not websiteB.
        mockMvc.perform(MockMvcRequestBuilders.delete(
                        "/api/stories/" + websiteB.getId() + "/events/" + eventOnA.getId())
                        .with(as(owner)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanDeleteTheirOwnEvent() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(
                        "/api/stories/" + websiteA.getId() + "/events/" + eventOnA.getId())
                        .with(as(owner)))
                .andExpect(status().isOk());
    }

    @Test
    void reorderIsAtomic_aFailureOnOneItemRollsBackTheWholeBatch() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/stories/" + websiteA.getId() + "/events/order")
                        .with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"" + secondEventOnA.getId() + "\", \"does-not-exist\"]"))
                .andExpect(status().isNotFound());

        WebsiteEvent first = eventRepository.findById(eventOnA.getId()).orElseThrow();
        WebsiteEvent second = eventRepository.findById(secondEventOnA.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(0, first.getSortOrder(),
                "first event's sortOrder must be unchanged - the batch should have rolled back");
        org.junit.jupiter.api.Assertions.assertEquals(1, second.getSortOrder(),
                "second event's sortOrder must be unchanged even though it was processed " +
                        "before the failing item - the whole transaction must roll back, not partially apply");
    }
}
