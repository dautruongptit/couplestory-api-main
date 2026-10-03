package com.couplestory.controller;

import com.couplestory.entity.Template;
import com.couplestory.repository.TemplateRepository;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class TemplateController {
    private static final int MAX_PAGE_SIZE = 24;

    private final TemplateRepository templateRepository;

    public TemplateController(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @GetMapping("/api/templates")
    public ResponseEntity<List<Template>> list(@RequestParam(required = false) String type) {
        List<Template> templates = templateRepository.findByIsActiveTrueOrderBySortOrder().stream()
                .filter(t -> type == null || type.equals(t.getType()))
                .toList();
        return ResponseEntity.ok(templates);
    }

    public static class ExploreItem {
        @JsonUnwrapped
        private final Template template;
        private final long usageCount;

        ExploreItem(Template template, long usageCount) {
            this.template = template;
            this.usageCount = usageCount;
        }

        public Template getTemplate() { return template; }
        public long getUsageCount() { return usageCount; }
    }

    @GetMapping("/api/templates/explore")
    public ResponseEntity<Map<String, Object>> explore(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "popular") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        String keyword = q == null || q.isBlank()
                ? "%"
                : "%" + q.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        Page<Object[]> result = "newest".equals(sort)
                ? templateRepository.exploreNewest(type, keyword, pageable)
                : templateRepository.explorePopular(type, keyword, pageable);
        List<ExploreItem> items = result.getContent().stream()
                .map(row -> new ExploreItem((Template) row[0], ((Number) row[1]).longValue()))
                .toList();
        return ResponseEntity.ok(Map.of(
                "items", items,
                "page", result.getNumber(),
                "hasMore", result.hasNext(),
                "total", result.getTotalElements()));
    }
}
