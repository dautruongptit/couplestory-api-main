package com.couplestory.service;

import com.couplestory.entity.ActivityAction;
import com.couplestory.entity.UserActivity;
import com.couplestory.repository.UserActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Records the user's activity timeline. Recording is best effort: it runs after the business
 * transaction commits (so rolled-back actions leave nothing) in its own transaction, and a failure
 * is logged, never thrown into the caller.
 */
@Service
public class UserActivityService {

    private static final Logger log = LoggerFactory.getLogger(UserActivityService.class);
    static final int MAX_SUMMARY = 255;
    static final int DEDUPE_MINUTES = 10;
    static final int MAX_PAGE_SIZE = 50;

    private final UserActivityRepository repository;
    private final TransactionTemplate isolated;

    public UserActivityService(UserActivityRepository repository, PlatformTransactionManager txManager) {
        this.repository = repository;
        this.isolated = new TransactionTemplate(txManager);
        this.isolated.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void record(UUID userId, ActivityAction action, String entityType, UUID entityId, String summary) {
        if (userId == null || action == null || summary == null || summary.isBlank()) return;
        String text = summary.length() > MAX_SUMMARY ? summary.substring(0, MAX_SUMMARY) : summary;
        Runnable write = () -> persist(userId, action, entityType, entityId, text);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    write.run();
                }
            });
        } else {
            write.run();
        }
    }

    public Page<UserActivity> list(UUID userId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(Math.max(page, 0), safeSize));
    }

    private void persist(UUID userId, ActivityAction action, String entityType, UUID entityId, String summary) {
        try {
            isolated.executeWithoutResult(status -> {
                if (action.isDeduplicated() && entityId != null && repository.existsByUserIdAndActionAndEntityIdAndCreatedAtAfter(
                        userId, action.name(), entityId, OffsetDateTime.now().minusMinutes(DEDUPE_MINUTES))) {
                    return;
                }
                repository.save(UserActivity.builder()
                        .userId(userId).action(action.name()).entityType(entityType).entityId(entityId).summary(summary).build());
            });
        } catch (RuntimeException e) {
            log.warn("Could not record user activity {}: {}", action, e.getClass().getSimpleName());
        }
    }
}
