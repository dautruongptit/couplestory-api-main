package com.couplestory.repository;

import com.couplestory.entity.StoryMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoryMessageRepository extends JpaRepository<StoryMessage, UUID> {
    List<StoryMessage> findByStoryId(UUID storyId);
    List<StoryMessage> findByStoryIdAndType(UUID storyId, String type);
    Optional<StoryMessage> findByStoryIdAndType(UUID storyId, String type, String id);
    Optional<StoryMessage> findFirstByStoryIdAndType(UUID storyId, String type);
}
