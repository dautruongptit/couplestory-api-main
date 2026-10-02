package com.couplestory.job;

import com.couplestory.entity.Story;
import com.couplestory.repository.StoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/** Hides published websites whose paid/free period has ended. The owner can still edit and renew them. */
@Slf4j
@Component
public class TrialExpiryJob {
    private final StoryRepository storyRepository;

    public TrialExpiryJob(StoryRepository storyRepository) {
        this.storyRepository = storyRepository;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void checkExpiredTrials() {
        List<Story> expired = storyRepository.findByStatusAndExpiresAtBefore("PUBLISHED", OffsetDateTime.now());
        for (Story story : expired) {
            story.setStatus("HIDDEN");
        }
        storyRepository.saveAll(expired);
        log.info("TrialExpiryJob hid {} expired stories", expired.size());
    }
}
