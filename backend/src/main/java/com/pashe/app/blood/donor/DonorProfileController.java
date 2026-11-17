package com.pashe.app.blood.donor;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/blood/donor-profile")
public class DonorProfileController {

    private final DonorProfileService service;

    public DonorProfileController(DonorProfileService service) {
        this.service = service;
    }

    @GetMapping
    public DonorProfile getProfile(Principal principal) {

        UUID userId = UUID.fromString(principal.getName());

        return service.getProfile(userId);
    }

    @PutMapping
    public DonorProfile saveProfile(
            Principal principal,
            @Valid @RequestBody DonorProfileRequest request) {

        UUID userId = UUID.fromString(principal.getName());

        return service.saveProfile(userId, request);
    }
}