package com.couplestory.logging;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiLogRetentionJobTest {

    // Typed matchers on purpose: with bare any() the compiler picks update(String, Object[], int[]) instead.

    @Test
    void deletesInBatchesUntilABatchComesBackShort() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(contains("DELETE FROM api_logs"), any(OffsetDateTime.class), anyInt())).thenReturn(10_000, 10_000, 7);
        ApiLogRetentionJob job = new ApiLogRetentionJob(jdbc);

        int removed = job.purgeOlderThan(OffsetDateTime.now().minusDays(30));

        assertThat(removed).isEqualTo(20_007);
        verify(jdbc, times(3)).update(contains("DELETE FROM api_logs"), any(OffsetDateTime.class), anyInt());
    }

    @Test
    void thePurgeKeeps30DaysOfLogs() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(contains("DELETE FROM api_logs"), any(OffsetDateTime.class), anyInt())).thenReturn(0);
        ApiLogRetentionJob job = new ApiLogRetentionJob(jdbc);

        job.purge();

        ArgumentCaptor<OffsetDateTime> cutoff = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(jdbc).update(contains("DELETE FROM api_logs"), cutoff.capture(), anyInt());
        OffsetDateTime expected = OffsetDateTime.now().minusDays(30);
        assertThat(ChronoUnit.MINUTES.between(cutoff.getValue(), expected)).isBetween(-1L, 1L);
    }
}
