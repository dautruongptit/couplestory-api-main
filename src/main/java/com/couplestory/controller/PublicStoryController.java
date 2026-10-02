package com.couplestory.controller;

import com.couplestory.dto.PublicStoryResponse;
import com.couplestory.entity.*;
import com.couplestory.repository.*;
import com.couplestory.service.PlanLimitService;
import com.couplestory.service.StoryService;
import com.couplestory.service.TemplateAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public")
public class PublicStoryController {
    private final StoryService storyService;
    private final StoryEventRepository eventRepo;
    private final PhotoRepository photoRepo;
    private final StoryMessageRepository messageRepo;
    private final FavoriteMomentRepository momentRepo;
    private final TemplateAccessService templateAccessService;
    private final PlanLimitService planLimitService;

    public PublicStoryController(StoryService storyService,
                                 StoryEventRepository eventRepo,
                                 PhotoRepository photoRepo,
                                 StoryMessageRepository messageRepo,
                                 FavoriteMomentRepository momentRepo,
                                 TemplateAccessService templateAccessService,
                                 PlanLimitService planLimitService) {
        this.storyService = storyService;
        this.eventRepo = eventRepo;
        this.photoRepo = photoRepo;
        this.messageRepo = messageRepo;
        this.momentRepo = momentRepo;
        this.templateAccessService = templateAccessService;
        this.planLimitService = planLimitService;
    }

    @GetMapping("/story")
    public ResponseEntity<PublicStoryResponse> getPublicStory(@RequestParam String slug) {
        Story story = storyService.getPublicStory(slug);
        UUID storyId = story.getId();

        int maxEvents = templateAccessService.maxDisplayEvents(story.getTemplateCode());
        List<StoryEvent> events = eventRepo.findByStoryIdOrderBySortOrderAsc(storyId).stream()
                .filter(e -> Boolean.TRUE.equals(e.getIsVisible()))
                .limit(maxEvents)
                .collect(Collectors.toList());
        List<Photo> photos = photoRepo.findByOwnerTypeAndOwnerIdOrderBySortOrderAsc("STORY", storyId);
        List<StoryMessage> messages = messageRepo.findByStoryId(storyId);
        List<FavoriteMoment> moments = momentRepo.findByStoryIdOrderBySortOrderAsc(storyId);

        StoryMessage loveLetter = messages.stream()
                .filter(m -> "LOVE_LETTER".equals(m.getType())).findFirst().orElse(null);
        StoryMessage finalMessage = messages.stream()
                .filter(m -> "FINAL_MESSAGE".equals(m.getType())).findFirst().orElse(null);

        String coverStorageKey = null;
        if (story.getCoverPhotoId() != null) {
            coverStorageKey = photos.stream()
                    .filter(p -> p.getId().equals(story.getCoverPhotoId()))
                    .map(Photo::getStorageKey).findFirst().orElse(null);
        }

        Map<UUID, String> photoKeyById = photos.stream()
                .collect(Collectors.toMap(Photo::getId, Photo::getStorageKey, (a, b) -> a));

        PublicStoryResponse response = PublicStoryResponse.builder()
                .id(storyId.toString())
                .slug(story.getSlug())
                .templateCode(story.getTemplateCode())
                .templateConfig(story.getTemplateConfig())
                .coupleName1(story.getCoupleName1())
                .coupleName2(story.getCoupleName2())
                .title(story.getTitle())
                .shortQuote(story.getShortQuote())
                .startDate(story.getStartDate() != null ? story.getStartDate().toString() : null)
                .coverPhotoUrl(coverStorageKey)
                .showWatermark(planLimitService.plan(story.getOwnerId()).getShowWatermark())
                .events(events.stream().map(e -> PublicStoryResponse.TimelineEventDto.builder()
                        .id(e.getId().toString()).title(e.getTitle()).message(e.getMessage()).location(e.getLocation())
                        .eventDate(e.getEventDate() != null ? e.getEventDate().toString() : null)
                        .photoUrl(e.getPhotoId() != null ? photoKeyById.get(e.getPhotoId()) : null)
                        .order(e.getSortOrder()).build()).collect(Collectors.toList()))
                .gallery(photos.stream().map(p -> PublicStoryResponse.PhotoDto.builder()
                        .id(p.getId().toString()).url(p.getStorageKey()).thumbnailUrl(p.getThumbnailKey())
                        .order(p.getSortOrder()).build()).collect(Collectors.toList()))
                .loveLetter(loveLetter != null ? PublicStoryResponse.MessageDto.builder()
                        .id(loveLetter.getId().toString()).heading(loveLetter.getHeading())
                        .content(loveLetter.getContent()).signature(loveLetter.getSignature()).build() : null)
                .finalMessage(finalMessage != null ? PublicStoryResponse.MessageDto.builder()
                        .id(finalMessage.getId().toString()).heading(finalMessage.getHeading())
                        .content(finalMessage.getContent()).signature(finalMessage.getSignature()).build() : null)
                .moments(moments.stream().map(m -> PublicStoryResponse.MomentDto.builder()
                        .id(m.getId().toString()).title(m.getTitle()).description(m.getDescription())
                        .photoUrl(m.getPhotoId() != null ? photoKeyById.get(m.getPhotoId()) : null)
                        .order(m.getSortOrder()).build()).collect(Collectors.toList()))
                .build();

        return ResponseEntity.ok(response);
    }
}
