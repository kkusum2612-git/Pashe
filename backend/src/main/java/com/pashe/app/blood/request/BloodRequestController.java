package com.pashe.app.blood.request;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/blood/requests")
public class BloodRequestController {

    private final BloodRequestService service;

    public BloodRequestController(BloodRequestService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<BloodRequest> createRequest(
            Principal principal,
            @Valid @RequestBody BloodRequestCreateRequest request) {

        UUID userId = UUID.fromString(principal.getName());

        BloodRequest created = service.createRequest(userId, request);

        return ResponseEntity
            .created(URI.create("/api/v1/blood/requests/" + created.id()))
            .body(created);
    }

    @GetMapping
    public List<BloodRequest> getMyRequests(Principal principal) {

        UUID userId = UUID.fromString(principal.getName());

        return service.getMyRequests(userId);
    }

    @GetMapping("/{requestId}")
    public BloodRequest getMyRequest(
            Principal principal,
            @PathVariable UUID requestId) {

        UUID userId = UUID.fromString(principal.getName());

        return service.getMyRequest(userId, requestId);
    }
}