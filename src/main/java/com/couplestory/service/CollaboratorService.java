package com.couplestory.service;

import com.couplestory.entity.StoryCollaborator;
import com.couplestory.exception.ForbiddenOperationException;
import com.couplestory.exception.ResourceNotFoundException;
import com.couplestory.repository.StoryCollaboratorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollaboratorService {

    private final StoryCollaboratorRepository collaboratorRepository;
    private final StoryAccessService storyAccessService;

    public StoryCollaborator invitePartner(UUID storyId, UUID inviterId, String email) {
        storyAccessService.requireOwnedStory(storyId, inviterId);

        String rawToken = UUID.randomUUID().toString();
        String tokenHash = sha256(rawToken);

        StoryCollaborator collaborator = StoryCollaborator.builder()
                .storyId(storyId)
                .email(email.toLowerCase())
                .role("PARTNER")
                .status("PENDING")
                .inviteTokenHash(tokenHash)
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .build();

        StoryCollaborator saved = collaboratorRepository.save(collaborator);
        log.info("Invite sent to {} for story {} (token not logged)", email, storyId);
        return saved;
    }

    public StoryCollaborator acceptInvite(String rawToken, UUID userId, String userEmail) {
        String tokenHash = sha256(rawToken);
        StoryCollaborator collaborator = collaboratorRepository.findByInviteTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invite token"));

        if (!"PENDING".equals(collaborator.getStatus())) {
            throw new IllegalArgumentException("Invite is not pending");
        }
        if (collaborator.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Invite has expired");
        }
        if (!collaborator.getEmail().equalsIgnoreCase(userEmail)) {
            throw new IllegalArgumentException("Email does not match the invited email");
        }

        collaborator.setStatus("ACCEPTED");
        collaborator.setAcceptedAt(OffsetDateTime.now());
        collaborator.setUserId(userId);
        return collaboratorRepository.save(collaborator);
    }

    public List<StoryCollaborator> getCollaborators(UUID storyId, UUID userId) {
        storyAccessService.requireOwnedStory(storyId, userId);
        return collaboratorRepository.findByStoryId(storyId);
    }

    public void removePartner(UUID storyId, UUID ownerId, UUID collaboratorId) {
        storyAccessService.requireOwnedStory(storyId, ownerId);
        StoryCollaborator collaborator = collaboratorRepository.findById(collaboratorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaborator not found"));
        if (!collaborator.getStoryId().equals(storyId)) {
            throw new ForbiddenOperationException("Collaborator does not belong to this story");
        }
        collaborator.setStatus("REMOVED");
        collaborator.setRemovedAt(OffsetDateTime.now());
        collaboratorRepository.save(collaborator);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
