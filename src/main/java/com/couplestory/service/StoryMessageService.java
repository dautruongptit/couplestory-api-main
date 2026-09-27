package com.couplestory.service;

import com.couplestory.dto.CreateMessageRequest;
import com.couplestory.entity.StoryMessage;
import com.couplestory.exception.ForbiddenOperationException;
import com.couplestory.exception.ResourceNotFoundException;
import com.couplestory.repository.StoryMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StoryMessageService {
    private final StoryMessageRepository messageRepository;
    private final StoryAccessService storyAccessService;

    public StoryMessageService(StoryMessageRepository messageRepository, StoryAccessService storyAccessService) {
        this.messageRepository = messageRepository;
        this.storyAccessService = storyAccessService;
    }

    public List<StoryMessage> getMessagesByStoryId(UUID storyId, UUID userId) {
        storyAccessService.requireOwnedStory(storyId, userId);
        return messageRepository.findByStoryId(storyId);
    }

    public Optional<StoryMessage> getMessageByType(UUID storyId, String type, UUID userId) {
        storyAccessService.requireOwnedStory(storyId, userId);
        return messageRepository.findFirstByStoryIdAndType(storyId, type);
    }

    @Transactional
    public StoryMessage saveMessage(UUID storyId, CreateMessageRequest request, UUID userId) {
        storyAccessService.requireOwnedStory(storyId, userId);
        Optional<StoryMessage> existing = messageRepository.findFirstByStoryIdAndType(storyId, request.getType());
        StoryMessage message;
        if (existing.isPresent()) {
            message = existing.get();
            message.setContent(request.getContent());
            message.setHeading(request.getHeading());
            message.setSignature(request.getSignature());
        } else {
            message = StoryMessage.builder()
                    .storyId(storyId)
                    .type(request.getType())
                    .content(request.getContent())
                    .heading(request.getHeading())
                    .signature(request.getSignature())
                    .build();
        }
        return messageRepository.save(message);
    }

    @Transactional
    public void deleteMessage(UUID storyId, UUID messageId, UUID userId) {
        storyAccessService.requireOwnedStory(storyId, userId);
        StoryMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        if (!message.getStoryId().equals(storyId)) {
            throw new ForbiddenOperationException("Message does not belong to this story");
        }
        messageRepository.delete(message);
    }
}
