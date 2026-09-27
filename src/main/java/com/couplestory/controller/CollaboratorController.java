package com.couplestory.controller;

import com.couplestory.dto.InvitePartnerRequest;
import com.couplestory.entity.StoryCollaborator;
import com.couplestory.security.UserDetailsImpl;
import com.couplestory.service.CollaboratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CollaboratorController {

    private final CollaboratorService collaboratorService;

    @PostMapping("/stories/{storyId}/collaborators/invite")
    public ResponseEntity<StoryCollaborator> invitePartner(
            @PathVariable UUID storyId,
            @Valid @RequestBody InvitePartnerRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        StoryCollaborator collaborator = collaboratorService.invitePartner(
                storyId, userDetails.getId(), request.getEmail());
        return ResponseEntity.ok(collaborator);
    }

    @PostMapping("/collaborators/accept")
    public ResponseEntity<StoryCollaborator> acceptInvite(
            @RequestParam String token,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        StoryCollaborator collaborator = collaboratorService.acceptInvite(
                token, userDetails.getId(), userDetails.getEmail());
        return ResponseEntity.ok(collaborator);
    }

    @GetMapping("/stories/{storyId}/collaborators")
    public ResponseEntity<List<StoryCollaborator>> getCollaborators(
            @PathVariable UUID storyId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<StoryCollaborator> collaborators = collaboratorService.getCollaborators(storyId, userDetails.getId());
        return ResponseEntity.ok(collaborators);
    }

    @DeleteMapping("/stories/{storyId}/collaborators/{collaboratorId}")
    public ResponseEntity<Void> removePartner(
            @PathVariable UUID storyId,
            @PathVariable UUID collaboratorId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        collaboratorService.removePartner(storyId, userDetails.getId(), collaboratorId);
        return ResponseEntity.ok().build();
    }
}
