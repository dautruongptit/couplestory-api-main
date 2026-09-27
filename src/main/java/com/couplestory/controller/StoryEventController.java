package com.couplestory.controller;

import com.couplestory.dto.CreateEventRequest;
import com.couplestory.entity.StoryEvent;
import com.couplestory.security.UserDetailsImpl;
import com.couplestory.service.StoryEventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stories/{storyId}/events")
public class StoryEventController {
    private final StoryEventService eventService;

    public StoryEventController(StoryEventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<StoryEvent>> getEvents(
            @PathVariable UUID storyId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(eventService.getEventsByStoryId(storyId, userDetails.getId()));
    }

    @PostMapping
    public ResponseEntity<StoryEvent> createEvent(
            @PathVariable UUID storyId,
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(eventService.createEvent(storyId, request, userDetails.getId()));
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<StoryEvent> updateEvent(
            @PathVariable UUID storyId,
            @PathVariable UUID eventId,
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(eventService.updateEvent(storyId, eventId, request, userDetails.getId()));
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable UUID storyId,
            @PathVariable UUID eventId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        eventService.deleteEvent(storyId, eventId, userDetails.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/order")
    public ResponseEntity<Void> reorderEvents(
            @PathVariable UUID storyId,
            @RequestBody List<UUID> eventIds,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        eventService.reorderEvents(storyId, eventIds, userDetails.getId());
        return ResponseEntity.ok().build();
    }
}
