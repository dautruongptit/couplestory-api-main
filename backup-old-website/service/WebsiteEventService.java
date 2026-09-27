package com.couplestory.service;

import com.couplestory.dto.CreateEventRequest;
import com.couplestory.entity.WebsiteEvent;
import com.couplestory.exception.ForbiddenOperationException;
import com.couplestory.exception.ResourceNotFoundException;
import com.couplestory.repository.WebsiteEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class WebsiteEventService {
    private final WebsiteEventRepository eventRepository;
    private final WebsiteAccessService websiteAccessService;

    public WebsiteEventService(WebsiteEventRepository eventRepository, WebsiteAccessService websiteAccessService) {
        this.eventRepository = eventRepository;
        this.websiteAccessService = websiteAccessService;
    }

    public List<WebsiteEvent> getEventsByWebsiteId(String websiteId, String userId) {
        websiteAccessService.requireOwnedWebsite(websiteId, userId);
        return eventRepository.findByWebsiteIdOrderBySortOrderAsc(websiteId);
    }

    @Transactional
    public WebsiteEvent createEvent(String websiteId, CreateEventRequest request, String userId) {
        websiteAccessService.requireOwnedWebsite(websiteId, userId);
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        WebsiteEvent event = WebsiteEvent.builder()
                .websiteId(websiteId)
                .title(request.getTitle())
                .description(request.getDescription())
                .eventDate(request.getEventDate() != null ? LocalDate.parse(request.getEventDate()) : null)
                .photoId(request.getPhotoId())
                .sortOrder(request.getOrder())
                .build();
        return eventRepository.save(event);
    }

    @Transactional
    public WebsiteEvent updateEvent(String websiteId, String eventId, CreateEventRequest request, String userId) {
        websiteAccessService.requireOwnedWebsite(websiteId, userId);
        WebsiteEvent event = requireEventInWebsite(websiteId, eventId);
        if (request.getTitle() != null) event.setTitle(request.getTitle());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventDate() != null) event.setEventDate(LocalDate.parse(request.getEventDate()));
        if (request.getPhotoId() != null) event.setPhotoId(request.getPhotoId());
        if (request.getOrder() != null) event.setSortOrder(request.getOrder());
        return eventRepository.save(event);
    }

    @Transactional
    public void deleteEvent(String websiteId, String eventId, String userId) {
        websiteAccessService.requireOwnedWebsite(websiteId, userId);
        WebsiteEvent event = requireEventInWebsite(websiteId, eventId);
        eventRepository.delete(event);
    }

    @Transactional
    public void reorderEvents(String websiteId, List<String> eventIds, String userId) {
        websiteAccessService.requireOwnedWebsite(websiteId, userId);
        for (int i = 0; i < eventIds.size(); i++) {
            WebsiteEvent event = requireEventInWebsite(websiteId, eventIds.get(i));
            event.setSortOrder(i);
            eventRepository.save(event);
        }
    }

    private WebsiteEvent requireEventInWebsite(String websiteId, String eventId) {
        WebsiteEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        if (!event.getWebsiteId().equals(websiteId)) {
            throw new ForbiddenOperationException("Event does not belong to this website");
        }
        return event;
    }
}
