package com.couplestory.controller;

import com.couplestory.dto.CreateStoryRequest;
import com.couplestory.dto.StoryResponse;
import com.couplestory.dto.UpdateStoryRequest;
import com.couplestory.entity.Story;
import com.couplestory.security.UserDetailsImpl;
import com.couplestory.service.StoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stories")
public class StoryController {
    private final StoryService storyService;

    public StoryController(StoryService storyService) {
        this.storyService = storyService;
    }

    @GetMapping
    public ResponseEntity<List<StoryResponse>> getMyStories(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<StoryResponse> stories = storyService.getStoriesByOwnerId(userDetails.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(stories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoryResponse> getStoryById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Story story = storyService.getOwnedStoryById(id, userDetails.getId());
        return ResponseEntity.ok(toResponse(story));
    }

    @PostMapping
    public ResponseEntity<StoryResponse> createStory(
            @Valid @RequestBody CreateStoryRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Story story = storyService.createStory(userDetails.getId(), request);
        return ResponseEntity.ok(toResponse(story));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StoryResponse> updateStory(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStoryRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Story story = storyService.updateStory(id, request, userDetails.getId());
        return ResponseEntity.ok(toResponse(story));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStory(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        storyService.deleteStory(id, userDetails.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<StoryResponse> publishStory(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Story story = storyService.publishStory(id, userDetails.getId());
        return ResponseEntity.ok(toResponse(story));
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<StoryResponse> unpublishStory(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Story story = storyService.unpublishStory(id, userDetails.getId());
        return ResponseEntity.ok(toResponse(story));
    }

    @PutMapping("/{id}/template")
    public ResponseEntity<StoryResponse> switchTemplate(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Story story = storyService.switchTemplate(id, body.get("templateCode"), userDetails.getId());
        return ResponseEntity.ok(toResponse(story));
    }

    private StoryResponse toResponse(Story s) {
        return StoryResponse.builder()
                .id(s.getId().toString())
                .slug(s.getSlug())
                .type(s.getType())
                .status(s.getStatus())
                .planType(s.getPlanType())
                .templateCode(s.getTemplateCode())
                .coupleName1(s.getCoupleName1())
                .coupleName2(s.getCoupleName2())
                .title(s.getTitle())
                .shortQuote(s.getShortQuote())
                .description(s.getDescription())
                .startDate(s.getStartDate() != null ? s.getStartDate().toString() : null)
                .coverPhotoId(s.getCoverPhotoId() != null ? s.getCoverPhotoId().toString() : null)
                .createdAt(s.getCreatedAt() != null ? s.getCreatedAt().toString() : null)
                .publishedAt(s.getPublishedAt() != null ? s.getPublishedAt().toString() : null)
                .expiresAt(s.getExpiresAt() != null ? s.getExpiresAt().toString() : null)
                .build();
    }
}
