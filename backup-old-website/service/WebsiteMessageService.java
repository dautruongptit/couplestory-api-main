package com.couplestory.service;

import com.couplestory.dto.CreateMessageRequest;
import com.couplestory.entity.WebsiteMessage;
import com.couplestory.exception.ForbiddenOperationException;
import com.couplestory.exception.ResourceNotFoundException;
import com.couplestory.repository.WebsiteMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class WebsiteMessageService {
    private final WebsiteMessageRepository messageRepository;
    private final WebsiteAccessService websiteAccessService;

    public WebsiteMessageService(WebsiteMessageRepository messageRepository, WebsiteAccessService websiteAccessService) {
        this.messageRepository = messageRepository;
        this.websiteAccessService = websiteAccessService;
    }

    public List<WebsiteMessage> getMessagesByWebsiteId(String websiteId, String userId) {
        websiteAccessService.requireOwnedWebsite(websiteId, userId);
        return messageRepository.findByWebsiteId(websiteId);
    }

    public Optional<WebsiteMessage> getMessageByType(String websiteId, String type, String userId) {
        websiteAccessService.requireOwnedWebsite(websiteId, userId);
        List<WebsiteMessage> messages = messageRepository.findByWebsiteIdAndType(websiteId, type);
        return messages.isEmpty() ? Optional.empty() : Optional.of(messages.get(0));
    }

    @Transactional
    public WebsiteMessage saveMessage(String websiteId, CreateMessageRequest request, String userId) {
        websiteAccessService.requireOwnedWebsite(websiteId, userId);
        // Upsert: if a message of this type already exists, update it; otherwise create new
        List<WebsiteMessage> existing = messageRepository.findByWebsiteIdAndType(websiteId, request.getType());
        WebsiteMessage message;
        if (!existing.isEmpty()) {
            message = existing.get(0);
            message.setContent(request.getContent());
            message.setHeading(request.getHeading());
            message.setSignature(request.getSignature());
        } else {
            message = WebsiteMessage.builder()
                    .websiteId(websiteId)
                    .type(request.getType())
                    .content(request.getContent())
                    .heading(request.getHeading())
                    .signature(request.getSignature())
                    .build();
        }
        return messageRepository.save(message);
    }

    @Transactional
    public void deleteMessage(String websiteId, String messageId, String userId) {
        websiteAccessService.requireOwnedWebsite(websiteId, userId);
        WebsiteMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        if (!message.getWebsiteId().equals(websiteId)) {
            throw new ForbiddenOperationException("Message does not belong to this website");
        }
        messageRepository.delete(message);
    }
}
