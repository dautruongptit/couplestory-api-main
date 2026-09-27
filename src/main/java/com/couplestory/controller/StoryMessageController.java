package com.couplestory.controller;

import com.couplestory.dto.CreateMessageRequest;
import com.couplestory.entity.StoryMessage;
import com.couplestory.security.UserDetailsImpl;
import com.couplestory.service.StoryMessageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stories/{storyId}/messages")
public class StoryMessageController {
    private final StoryMessageService messageService;

    public StoryMessageController(StoryMessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public ResponseEntity<List<StoryMessage>> getMessages(
            @PathVariable UUID storyId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(messageService.getMessagesByStoryId(storyId, userDetails.getId()));
    }

    @GetMapping("/{type}")
    public ResponseEntity<StoryMessage> getMessageByType(
            @PathVariable UUID storyId,
            @PathVariable String type,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return messageService.getMessageByType(storyId, type, userDetails.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<StoryMessage> saveMessage(
            @PathVariable UUID storyId,
            @Valid @RequestBody CreateMessageRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(messageService.saveMessage(storyId, request, userDetails.getId()));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable UUID storyId,
            @PathVariable UUID messageId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        messageService.deleteMessage(storyId, messageId, userDetails.getId());
        return ResponseEntity.ok().build();
    }
}
