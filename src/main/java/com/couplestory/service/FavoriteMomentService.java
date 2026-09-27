package com.couplestory.service;

import com.couplestory.dto.CreateMomentRequest;
import com.couplestory.entity.FavoriteMoment;
import com.couplestory.exception.ForbiddenOperationException;
import com.couplestory.exception.ResourceNotFoundException;
import com.couplestory.repository.FavoriteMomentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FavoriteMomentService {
    private final FavoriteMomentRepository momentRepository;
    private final StoryAccessService storyAccessService;

    public FavoriteMomentService(FavoriteMomentRepository momentRepository, StoryAccessService storyAccessService) {
        this.momentRepository = momentRepository;
        this.storyAccessService = storyAccessService;
    }

    public List<FavoriteMoment> getMomentsByStoryId(UUID storyId, UUID userId) {
        storyAccessService.requireOwnedStory(storyId, userId);
        return momentRepository.findByStoryIdOrderBySortOrderAsc(storyId);
    }

    @Transactional
    public FavoriteMoment createMoment(UUID storyId, CreateMomentRequest request, UUID userId) {
        storyAccessService.requireOwnedStory(storyId, userId);
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        FavoriteMoment moment = FavoriteMoment.builder()
                .storyId(storyId)
                .title(request.getTitle())
                .description(request.getDescription())
                .photoId(request.getPhotoId() != null ? UUID.fromString(request.getPhotoId()) : null)
                .sortOrder(request.getOrder() != null ? request.getOrder() : 0)
                .build();
        return momentRepository.save(moment);
    }

    @Transactional
    public FavoriteMoment updateMoment(UUID storyId, UUID momentId, CreateMomentRequest request, UUID userId) {
        storyAccessService.requireOwnedStory(storyId, userId);
        FavoriteMoment moment = requireMomentInStory(storyId, momentId);
        if (request.getTitle() != null) moment.setTitle(request.getTitle());
        if (request.getDescription() != null) moment.setDescription(request.getDescription());
        if (request.getPhotoId() != null) moment.setPhotoId(UUID.fromString(request.getPhotoId()));
        if (request.getOrder() != null) moment.setSortOrder(request.getOrder());
        return momentRepository.save(moment);
    }

    @Transactional
    public void deleteMoment(UUID storyId, UUID momentId, UUID userId) {
        storyAccessService.requireOwnedStory(storyId, userId);
        FavoriteMoment moment = requireMomentInStory(storyId, momentId);
        momentRepository.delete(moment);
    }

    private FavoriteMoment requireMomentInStory(UUID storyId, UUID momentId) {
        FavoriteMoment moment = momentRepository.findById(momentId)
                .orElseThrow(() -> new ResourceNotFoundException("Moment not found"));
        if (!moment.getStoryId().equals(storyId)) {
            throw new ForbiddenOperationException("Moment does not belong to this story");
        }
        return moment;
    }
}
