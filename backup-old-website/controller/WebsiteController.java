package com.couplestory.controller;

import com.couplestory.dto.CreateStoryRequest;
import com.couplestory.dto.StoryResponse;
import com.couplestory.dto.UpdateStoryRequest;
import com.couplestory.entity.Website;
import com.couplestory.security.UserDetailsImpl;
import com.couplestory.service.WebsiteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stories")
public class WebsiteController {
    private final WebsiteService websiteService;

    public WebsiteController(WebsiteService websiteService) {
        this.websiteService = websiteService;
    }

    @GetMapping
    public ResponseEntity<List<StoryResponse>> getMyStories(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<StoryResponse> stories = websiteService.getWebsitesByUserId(userDetails.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(stories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoryResponse> getStoryById(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Website website = websiteService.getOwnedWebsiteById(id, userDetails.getId());
        return ResponseEntity.ok(toResponse(website));
    }

    @PostMapping
    public ResponseEntity<StoryResponse> createStory(
            @Valid @RequestBody CreateStoryRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Website website = websiteService.createWebsite(userDetails.getId(), request);
        return ResponseEntity.ok(toResponse(website));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StoryResponse> updateStory(
            @PathVariable String id,
            @Valid @RequestBody UpdateStoryRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Website website = websiteService.updateWebsite(id, request, userDetails.getId());
        return ResponseEntity.ok(toResponse(website));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStory(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        websiteService.deleteWebsite(id, userDetails.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<StoryResponse> publishStory(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Website website = websiteService.publishWebsite(id, userDetails.getId());
        return ResponseEntity.ok(toResponse(website));
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<StoryResponse> unpublishStory(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Website website = websiteService.unpublishWebsite(id, userDetails.getId());
        return ResponseEntity.ok(toResponse(website));
    }

    @PutMapping("/{id}/template")
    public ResponseEntity<StoryResponse> switchTemplate(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Website website = websiteService.switchTemplate(id, body.get("templateCode"), userDetails.getId());
        return ResponseEntity.ok(toResponse(website));
    }

    private StoryResponse toResponse(Website w) {
        return StoryResponse.builder()
                .id(w.getId())
                .subdomain(w.getSubdomain())
                .status(w.getStatus())
                .planType(w.getPlanType())
                .templateCode(w.getTemplateCode())
                .coupleName1(w.getCoupleName1())
                .coupleName2(w.getCoupleName2())
                .title(w.getTitle())
                .shortQuote(w.getShortQuote())
                .startDate(w.getStartDate() != null ? w.getStartDate().toString() : null)
                .coverPhotoId(w.getCoverPhotoId())
                .createdAt(w.getCreatedAt() != null ? w.getCreatedAt().toString() : null)
                .publishedAt(w.getPublishedAt() != null ? w.getPublishedAt().toString() : null)
                .expiresAt(w.getExpiresAt() != null ? w.getExpiresAt().toString() : null)
                .build();
    }
}
