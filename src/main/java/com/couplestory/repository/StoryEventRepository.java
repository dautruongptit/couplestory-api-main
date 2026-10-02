package com.couplestory.repository;

import com.couplestory.entity.StoryEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StoryEventRepository extends JpaRepository<StoryEvent, UUID> {
    List<StoryEvent> findByStoryIdOrderBySortOrderAsc(UUID storyId);

    long countByStoryIdAndIsVisibleTrue(UUID storyId);
}
