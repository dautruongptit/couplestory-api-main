package com.couplestory.logging;

import java.util.List;

/** Where a batch of API log records ends up. A seam so the writer can be tested without a database. */
@FunctionalInterface
public interface ApiLogSink {
    void saveAll(List<ApiLog> batch);
}
