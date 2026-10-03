package com.couplestory.service;

import com.couplestory.entity.Notification;
import com.couplestory.exception.ForbiddenOperationException;
import com.couplestory.exception.ResourceNotFoundException;
import com.couplestory.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public long countUnread(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    public List<Notification> getNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void markAsRead(UUID id, UUID userId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getUserId().equals(userId)) {
            throw new ForbiddenOperationException("This notification does not belong to you");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    public void markAllAsRead(UUID userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalse(userId);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    static final String STORY_TYPE = "STORY";
    static final String SAVED_TITLE = "Đã lưu thay đổi";
    static final String PUBLISHED_TITLE = "Đã xuất bản website";
    private static final long SAVE_NOTIFICATION_WINDOW_MINUTES = 30;
    private static final long PUBLISH_MERGE_WINDOW_MINUTES = 2;

    /** One notification per story per 30 minutes, so repeated saves do not flood the list. */
    public void notifyStorySaved(UUID userId, UUID storyId, String coupleNames) {
        OffsetDateTime since = OffsetDateTime.now().minusMinutes(SAVE_NOTIFICATION_WINDOW_MINUTES);
        if (notificationRepository.existsByUserIdAndRelatedStoryIdAndTypeAndTitleAndCreatedAtAfter(userId, storyId, STORY_TYPE, SAVED_TITLE, since)) {
            return;
        }
        createNotification(userId, SAVED_TITLE, "Các thay đổi của câu chuyện " + coupleNames + " đã được lưu.", STORY_TYPE, storyId);
    }

    /** Publishing from the editor saves first; drop that just-created save notification so only one shows. */
    public void notifyStoryPublished(UUID userId, UUID storyId, String coupleNames, String slug) {
        OffsetDateTime since = OffsetDateTime.now().minusMinutes(PUBLISH_MERGE_WINDOW_MINUTES);
        notificationRepository.deleteByUserIdAndRelatedStoryIdAndTypeAndTitleAndCreatedAtAfter(userId, storyId, STORY_TYPE, SAVED_TITLE, since);
        createNotification(userId, PUBLISHED_TITLE, "Website " + coupleNames + " đã hoạt động tại " + slug + ".couplestory.site.", STORY_TYPE, storyId);
    }

    public Notification createNotification(UUID userId, String title, String content, String type, UUID relatedStoryId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .content(content)
                .type(type)
                .relatedStoryId(relatedStoryId)
                .read(false)
                .build();
        return notificationRepository.save(notification);
    }
}
