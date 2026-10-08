package com.couplestory.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/** Keeps api_logs to the last 30 days, deleting in batches so the table is never locked for long. */
@Component
public class ApiLogRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(ApiLogRetentionJob.class);
    static final int RETENTION_DAYS = 30;
    static final int BATCH = 10_000;

    private final JdbcTemplate jdbc;

    public ApiLogRetentionJob(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Scheduled(cron = "0 30 3 * * *")
    public void purge() {
        int removed = purgeOlderThan(OffsetDateTime.now().minusDays(RETENTION_DAYS));
        if (removed > 0) log.info("Purged {} api log rows older than {} days", removed, RETENTION_DAYS);
    }

    int purgeOlderThan(OffsetDateTime cutoff) {
        int total = 0;
        int deleted;
        do {
            deleted = jdbc.update(
                    "DELETE FROM api_logs WHERE id IN (SELECT id FROM api_logs WHERE created_at < ? ORDER BY id LIMIT ?)",
                    cutoff, BATCH);
            total += deleted;
        } while (deleted == BATCH);
        return total;
    }
}
