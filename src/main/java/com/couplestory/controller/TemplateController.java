package com.couplestory.controller;

import com.couplestory.entity.Template;
import com.couplestory.repository.TemplateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TemplateController {
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
}
