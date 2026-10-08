package com.couplestory.service;

import com.couplestory.dto.CreateEventRequest;
import com.couplestory.entity.ActivityAction;
import com.couplestory.entity.Story;
import com.couplestory.entity.StoryEvent;
import com.couplestory.exception.ForbiddenOperationException;
import com.couplestory.exception.ResourceNotFoundException;
import com.couplestory.repository.StoryEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class StoryEventService {
    private final StoryEventRepository eventRepository;
    private final StoryAccessService storyAccessService;
    private final TemplateAccessService templateAccessService;
    private final UserActivityService activityService;

    public StoryEventService(StoryEventRepository eventRepository,
                             StoryAccessService storyAccessService,
                             TemplateAccessService templateAccessService,
                             UserActivityService activityService) {
        this.eventRepository = eventRepository;
        this.storyAccessService = storyAccessService;
        this.templateAccessService = templateAccessService;
        this.activityService = activityService;
    }

    public List<StoryEvent> getEventsByStoryId(UUID storyId, UUID userId) {
        storyAccessService.requireOwnedStory(storyId, userId);
        return eventRepository.findByStoryIdOrderBySortOrderAsc(storyId);
    }

    @Transactional
    public StoryEvent createEvent(UUID storyId, CreateEventRequest request, UUID userId) {
        Story story = storyAccessService.requireOwnedStory(storyId, userId);
        requireText(request.getTitle(), "title");
        requireText(request.getMessage(), "message");

        // A new event is shown only while the template still has room; extra events are kept hidden.
        int max = templateAccessService.maxDisplayEvents(story.getTemplateCode());
        boolean hasRoom = eventRepository.countByStoryIdAndIsVisibleTrue(storyId) < max;

        StoryEvent event = StoryEvent.builder()
                .storyId(storyId)
                .title(request.getTitle().trim())
                .message(request.getMessage().trim())
                .location(blankToNull(request.getLocation()))
                .eventDate(parseDate(request.getEventDate()))
                .photoId(request.getPhotoId() != null ? UUID.fromString(request.getPhotoId()) : null)
                .sortOrder(request.getOrder() != null ? request.getOrder() : 0)
                .isVisible(hasRoom)
                .build();
        StoryEvent saved = eventRepository.save(event);
        activityService.record(userId, ActivityAction.EVENT_CREATED, "EVENT", saved.getId(), "Đã thêm kỷ niệm \"" + saved.getTitle() + "\"");
        return saved;
    }

    @Transactional
    public StoryEvent updateEvent(UUID storyId, UUID eventId, CreateEventRequest request, UUID userId) {
        storyAccessService.requireOwnedStory(storyId, userId);
        StoryEvent event = requireEventInStory(storyId, eventId);
        if (request.getTitle() != null) {
            requireText(request.getTitle(), "title");
            event.setTitle(request.getTitle().trim());
        }
        if (request.getMessage() != null) {
            requireText(request.getMessage(), "message");
            event.setMessage(request.getMessage().trim());
        }
        if (request.getLocation() != null) event.setLocation(blankToNull(request.getLocation()));
        if (request.getEventDate() != null) event.setEventDate(parseDate(request.getEventDate()));
        if (request.getPhotoId() != null) event.setPhotoId(UUID.fromString(request.getPhotoId()));
        if (request.getOrder() != null) event.setSortOrder(request.getOrder());
        StoryEvent saved = eventRepository.save(event);
        activityService.record(userId, ActivityAction.EVENT_UPDATED, "EVENT", saved.getId(), "Đã cập nhật kỷ niệm \"" + saved.getTitle() + "\"");
        return saved;
    }

    @Transactional
    public void deleteEvent(UUID storyId, UUID eventId, UUID userId) {
        storyAccessService.requireOwnedStory(storyId, userId);
        StoryEvent event = requireEventInStory(storyId, eventId);
        eventRepository.delete(event);
        activityService.record(userId, ActivityAction.EVENT_DELETED, "EVENT", eventId, "Đã xóa kỷ niệm \"" + event.getTitle() + "\"");
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

    /** Sets which events the template shows. Events not listed stay in the story but are hidden. */
    @Transactional
    public List<StoryEvent> setVisibleEvents(UUID storyId, List<UUID> visibleIds, UUID userId) {
        Story story = storyAccessService.requireOwnedStory(storyId, userId);
        int max = templateAccessService.maxDisplayEvents(story.getTemplateCode());
        Set<UUID> visible = new HashSet<>(visibleIds);
        if (visible.size() > max) {
            throw new IllegalArgumentException("Template này hiển thị tối đa " + max + " kỷ niệm.");
        }
        List<StoryEvent> events = eventRepository.findByStoryIdOrderBySortOrderAsc(storyId);
        Set<UUID> known = new HashSet<>();
        events.forEach(e -> known.add(e.getId()));
        if (!known.containsAll(visible)) {
            throw new ForbiddenOperationException("Event does not belong to this story");
        }
        events.forEach(e -> e.setIsVisible(visible.contains(e.getId())));
        return eventRepository.saveAll(events);
    }

    private StoryEvent requireEventInStory(UUID storyId, UUID eventId) {
        StoryEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        if (!event.getStoryId().equals(storyId)) {
            throw new ForbiddenOperationException("Event does not belong to this story");
        }
        return event;
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }
}
