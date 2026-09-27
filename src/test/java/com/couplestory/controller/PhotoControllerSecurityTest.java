package com.couplestory.controller;

import com.couplestory.entity.Photo;
import com.couplestory.entity.User;
import com.couplestory.entity.Website;
import com.couplestory.repository.PhotoRepository;
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
 * Regression coverage for photo authorization: ownership of the parent website, plus
 * the narrower check that a photo actually belongs to the website named in the path
 * (the secondary IDOR closed alongside the main ownership fix).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PhotoControllerSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private WebsiteRepository websiteRepository;
    @Autowired private PhotoRepository photoRepository;

    private User owner;
    private User intruder;
    private Website ownerWebsiteA;
    private Website ownerWebsiteB;
    private Photo photoOnWebsiteA;
    private Photo secondPhotoOnWebsiteA;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder()
                .email("owner-" + UUID.randomUUID() + "@test.com").passwordHash("x").role("USER").build());
        intruder = userRepository.save(User.builder()
                .email("intruder-" + UUID.randomUUID() + "@test.com").passwordHash("x").role("USER").build());

        ownerWebsiteA = websiteRepository.save(newWebsite(owner.getId()));
        ownerWebsiteB = websiteRepository.save(newWebsite(owner.getId())); // second website, same owner

        photoOnWebsiteA = photoRepository.save(Photo.builder()
                .ownerType("WEBSITE").ownerId(ownerWebsiteA.getId())
                .url("http://example.test/a.jpg").filenameStored("a.jpg").sortOrder(0)
                .build());
        secondPhotoOnWebsiteA = photoRepository.save(Photo.builder()
                .ownerType("WEBSITE").ownerId(ownerWebsiteA.getId())
                .url("http://example.test/b.jpg").filenameStored("b.jpg").sortOrder(1)
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
    void ownerCanDeleteTheirOwnPhoto() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(
                        "/api/stories/" + ownerWebsiteA.getId() + "/photos/" + photoOnWebsiteA.getId())
                        .with(as(owner)))
                .andExpect(status().isOk());
    }

    @Test
    void nonOwnerCannotDeleteThePhoto() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(
                        "/api/stories/" + ownerWebsiteA.getId() + "/photos/" + photoOnWebsiteA.getId())
                        .with(as(intruder)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotDeletePhotoThroughADifferentWebsitePathEvenIfCallerOwnsThatWebsiteToo() throws Exception {
        // photoOnWebsiteA belongs to ownerWebsiteA, not ownerWebsiteB - even though the
        // same user owns both, the path/photo pair must match or the delete is rejected.
        mockMvc.perform(MockMvcRequestBuilders.delete(
                        "/api/stories/" + ownerWebsiteB.getId() + "/photos/" + photoOnWebsiteA.getId())
                        .with(as(owner)))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonOwnerCannotReorderPhotos() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/stories/" + ownerWebsiteA.getId() + "/photos/order")
                        .with(as(intruder))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"" + photoOnWebsiteA.getId() + "\"]"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotReorderAPhotoThatBelongsToAnotherWebsite() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/stories/" + ownerWebsiteB.getId() + "/photos/order")
                        .with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"" + photoOnWebsiteA.getId() + "\"]"))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonOwnerCannotListPhotos() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/stories/" + ownerWebsiteA.getId() + "/photos")
                        .with(as(intruder)))
                .andExpect(status().isForbidden());
    }

    @Test
    void reorderIsAtomic_aFailureOnOneItemRollsBackTheWholeBatch() throws Exception {
        // photoOnWebsiteA=0, secondPhotoOnWebsiteA=1 initially. Reordering with a mix of
        // a valid item first and a photo belonging to nobody (unknown id) second must
        // leave BOTH photos' sortOrder untouched, not just reject the unknown one.
        mockMvc.perform(MockMvcRequestBuilders.put("/api/stories/" + ownerWebsiteA.getId() + "/photos/order")
                        .with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"" + secondPhotoOnWebsiteA.getId() + "\", \"does-not-exist\"]"))
                .andExpect(status().isNotFound());

        Photo first = photoRepository.findById(photoOnWebsiteA.getId()).orElseThrow();
        Photo second = photoRepository.findById(secondPhotoOnWebsiteA.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(0, first.getSortOrder(),
                "first photo's sortOrder must be unchanged - the batch should have rolled back");
        org.junit.jupiter.api.Assertions.assertEquals(1, second.getSortOrder(),
                "second photo's sortOrder must be unchanged even though it was processed " +
                        "before the failing item - the whole transaction must roll back, not partially apply");
    }
}
