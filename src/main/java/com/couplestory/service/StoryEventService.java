package com.couplestory.service;

import com.couplestory.dto.CreateEventRequest;
import com.couplestory.entity.StoryEvent;
import com.couplestory.exception.ForbiddenOperationException;
import com.couplestory.exception.ResourceNotFoundException;
import com.couplestory.repository.StoryEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class StoryEventService {
    private final StoryEventRepository eventRepository;
    private final StoryAccessService storyAccessService;

    public StoryEventService(StoryEventRepository eventRepository, StoryAccessService storyAccessService) {
        this.eventRepository = eventRepository;
        this.storyAccessService = storyAccessService;
    }

    public List<StoryEvent> getEventsByStoryId(UUID storyId, UUID userId) {
        storyAccessService.requireOwnedStory(storyId, userId);
        return eventRepository.findByStoryIdOrderBySortOrderAsc(storyId);
    }

    @Transactional
    public StoryEvent createEvent(UUID storyId, CreateEventRequest request, UUID userId) {
        storyAccessService.requireOwnedStory(storyId, userId);
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        StoryEvent event = StoryEvent.builder()
                .storyId(storyId)
                .title(request.getTitle())
                .description(request.getDescription())
                .eventDate(request.getEventDate() != null ? LocalDate.parse(request.getEventDate()) : LocalDate.now())
                .photoId(request.getPhotoId() != null ? UUID.fromString(request.getPhotoId()) : null)
                .sortOrder(request.getOrder() != null ? request.getOrder() : 0)
                .build();
        return eventRepository.save(event);
    }

    @Transactional
    public StoryEvent updateEvent(UUID storyId, UUID eventId, CreateEventRequest request, UUID userId) {
        storyAccessService.requireOwnedStory(storyId, userId);
        StoryEvent event = requireEventInStory(storyId, eventId);
        if (request.getTitle() != null) event.setTitle(request.getTitle());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventDate() != null) event.setEventDate(LocalDate.parse(request.getEventDate()));
        if (request.getPhotoId() != null) event.setPhotoId(UUID.fromString(request.getPhotoId()));
        if (request.getOrder() != null) event.setSortOrder(request.getOrder());
        return eventRepository.save(event);
    }

    @Transactional
    public void deleteEvent(UUID storyId, UUID eventId, UUID userId) {
        storyAccessService.requireOwnedStory(storyId, userId);
        StoryEvent event = requireEventInStory(storyId, eventId);
        eventRepository.delete(event);
    }

    @Transactional
    public void reorderEvents(UUID storyId, List<UUID> eventIds, UUID userId) {
        storyAccessService.requireOwnedStory(storyId, userId);
        for (int i = 0; i < eventIds.size(); i++) {
            StoryEvent event = requireEventInStory(storyId, eventIds.get(i));
            event.setSortOrder(i);
            eventRepository.save(event);
        }
    }

    private StoryEvent requireEventInStory(UUID storyId, UUID eventId) {
        StoryEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        if (!event.getStoryId().equals(storyId)) {
            throw new ForbiddenOperationException("Event does not belong to this story");
        }
        return event;
    }
}
