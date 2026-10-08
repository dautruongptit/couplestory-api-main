package com.couplestory.service;

import com.couplestory.dto.CreateEventRequest;
import com.couplestory.entity.ActivityAction;
import com.couplestory.entity.Story;
import com.couplestory.entity.StoryEvent;
import com.couplestory.repository.StoryEventRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The service layer must tell the timeline about the actions the user sees in "Hoạt động gần đây". */
class ActivityCallSitesTest {

    private final StoryEventRepository events = mock(StoryEventRepository.class);
    private final StoryAccessService access = mock(StoryAccessService.class);
    private final TemplateAccessService templates = mock(TemplateAccessService.class);
    private final UserActivityService activity = mock(UserActivityService.class);
    private final StoryEventService service = new StoryEventService(events, access, templates, activity);

    private final UUID user = UUID.randomUUID();
    private final UUID storyId = UUID.randomUUID();

    @Test
    void creatingAnEventIsRecordedWithItsTitle() {
        Story story = Story.builder().id(storyId).templateCode("t").build();
        when(access.requireOwnedStory(storyId, user)).thenReturn(story);
        when(templates.maxDisplayEvents("t")).thenReturn(6);
        UUID eventId = UUID.randomUUID();
        when(events.save(any(StoryEvent.class))).thenAnswer(i -> {
            StoryEvent e = i.getArgument(0);
            e.setId(eventId);
            return e;
        });
        CreateEventRequest request = new CreateEventRequest();
        request.setTitle("Lần đầu gặp nhau");
        request.setMessage("Hôm đó trời mưa");

        service.createEvent(storyId, request, user);

        verify(activity).record(eq(user), eq(ActivityAction.EVENT_CREATED), eq("EVENT"), eq(eventId), contains("Lần đầu gặp nhau"));
    }

    @Test
    void aFailedCreateRecordsNothing() {
        when(access.requireOwnedStory(storyId, user)).thenReturn(Story.builder().id(storyId).build());
        CreateEventRequest request = new CreateEventRequest();
        request.setTitle(" ");
        request.setMessage("x");

        try {
            service.createEvent(storyId, request, user);
        } catch (IllegalArgumentException expected) {
            // validation failure
        }

        verify(activity, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void deletingAnEventIsRecorded() {
        UUID eventId = UUID.randomUUID();
        StoryEvent event = StoryEvent.builder().id(eventId).storyId(storyId).title("Chuyến đi Đà Lạt").build();
        when(events.findById(eventId)).thenReturn(Optional.of(event));

        service.deleteEvent(storyId, eventId, user);

        verify(activity).record(eq(user), eq(ActivityAction.EVENT_DELETED), eq("EVENT"), eq(eventId), contains("Chuyến đi Đà Lạt"));
    }
}
