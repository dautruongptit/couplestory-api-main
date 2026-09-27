package com.couplestory.job;

import com.couplestory.repository.StoryRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TrialExpiryJob {
    private final StoryRepository storyRepository;

    public TrialExpiryJob(StoryRepository storyRepository) {
        this.storyRepository = storyRepository;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void checkExpiredTrials() {
        log.info("Running TrialExpiryJob to check for expired stories...");
    }
}
