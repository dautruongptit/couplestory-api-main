package com.couplestory.service;

import com.couplestory.dto.CreateStoryRequest;
import com.couplestory.dto.UpdateStoryRequest;
import com.couplestory.entity.Photo;
import com.couplestory.entity.Website;
import com.couplestory.entity.WebsiteEvent;
import com.couplestory.entity.WebsiteMessage;
import com.couplestory.exception.ResourceNotFoundException;
import com.couplestory.repository.PhotoRepository;
import com.couplestory.repository.WebsiteEventRepository;
import com.couplestory.repository.WebsiteMessageRepository;
import com.couplestory.repository.WebsiteRepository;
import com.couplestory.util.SlugUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WebsiteService {
    private final WebsiteRepository websiteRepository;
    private final PhotoRepository photoRepository;
    private final WebsiteEventRepository websiteEventRepository;
    private final WebsiteMessageRepository websiteMessageRepository;
    private final WebsiteAccessService websiteAccessService;

    public WebsiteService(WebsiteRepository websiteRepository,
                          PhotoRepository photoRepository,
                          WebsiteEventRepository websiteEventRepository,
                          WebsiteMessageRepository websiteMessageRepository,
                          WebsiteAccessService websiteAccessService) {
        this.websiteRepository = websiteRepository;
        this.photoRepository = photoRepository;
        this.websiteEventRepository = websiteEventRepository;
        this.websiteMessageRepository = websiteMessageRepository;
        this.websiteAccessService = websiteAccessService;
    }

    public List<Website> getWebsitesByUserId(String userId) {
        return websiteRepository.findByUserIdAndStatusNot(userId, "deleted");
    }

    /** Owner-only lookup: draft/unpublished story data is not public. Public viewing goes through getPublicStory(). */
    public Website getOwnedWebsiteById(String id, String userId) {
        return websiteAccessService.requireOwnedWebsite(id, userId);
    }

    @Transactional
    public Website createWebsite(String userId, CreateStoryRequest request) {
        // Quota check
        long count = websiteRepository.countByUserIdAndStatusNot(userId, "deleted");
        if (count >= 3) {
            throw new RuntimeException("Bạn đã đạt giới hạn tối đa 3 website. Vui lòng xóa website cũ để tạo mới.");
        }

        // Generate unique slug
        String baseSlug = SlugUtil.toSlug(request.getSubdomain() != null ? request.getSubdomain() : 
                request.getCoupleName1() + " " + request.getCoupleName2());
        String slug = generateUniqueSlug(baseSlug);

        Website website = Website.builder()
                .userId(userId)
                .subdomain(slug)
                .status("draft")
                .planType("TRIAL")
                .templateCode(request.getTemplateCode() != null ? request.getTemplateCode() : "minimal-couple")
                .templateConfig("{}")
                .coupleName1(request.getCoupleName1())
                .coupleName2(request.getCoupleName2())
                .title(request.getTitle())
                .startDate(request.getStartDate() != null ? LocalDate.parse(request.getStartDate()) : null)
                .build();

        website = websiteRepository.save(website);
        seedDefaultData(website);
        return website;
    }

    private void seedDefaultData(Website website) {
        Photo cover = photoRepository.save(Photo.builder()
                .ownerType("WEBSITE")
                .ownerId(website.getId())
                .url("https://images.unsplash.com/photo-1518199266791-5375a83190b7?auto=format&fit=crop&w=1200&q=80")
                .sortOrder(0)
                .build());

        photoRepository.save(Photo.builder()
                .ownerType("WEBSITE")
                .ownerId(website.getId())
                .url("https://api.dicebear.com/7.x/notionists/svg?seed=A")
                .sortOrder(1)
                .build());

        photoRepository.save(Photo.builder()
                .ownerType("WEBSITE")
                .ownerId(website.getId())
                .url("https://api.dicebear.com/7.x/notionists/svg?seed=B")
                .sortOrder(2)
                .build());

        Photo timeline1 = photoRepository.save(Photo.builder()
                .ownerType("WEBSITE")
                .ownerId(website.getId())
                .url("https://images.unsplash.com/photo-1522673607200-164d1b6ce486?auto=format&fit=crop&w=800&q=80")
                .sortOrder(3)
                .build());

        Photo timeline2 = photoRepository.save(Photo.builder()
                .ownerType("WEBSITE")
                .ownerId(website.getId())
                .url("https://images.unsplash.com/photo-1515934751635-c81c6bc9a2d8?auto=format&fit=crop&w=800&q=80")
                .sortOrder(4)
                .build());

        website.setCoverPhotoId(cover.getId());
        websiteRepository.save(website);

        websiteEventRepository.save(WebsiteEvent.builder()
                .websiteId(website.getId())
                .title("Ngày đầu tiên gặp nhau")
                .photoId(timeline1.getId())
                .sortOrder(0)
                .build());

        websiteEventRepository.save(WebsiteEvent.builder()
                .websiteId(website.getId())
                .title("Khoảnh khắc đáng nhớ")
                .photoId(timeline2.getId())
                .sortOrder(1)
                .build());

        websiteMessageRepository.save(WebsiteMessage.builder()
                .websiteId(website.getId())
                .type("love_letter")
                .content("Gửi người thương, cảm ơn vì đã đến bên...")
                .build());
    }

    @Transactional
    public Website updateWebsite(String id, UpdateStoryRequest request, String userId) {
        Website website = websiteAccessService.requireOwnedWebsite(id, userId);
        if (request.getCoupleName1() != null) website.setCoupleName1(request.getCoupleName1());
        if (request.getCoupleName2() != null) website.setCoupleName2(request.getCoupleName2());
        if (request.getStartDate() != null) website.setStartDate(LocalDate.parse(request.getStartDate()));
        if (request.getCoverPhotoId() != null) website.setCoverPhotoId(request.getCoverPhotoId());
        if (request.getSubdomain() != null) {
            String newSlug = SlugUtil.toSlug(request.getSubdomain());
            // Check uniqueness (exclude current website)
            Optional<Website> existing = websiteRepository.findBySubdomain(newSlug);
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new RuntimeException("Subdomain '" + newSlug + "' đã được sử dụng.");
            }
            website.setSubdomain(newSlug);
        }
        if (request.getTitle() != null) website.setTitle(request.getTitle());
        if (request.getShortQuote() != null) website.setShortQuote(request.getShortQuote());
        return websiteRepository.save(website);
    }

    @Transactional
    public void deleteWebsite(String id, String userId) {
        Website website = websiteAccessService.requireOwnedWebsite(id, userId);
        website.setStatus("deleted");
        websiteRepository.save(website);
    }

    @Transactional
    public Website publishWebsite(String id, String userId) {
        Website website = websiteAccessService.requireOwnedWebsite(id, userId);
        website.setStatus("published");
        website.setPublishedAt(LocalDateTime.now());
        if ("TRIAL".equals(website.getPlanType())) {
            website.setExpiresAt(LocalDateTime.now().plusDays(7));
        }
        return websiteRepository.save(website);
    }

    @Transactional
    public Website unpublishWebsite(String id, String userId) {
        Website website = websiteAccessService.requireOwnedWebsite(id, userId);
        website.setStatus("draft");
        return websiteRepository.save(website);
    }

    @Transactional
    public Website switchTemplate(String id, String templateCode, String userId) {
        Website website = websiteAccessService.requireOwnedWebsite(id, userId);
        website.setTemplateCode(templateCode);
        website.setTemplateConfig("{}"); // Reset config
        return websiteRepository.save(website);
    }

    public Website getPublicStory(String subdomain) {
        return websiteRepository.findBySubdomainAndStatus(subdomain, "published")
                .orElseThrow(() -> new ResourceNotFoundException("Story not found"));
    }

    private String generateUniqueSlug(String baseSlug) {
        if (baseSlug.isEmpty()) baseSlug = "my-love-story";
        String slug = baseSlug;
        for (int i = 1; i <= 10; i++) {
            if (websiteRepository.findBySubdomain(slug).isEmpty()) {
                return slug;
            }
            slug = baseSlug + "-" + i;
        }
        throw new RuntimeException("Không thể tạo slug duy nhất. Vui lòng chọn tên khác.");
    }
}
