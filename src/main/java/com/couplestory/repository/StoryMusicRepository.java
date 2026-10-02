package com.couplestory.repository;

import com.couplestory.entity.StoryMusic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StoryMusicRepository extends JpaRepository<StoryMusic, UUID> {

    List<StoryMusic> findByStoryIdOrderBySortOrderAsc(UUID storyId);

    void deleteByStoryId(UUID storyId);
}
