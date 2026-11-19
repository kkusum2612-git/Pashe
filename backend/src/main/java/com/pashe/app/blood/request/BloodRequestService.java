package com.pashe.app.blood.request;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BloodRequestService {

    private final BloodRequestRepository repository;

    public BloodRequestService(
            BloodRequestRepository repository) {

        this.repository = repository;
    }

    @Transactional
    public BloodRequest createRequest(
            UUID userId,
            BloodRequestCreateRequest request) {

        if (request.expiresAt() == null
                || !request.expiresAt().isAfter(
                    OffsetDateTime.now())) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Expiration deadline must be in the future"
            );
        }

        return repository.create(userId, request);
    }

    @Transactional
    public BloodRequest getMyRequest(
            UUID userId,
            UUID requestId) {

        repository.expireOverdueRequests();
        repository.fulfillCompletedRequests();

        return repository.findByIdForRequester(
            requestId,
            userId
        ).orElseThrow(() ->
            new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Blood request not found"
            )
        );
    }

    @Transactional
    public List<BloodRequest> getMyRequests(
            UUID userId) {

        repository.expireOverdueRequests();
        repository.fulfillCompletedRequests();

        return repository.findByRequesterUserId(userId);
    }

    @Transactional
    public int expireOverdueRequests() {
        return repository.expireOverdueRequests();
    }
}