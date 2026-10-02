package com.couplestory.service;

import com.couplestory.entity.Template;
import com.couplestory.exception.ForbiddenOperationException;
import com.couplestory.repository.TemplateRepository;
import com.couplestory.util.PackageRank;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TemplateAccessService {
    public static final int DEFAULT_MAX_DISPLAY_EVENTS = 6;
    public static final int DEFAULT_MIN_EVENTS_FOR_PUBLISH = 2;

    private final TemplateRepository templateRepository;

    public TemplateAccessService(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    /** A template must exist, be active, match the story type, and be covered by the user's plan. */
    public Template requireUsable(String templateCode, String storyType, String userPlan) {
        Template template = templateRepository.findByCode(templateCode)
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .orElseThrow(() -> new IllegalArgumentException("Template không tồn tại hoặc chưa mở: " + templateCode));
        if (!template.getType().equals(storyType)) {
            throw new IllegalArgumentException("Template này thuộc loại " + template.getType()
                    + ", không dùng được cho " + storyType + ".");
        }
        if (!PackageRank.covers(userPlan, template.getPackageCode())) {
            throw new ForbiddenOperationException("Template này cần gói " + template.getPackageCode()
                    + " trở lên. Vui lòng nâng cấp gói.");
        }
        return template;
    }

    /** Legacy stories may reference codes that are not in the catalog; those fall back to defaults. */
    public Optional<Template> find(String templateCode) {
        return templateRepository.findByCode(templateCode);
    }

    public int maxDisplayEvents(String templateCode) {
        return find(templateCode).map(Template::getMaxDisplayEvents).orElse(DEFAULT_MAX_DISPLAY_EVENTS);
    }

    public int minEventsForPublish(String templateCode) {
        return find(templateCode).map(Template::getMinEventsForPublish).orElse(DEFAULT_MIN_EVENTS_FOR_PUBLISH);
    }
}
