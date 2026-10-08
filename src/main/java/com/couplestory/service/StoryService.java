package com.couplestory.service;

import com.couplestory.dto.CreateStoryRequest;
import com.couplestory.dto.UpdateStoryRequest;
import com.couplestory.entity.Plan;
import com.couplestory.entity.ActivityAction;
import com.couplestory.entity.Story;
import com.couplestory.entity.StoryEvent;
import com.couplestory.entity.StoryMessage;
import com.couplestory.exception.ResourceNotFoundException;
import com.couplestory.exception.SlugTakenException;
import com.couplestory.repository.StoryEventRepository;
import com.couplestory.repository.StoryMessageRepository;
import com.couplestory.repository.StoryRepository;
import com.couplestory.util.SlugUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StoryService {
    private final StoryRepository storyRepository;
    private final StoryEventRepository storyEventRepository;
    private final StoryMessageRepository storyMessageRepository;
    private final StoryAccessService storyAccessService;
    private final TemplateAccessService templateAccessService;
    private final PlanLimitService planLimitService;
    private final NotificationService notificationService;
    private final UserActivityService activityService;

    public StoryService(StoryRepository storyRepository,
                        StoryEventRepository storyEventRepository,
                        StoryMessageRepository storyMessageRepository,
                        StoryAccessService storyAccessService,
                        TemplateAccessService templateAccessService,
                        PlanLimitService planLimitService,
                        NotificationService notificationService,
                        UserActivityService activityService) {
        this.storyRepository = storyRepository;
        this.storyEventRepository = storyEventRepository;
        this.storyMessageRepository = storyMessageRepository;
        this.storyAccessService = storyAccessService;
        this.templateAccessService = templateAccessService;
        this.planLimitService = planLimitService;
        this.notificationService = notificationService;
        this.activityService = activityService;
    }

    private String planOf(UUID userId) {
        return planLimitService.planCode(userId);
    }

    public List<Story> getStoriesByOwnerId(UUID ownerId) {
        return storyRepository.findByOwnerIdAndStatusNot(ownerId, "DELETED");
    }

    public Story getOwnedStoryById(UUID id, UUID userId) {
        return storyAccessService.requireOwnedStory(id, userId);
    }

    @Transactional
    public Story createStory(UUID ownerId, CreateStoryRequest request) {
        String plan = planOf(ownerId);
        Plan planRow = planLimitService.plan(ownerId);
        int maxStories = planRow.getMaxStories();
        if (planRow.getMaxTotalStories() != null
                && storyRepository.countByOwnerId(ownerId) >= planRow.getMaxTotalStories()) {
            throw new IllegalArgumentException("Bạn đã tạo tối đa " + planRow.getMaxTotalStories()
                    + " website của gói " + plan + " (tính cả website đã xóa). Vui lòng nâng cấp gói.");
        }
        long count = storyRepository.countByOwnerIdAndStatusNot(ownerId, "DELETED");
        if (count >= maxStories) {
            throw new IllegalArgumentException("Bạn đã đạt giới hạn " + maxStories
                    + " story của gói " + plan + ". Vui lòng xóa story cũ hoặc nâng cấp gói.");
        }

        String templateCode = request.getTemplateCode() != null ? request.getTemplateCode() : "minimal-couple";
        String type = request.getType() != null ? request.getType() : "LOVE_STORY";
        templateAccessService.requireUsable(templateCode, type, plan);

        String slug;
        if (request.getSubdomain() != null && !request.getSubdomain().isBlank()) {
            slug = requireAvailableSlug(request.getSubdomain(), null);
        } else {
            slug = generateUniqueSlug(SlugUtil.givenName(request.getCoupleName1()) + " " + SlugUtil.givenName(request.getCoupleName2()));
        }

        Story story = Story.builder()
                .ownerId(ownerId)
                .slug(slug)
                .title(request.getTitle() != null ? request.getTitle() : request.getCoupleName1() + " & " + request.getCoupleName2())
                .status("DRAFT")
                .type(type)
                .planType(plan)
                .templateCode(templateCode)
                .templateConfig("{}")
                .coupleName1(request.getCoupleName1())
                .coupleName2(request.getCoupleName2())
                .startDate(request.getStartDate() != null && !request.getStartDate().isEmpty() ? LocalDate.parse(request.getStartDate()) : null)
                .build();

        story = storyRepository.save(story);
        seedDefaultData(story);
        activityService.record(ownerId, ActivityAction.STORY_CREATED, "STORY", story.getId(), "Đã tạo câu chuyện \"" + coupleNames(story) + "\"");
        return story;
    }

    private void seedDefaultData(Story story) {
        storyEventRepository.save(StoryEvent.builder()
                .storyId(story.getId())
                .title("Ngày đầu tiên gặp nhau")
                .message("Ngày chúng mình gặp nhau...")
                .eventDate(story.getStartDate())
                .sortOrder(0)
                .build());

        storyEventRepository.save(StoryEvent.builder()
                .storyId(story.getId())
                .title("Khoảnh khắc đáng nhớ")
                .message("Một khoảnh khắc mình luôn nhớ...")
                .sortOrder(1)
                .build());

        storyMessageRepository.save(StoryMessage.builder()
                .storyId(story.getId())
                .type("LOVE_LETTER")
                .content("Gửi người thương, cảm ơn vì đã đến bên...")
                .build());
    }

    @Transactional
    public Story updateStory(UUID id, UpdateStoryRequest request, UUID userId) {
        Story story = storyAccessService.requireOwnedStory(id, userId);
        if (request.getCoupleName1() != null) story.setCoupleName1(request.getCoupleName1());
        if (request.getCoupleName2() != null) story.setCoupleName2(request.getCoupleName2());
        if (request.getStartDate() != null && !request.getStartDate().isEmpty()) story.setStartDate(LocalDate.parse(request.getStartDate()));
        if (request.getCoverPhotoId() != null) story.setCoverPhotoId(UUID.fromString(request.getCoverPhotoId()));
        if (request.getSubdomain() != null && !request.getSubdomain().isBlank()
                && !SlugUtil.toSubdomain(request.getSubdomain()).equals(story.getSlug())) {
            if (!"DRAFT".equals(story.getStatus())) {
                throw new IllegalArgumentException("Link đã xuất bản nên không thể thay đổi, vì người khác có thể đã lưu link này.");
            }
            story.setSlug(requireAvailableSlug(request.getSubdomain(), id));
        }
        if (request.getTitle() != null) story.setTitle(request.getTitle());
        if (request.getShortQuote() != null) story.setShortQuote(request.getShortQuote());
        Story saved = storyRepository.save(story);
        notificationService.notifyStorySaved(userId, saved.getId(), coupleNames(saved));
        activityService.record(userId, ActivityAction.STORY_UPDATED, "STORY", saved.getId(), "Đã cập nhật câu chuyện \"" + coupleNames(saved) + "\"");
        return saved;
    }

    @Transactional
    public void deleteStory(UUID id, UUID userId) {
        Story story = storyAccessService.requireOwnedStory(id, userId);
        story.setStatus("DELETED");
        story.setDeletedAt(OffsetDateTime.now());
        storyRepository.save(story);
        activityService.record(userId, ActivityAction.STORY_DELETED, "STORY", id, "Đã xóa câu chuyện \"" + coupleNames(story) + "\"");
    }

    @Transactional
    public Story publishStory(UUID id, UUID userId) {
        Story story = storyAccessService.requireOwnedStory(id, userId);
        int min = templateAccessService.minEventsForPublish(story.getTemplateCode());
        long visible = storyEventRepository.countByStoryIdAndIsVisibleTrue(id);
        if (visible < min) {
            throw new IllegalArgumentException("Cần ít nhất " + min + " kỷ niệm hiển thị để xuất bản (hiện có " + visible + ").");
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (story.getExpiresAt() != null && !story.getExpiresAt().isAfter(now)) {
            throw new IllegalArgumentException("Website đã hết hạn. Vui lòng gia hạn để xuất bản lại.");
        }
        Plan plan = planLimitService.plan(story.getOwnerId());
        story.setStatus("PUBLISHED");
        story.setPublishedAt(now);
        if (plan.getWebsiteDurationDays() == null) {
            story.setExpiresAt(null);
        } else if (story.getExpiresAt() == null) {
            story.setExpiresAt(now.plusDays(plan.getWebsiteDurationDays()));
        }
        Story published = storyRepository.save(story);
        notificationService.notifyStoryPublished(userId, published.getId(), coupleNames(published), published.getSlug());
        activityService.record(userId, ActivityAction.STORY_PUBLISHED, "STORY", published.getId(), "Đã xuất bản website \"" + coupleNames(published) + "\"");
        return published;
    }

    @Transactional
    public Story unpublishStory(UUID id, UUID userId) {
        Story story = storyAccessService.requireOwnedStory(id, userId);
        story.setStatus("DRAFT");
        Story saved = storyRepository.save(story);
        activityService.record(userId, ActivityAction.STORY_UNPUBLISHED, "STORY", saved.getId(), "Đã gỡ website \"" + coupleNames(saved) + "\" về bản nháp");
        return saved;
    }

    @Transactional
    public Story switchTemplate(UUID id, String templateCode, UUID userId) {
        Story story = storyAccessService.requireOwnedStory(id, userId);
        templateAccessService.requireUsable(templateCode, story.getType(), planOf(userId));
        story.setTemplateCode(templateCode);
        story.setTemplateConfig("{}");
        Story saved = storyRepository.save(story);
        activityService.record(userId, ActivityAction.TEMPLATE_CHANGED, "STORY", saved.getId(), "Đã đổi mẫu giao diện sang \"" + templateCode + "\"");
        return saved;
    }

    public Story getPublicStory(String slug) {
        return storyRepository.findBySlugAndStatus(slug, "PUBLISHED")
                .filter(s -> s.getExpiresAt() == null || s.getExpiresAt().isAfter(OffsetDateTime.now()))
                .orElseThrow(() -> new ResourceNotFoundException("Story not found"));
    }

    private static String coupleNames(Story s) {
        return s.getCoupleName1() + " & " + s.getCoupleName2();
    }

    public record SlugCheck(String slug, boolean available, String reason, List<String> suggestions) {}

    /** Checks a desired link. A story's own current link counts as available when excludeStoryId is given. */
    public SlugCheck checkSlug(String raw, UUID excludeStoryId) {
        String slug = SlugUtil.toSubdomain(raw);
        String reason = SlugUtil.validate(slug);
        if (reason == null && isSlugTaken(slug, excludeStoryId)) {
            reason = "Link này đã có người sử dụng.";
        }
        if (reason == null) {
            return new SlugCheck(slug, true, null, List.of());
        }
        return new SlugCheck(slug, false, reason, suggestSlugs(slug, excludeStoryId));
    }

    private boolean isSlugTaken(String slug, UUID excludeStoryId) {
        return excludeStoryId == null
                ? storyRepository.existsBySlugAndStatusNot(slug, "DELETED")
                : storyRepository.existsBySlugAndStatusNotAndIdNot(slug, "DELETED", excludeStoryId);
    }

    private String requireAvailableSlug(String raw, UUID excludeStoryId) {
        String slug = SlugUtil.toSubdomain(raw);
        String invalid = SlugUtil.validate(slug);
        if (invalid != null) throw new IllegalArgumentException(invalid);
        if (isSlugTaken(slug, excludeStoryId)) {
            throw new SlugTakenException("Link '" + slug + "' đã có người sử dụng. Vui lòng chọn link khác.");
        }
        return slug;
    }

    private List<String> suggestSlugs(String base, UUID excludeStoryId) {
        if (base.isEmpty()) base = "my-love-story";
        List<String> candidates = new ArrayList<>();
        candidates.add(withSuffix(base, String.valueOf(Year.now().getValue())));
        for (int i = 1; i <= 20; i++) candidates.add(withSuffix(base, String.valueOf(i)));
        return candidates.stream()
                .filter(c -> SlugUtil.validate(c) == null && !isSlugTaken(c, excludeStoryId))
                .limit(3)
                .toList();
    }

    private String withSuffix(String base, String suffix) {
        return SlugUtil.truncate(base, SlugUtil.MAX_LENGTH - suffix.length() - 1) + "-" + suffix;
    }

    /** Default link from the couple's names; adds -1, -2... when it is taken or not usable. */
    private String generateUniqueSlug(String names) {
        String base = SlugUtil.toSubdomain(names);
        if (base.isEmpty()) base = "my-love-story";
        if (SlugUtil.validate(base) == null && !isSlugTaken(base, null)) return base;
        for (int i = 1; i <= 50; i++) {
            String candidate = withSuffix(base, String.valueOf(i));
            if (SlugUtil.validate(candidate) == null && !isSlugTaken(candidate, null)) return candidate;
        }
        throw new SlugTakenException("Không thể tạo link duy nhất. Vui lòng chọn tên khác.");
    }
}
