package com.couplestory.logging;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Hands API log records from the request threads to the database without ever making a request wait:
 * a bounded in-memory queue drained once a second. When the queue is full the record is dropped and
 * counted, because losing a log line is better than slowing the API down.
 */
@Component
public class ApiLogWriter {

    private static final Logger log = LoggerFactory.getLogger(ApiLogWriter.class);
    static final int CAPACITY = 5000;
    static final int BATCH_SIZE = 500;

    private final BlockingQueue<ApiLog> queue;
    private final ApiLogSink sink;
    private final AtomicLong dropped = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();

    @Autowired
    public ApiLogWriter(ApiLogSink sink) {
        this(sink, CAPACITY);
    }

    ApiLogWriter(ApiLogSink sink, int capacity) {
        this.sink = sink;
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    public boolean offer(ApiLog record) {
        if (queue.offer(record)) return true;
        dropped.incrementAndGet();
        return false;
    }

    public long dropped() {
        return dropped.get();
    }

    public long failed() {
        return failed.get();
    }

    @Scheduled(fixedDelay = 1000)
    public void flush() {
        List<ApiLog> batch = new ArrayList<>(BATCH_SIZE);
        while (queue.drainTo(batch, BATCH_SIZE) > 0) {
            try {
                sink.saveAll(batch);
            } catch (RuntimeException e) {
                failed.addAndGet(batch.size());
                log.warn("Could not write {} api log records: {}", batch.size(), e.toString());
            }
            batch = new ArrayList<>(BATCH_SIZE);
        }
    }

    @PreDestroy
    void flushOnShutdown() {
        flush();
    }
}
