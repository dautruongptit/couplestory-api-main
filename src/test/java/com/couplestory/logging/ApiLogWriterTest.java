package com.couplestory.logging;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ApiLogWriterTest {

    private static ApiLog log(String id) {
        return ApiLog.builder().requestId(id).method("GET").route("/api/x").status((short) 200)
                .durationMs(1).createdAt(OffsetDateTime.now()).build();
    }

    @Test
    void flushWritesTheQueuedRecordsInOrder() {
        List<ApiLog> saved = new ArrayList<>();
        ApiLogWriter writer = new ApiLogWriter(saved::addAll, 10);

        writer.offer(log("a"));
        writer.offer(log("b"));
        writer.flush();

        assertThat(saved).extracting(ApiLog::getRequestId).containsExactly("a", "b");
    }

    @Test
    void largeBacklogsAreWrittenInBatchesOf500() {
        List<Integer> batchSizes = new ArrayList<>();
        ApiLogWriter writer = new ApiLogWriter(batch -> batchSizes.add(batch.size()), 2000);

        for (int i = 0; i < 1200; i++) writer.offer(log("r" + i));
        writer.flush();

        assertThat(batchSizes).containsExactly(500, 500, 200);
    }

    @Test
    void offerNeverBlocksAndCountsRecordsDroppedWhenTheQueueIsFull() {
        ApiLogWriter writer = new ApiLogWriter(batch -> { }, 2);

        assertThat(writer.offer(log("1"))).isTrue();
        assertThat(writer.offer(log("2"))).isTrue();
        assertThat(writer.offer(log("3"))).isFalse();

        assertThat(writer.dropped()).isEqualTo(1);
    }

    @Test
    void aFailingSinkDoesNotBreakTheSchedulerAndLaterRecordsStillFlush() {
        List<ApiLog> saved = new ArrayList<>();
        boolean[] fail = {true};
        ApiLogWriter writer = new ApiLogWriter(batch -> {
            if (fail[0]) throw new IllegalStateException("database down");
            saved.addAll(batch);
        }, 10);

        writer.offer(log("lost"));
        assertThatCode(writer::flush).doesNotThrowAnyException();

        fail[0] = false;
        writer.offer(log("kept"));
        writer.flush();

        assertThat(saved).extracting(ApiLog::getRequestId).containsExactly("kept");
        assertThat(writer.failed()).isEqualTo(1);
    }
}
