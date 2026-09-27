package com.couplestory.repository;

import com.couplestory.entity.StoryCollaborator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoryCollaboratorRepository extends JpaRepository<StoryCollaborator, UUID> {
    List<StoryCollaborator> findByStoryId(UUID storyId);
    boolean existsByStoryIdAndUserIdAndStatus(UUID storyId, UUID userId, String status);
    List<StoryCollaborator> findByStoryIdAndStatus(UUID storyId, String status);
    Optional<StoryCollaborator> findByInviteTokenHash(String tokenHash);
    List<StoryCollaborator> findByEmailAndStatus(String email, String status);
}
