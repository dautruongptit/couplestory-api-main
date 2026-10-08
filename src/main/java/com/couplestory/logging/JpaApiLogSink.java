package com.couplestory.logging;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
class JpaApiLogSink implements ApiLogSink {

    private final ApiLogRepository repository;

    JpaApiLogSink(ApiLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void saveAll(List<ApiLog> batch) {
        repository.saveAll(batch);
    }
}
