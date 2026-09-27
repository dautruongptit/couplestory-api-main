package com.couplestory.controller;

import com.couplestory.dto.CreateMomentRequest;
import com.couplestory.entity.FavoriteMoment;
import com.couplestory.security.UserDetailsImpl;
import com.couplestory.service.FavoriteMomentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stories/{storyId}/moments")
public class FavoriteMomentController {
    private final FavoriteMomentService momentService;

    public FavoriteMomentController(FavoriteMomentService momentService) {
        this.momentService = momentService;
    }

    @GetMapping
    public ResponseEntity<List<FavoriteMoment>> getMoments(
            @PathVariable UUID storyId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(momentService.getMomentsByStoryId(storyId, userDetails.getId()));
    }

    @PostMapping
    public ResponseEntity<FavoriteMoment> createMoment(
            @PathVariable UUID storyId,
            @Valid @RequestBody CreateMomentRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(momentService.createMoment(storyId, request, userDetails.getId()));
    }

    @PutMapping("/{momentId}")
    public ResponseEntity<FavoriteMoment> updateMoment(
            @PathVariable UUID storyId,
            @PathVariable UUID momentId,
            @Valid @RequestBody CreateMomentRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(momentService.updateMoment(storyId, momentId, request, userDetails.getId()));
    }

    @DeleteMapping("/{momentId}")
    public ResponseEntity<Void> deleteMoment(
            @PathVariable UUID storyId,
            @PathVariable UUID momentId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        momentService.deleteMoment(storyId, momentId, userDetails.getId());
        return ResponseEntity.ok().build();
    }
}
