package com.couplestory.service;

import com.couplestory.entity.Website;
import com.couplestory.exception.ForbiddenOperationException;
import com.couplestory.exception.ResourceNotFoundException;
import com.couplestory.repository.WebsiteRepository;
import org.springframework.stereotype.Service;

/**
 * Single point of truth for "does this user own this website" checks.
 * Every service that mutates or reads a website-scoped resource (photos, events,
 * messages, moments, the website itself) must go through here instead of
 * re-implementing the ownership comparison locally.
 *
 * Ownership is the single-owner model (Website.userId). Collaborator/partner access
 * (WebsiteCollaborator, Website.allowPartnerPublish) is defined but not wired into
 * any read/write path in this codebase, so it is intentionally not considered here.
 */
@Service
public class WebsiteAccessService {
    private final WebsiteRepository websiteRepository;

    public WebsiteAccessService(WebsiteRepository websiteRepository) {
        this.websiteRepository = websiteRepository;
    }

    public Website requireOwnedWebsite(String websiteId, String userId) {
        Website website = websiteRepository.findById(websiteId)
                .orElseThrow(() -> new ResourceNotFoundException("Website not found"));
        if (!website.getUserId().equals(userId)) {
            throw new ForbiddenOperationException("You do not have permission to access this website");
        }
        return website;
    }
}
