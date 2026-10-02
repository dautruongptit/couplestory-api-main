package com.couplestory.service;

import com.couplestory.dto.CreateStoryRequest;
import com.couplestory.dto.UpdateStoryRequest;
import com.couplestory.entity.Plan;
import com.couplestory.entity.Story;
import com.couplestory.entity.StoryEvent;
import com.couplestory.entity.StoryMessage;
import com.couplestory.exception.ResourceNotFoundException;
import com.couplestory.repository.StoryEventRepository;
import com.couplestory.repository.StoryMessageRepository;
import com.couplestory.repository.StoryRepository;
import com.couplestory.util.SlugUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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

    public StoryService(StoryRepository storyRepository,
                        StoryEventRepository storyEventRepository,
                        StoryMessageRepository storyMessageRepository,
                        StoryAccessService storyAccessService,
                        TemplateAccessService templateAccessService,
                        PlanLimitService planLimitService) {
        this.storyRepository = storyRepository;
        this.storyEventRepository = storyEventRepository;
        this.storyMessageRepository = storyMessageRepository;
        this.storyAccessService = storyAccessService;
        this.templateAccessService = templateAccessService;
        this.planLimitService = planLimitService;
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

        String baseSlug = SlugUtil.toSlug(request.getSubdomain() != null ? request.getSubdomain() :
                request.getCoupleName1() + " " + request.getCoupleName2());
        String slug = generateUniqueSlug(baseSlug);

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
        if (request.getSubdomain() != null) {
            String newSlug = SlugUtil.toSlug(request.getSubdomain());
            Optional<Story> existing = storyRepository.findBySlug(newSlug);
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new RuntimeException("Slug '" + newSlug + "' đã được sử dụng.");
            }
            story.setSlug(newSlug);
        }
        if (request.getTitle() != null) story.setTitle(request.getTitle());
        if (request.getShortQuote() != null) story.setShortQuote(request.getShortQuote());
        return storyRepository.save(story);
    }

    @Transactional
    public void deleteStory(UUID id, UUID userId) {
        Story story = storyAccessService.requireOwnedStory(id, userId);
        story.setStatus("DELETED");
        story.setDeletedAt(OffsetDateTime.now());
        storyRepository.save(story);
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
        return storyRepository.save(story);
    }

    @Transactional
    public Story unpublishStory(UUID id, UUID userId) {
        Story story = storyAccessService.requireOwnedStory(id, userId);
        story.setStatus("DRAFT");
        return storyRepository.save(story);
    }

    @Transactional
    public Story switchTemplate(UUID id, String templateCode, UUID userId) {
        Story story = storyAccessService.requireOwnedStory(id, userId);
        templateAccessService.requireUsable(templateCode, story.getType(), planOf(userId));
        story.setTemplateCode(templateCode);
        story.setTemplateConfig("{}");
        return storyRepository.save(story);
    }

    public Story getPublicStory(String slug) {
        return storyRepository.findBySlugAndStatus(slug, "PUBLISHED")
                .filter(s -> s.getExpiresAt() == null || s.getExpiresAt().isAfter(OffsetDateTime.now()))
                .orElseThrow(() -> new ResourceNotFoundException("Story not found"));
    }

    private String generateUniqueSlug(String baseSlug) {
        if (baseSlug.isEmpty()) baseSlug = "my-love-story";
        String slug = baseSlug;
        for (int i = 1; i <= 10; i++) {
            if (storyRepository.findBySlug(slug).isEmpty()) {
                return slug;
            }
            slug = baseSlug + "-" + i;
        }
        throw new RuntimeException("Không thể tạo slug duy nhất. Vui lòng chọn tên khác.");
    }
}
