package com.pashe.app.blood.donor;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DonorProfileService {

    private final DonorProfileRepository repository;

    public DonorProfileService(DonorProfileRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public DonorProfile getProfile(UUID userId) {
        return repository.findByUserId(userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Donor profile not found"
            ));
    }

    @Transactional
    public DonorProfile saveProfile(
            UUID userId,
            DonorProfileRequest request) {

        return repository.save(userId, request);
    }
}