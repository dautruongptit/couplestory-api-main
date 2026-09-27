package com.couplestory.controller;

import com.couplestory.dto.CreateMessageRequest;
import com.couplestory.entity.WebsiteMessage;
import com.couplestory.security.UserDetailsImpl;
import com.couplestory.service.WebsiteMessageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stories/{websiteId}/messages")
public class WebsiteMessageController {
    private final WebsiteMessageService messageService;

    public WebsiteMessageController(WebsiteMessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public ResponseEntity<List<WebsiteMessage>> getMessages(
            @PathVariable String websiteId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(messageService.getMessagesByWebsiteId(websiteId, userDetails.getId()));
    }

    @GetMapping("/{type}")
    public ResponseEntity<WebsiteMessage> getMessageByType(
            @PathVariable String websiteId,
            @PathVariable String type,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return messageService.getMessageByType(websiteId, type, userDetails.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<WebsiteMessage> saveMessage(
            @PathVariable String websiteId,
            @Valid @RequestBody CreateMessageRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(messageService.saveMessage(websiteId, request, userDetails.getId()));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable String websiteId,
            @PathVariable String messageId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        messageService.deleteMessage(websiteId, messageId, userDetails.getId());
        return ResponseEntity.ok().build();
    }
}
