package com.pashe.app.blood.request;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BloodRequestStatusScheduler {

    private final BloodRequestRepository repository;

    public BloodRequestStatusScheduler(
            BloodRequestRepository repository) {
        this.repository = repository;
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void reconcileActiveRequests() {
        repository.expireOverdueRequests();
        repository.fulfillCompletedRequests();
    }
}