package com.couplestory.service;

import com.couplestory.entity.Story;
import com.couplestory.exception.ForbiddenOperationException;
import com.couplestory.exception.ResourceNotFoundException;
import com.couplestory.repository.StoryRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StoryAccessService {
    private final StoryRepository storyRepository;

    public StoryAccessService(StoryRepository storyRepository) {
        this.storyRepository = storyRepository;
    }

    public Story requireOwnedStory(UUID storyId, UUID userId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found"));
        if (!story.getOwnerId().equals(userId)) {
            throw new ForbiddenOperationException("You do not have permission to access this story");
        }
        return story;
    }
}
