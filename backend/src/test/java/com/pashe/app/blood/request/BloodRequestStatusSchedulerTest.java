package com.pashe.app.blood.request;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.*;

class BloodRequestStatusSchedulerTest {

    @Test
    void reconcilesExpiredAndCompletedRequests() {
        BloodRequestRepository repository =
            mock(BloodRequestRepository.class);

        BloodRequestStatusScheduler scheduler =
            new BloodRequestStatusScheduler(repository);

        scheduler.reconcileActiveRequests();

        InOrder order = inOrder(repository);

        order.verify(repository).expireOverdueRequests();
        order.verify(repository).fulfillCompletedRequests();
    }
}