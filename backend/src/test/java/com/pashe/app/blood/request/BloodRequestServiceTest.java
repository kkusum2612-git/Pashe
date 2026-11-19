package com.pashe.app.blood.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BloodRequestServiceTest {

    private BloodRequestRepository repository;
    private BloodRequestService service;

    @BeforeEach
    void setup() {
        repository = mock(BloodRequestRepository.class);
        service = new BloodRequestService(repository);
    }

    @Test
    void createsValidBloodRequest() {
        UUID userId = UUID.randomUUID();

        BloodRequestCreateRequest request =
            new BloodRequestCreateRequest(
                "B+",
                3,
                "Dhaka Medical College Hospital",
                "URGENT",
                OffsetDateTime.now().plusDays(1)
            );

        service.createRequest(userId, request);

        verify(repository).create(userId, request);
    }

    @Test
    void rejectsExpiredDeadline() {
        UUID userId = UUID.randomUUID();

        BloodRequestCreateRequest request =
            new BloodRequestCreateRequest(
                "O+",
                2,
                "Square Hospital",
                "CRITICAL",
                OffsetDateTime.now().minusDays(1)
            );

        ResponseStatusException exception =
            assertThrows(
                ResponseStatusException.class,
                () -> service.createRequest(userId, request)
            );

        assertEquals(
            HttpStatus.BAD_REQUEST,
            exception.getStatusCode()
        );

        verifyNoInteractions(repository);
    }

    @Test
    void returnsOnlyRequesterRecords() {
        UUID userId = UUID.randomUUID();

        when(repository.findByRequesterUserId(userId))
            .thenReturn(List.of());

        List<BloodRequest> results =
            service.getMyRequests(userId);

        assertTrue(results.isEmpty());

        verify(repository).expireOverdueRequests();
        verify(repository).findByRequesterUserId(userId);
    }

    @Test
    void returnsNotFoundForInaccessibleRequest() {
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        when(repository.findByIdForRequester(
                requestId, userId))
            .thenReturn(Optional.empty());

        ResponseStatusException exception =
            assertThrows(
                ResponseStatusException.class,
                () -> service.getMyRequest(userId, requestId)
            );

        assertEquals(
            HttpStatus.NOT_FOUND,
            exception.getStatusCode()
        );
    }

    @Test
    void expiresOverdueRequests() {
        when(repository.expireOverdueRequests())
            .thenReturn(2);

        assertEquals(2, service.expireOverdueRequests());

        verify(repository).expireOverdueRequests();
    }
}