package com.couplestory.controller;

import com.couplestory.dto.CreateEventRequest;
import com.couplestory.entity.WebsiteEvent;
import com.couplestory.security.UserDetailsImpl;
import com.couplestory.service.WebsiteEventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stories/{websiteId}/events")
public class WebsiteEventController {
    private final WebsiteEventService eventService;

    public WebsiteEventController(WebsiteEventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<WebsiteEvent>> getEvents(
            @PathVariable String websiteId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(eventService.getEventsByWebsiteId(websiteId, userDetails.getId()));
    }

    @PostMapping
    public ResponseEntity<WebsiteEvent> createEvent(
            @PathVariable String websiteId,
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(eventService.createEvent(websiteId, request, userDetails.getId()));
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<WebsiteEvent> updateEvent(
            @PathVariable String websiteId,
            @PathVariable String eventId,
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(eventService.updateEvent(websiteId, eventId, request, userDetails.getId()));
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable String websiteId,
            @PathVariable String eventId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        eventService.deleteEvent(websiteId, eventId, userDetails.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/order")
    public ResponseEntity<Void> reorderEvents(
            @PathVariable String websiteId,
            @RequestBody List<String> eventIds,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        eventService.reorderEvents(websiteId, eventIds, userDetails.getId());
        return ResponseEntity.ok().build();
    }
}
