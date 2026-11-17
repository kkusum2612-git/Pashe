package com.pashe.app.ambulance.profile;

import com.pashe.app.ambulance.profile.AmbulanceProfileDtos.*;

import jakarta.validation.Valid;

import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AmbulanceProfileController {

    private final AmbulanceProfileService service;

    public AmbulanceProfileController(
            AmbulanceProfileService service
    ) {
        this.service = service;
    }

    // Register ambulance
    @PostMapping("/ambulance/profiles")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse register(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RegistrationRequest request
    ) {
        return service.register(userId(jwt), request);
    }

    // View operator's ambulances
    @GetMapping("/ambulance/profiles")
    public List<ProfileResponse> getMyAmbulances(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return service.getMyAmbulances(userId(jwt));
    }

    // Update ambulance details
    @PutMapping("/ambulance/profiles/{id}")
    public ProfileResponse updateDetails(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody RegistrationRequest request
    ) {
        return service.updateDetails(
                userId(jwt), id, request
        );
    }

    // Add equipment
    @PostMapping("/ambulance/profiles/{id}/equipment")
    public ProfileResponse addEquipment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody EquipmentRequest request
    ) {
        return service.addEquipment(
                userId(jwt), id, request
        );
    }

    // Remove equipment
    @DeleteMapping(
            "/ambulance/profiles/{id}/equipment/{equipmentId}"
    )
    public ProfileResponse removeEquipment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID equipmentId
    ) {
        return service.removeEquipment(
                userId(jwt), id, equipmentId
        );
    }

    // Upload license document
    @PostMapping(
            value = "/ambulance/profiles/{id}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ProfileResponse uploadDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file
    ) {
        return service.uploadDocument(
                userId(jwt), id, file
        );
    }

    // Remove license document
    @DeleteMapping(
            "/ambulance/profiles/{id}/documents/{documentId}"
    )
    public ProfileResponse removeDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID documentId
    ) {
        return service.removeDocument(
                userId(jwt), id, documentId
        );
    }

    // Download operator's license document
    @GetMapping(
            "/ambulance/profiles/{id}/documents/{documentId}"
    )
    public ResponseEntity<byte[]> downloadMyDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID documentId
    ) {
        return download(service.downloadMyDocument(
                userId(jwt), id, documentId
        ));
    }

    // Update availability
    @PatchMapping("/ambulance/profiles/{id}/availability")
    public ProfileResponse updateAvailability(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody AvailabilityRequest request
    ) {
        return service.updateAvailability(
                userId(jwt), id, request
        );
    }

    // Admin: view pending applications
    @GetMapping("/admin/ambulance-profiles/pending")
    public List<ProfileResponse> getPendingAmbulances() {
        return service.getPendingAmbulances();
    }

    // Admin: approve or reject
    @PatchMapping("/admin/ambulance-profiles/{id}/review")
    public ProfileResponse reviewAmbulance(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody ReviewRequest request
    ) {
        return service.reviewAmbulance(
                userId(jwt), id, request
        );
    }

    // Admin: download document for verification
    @GetMapping(
            "/admin/ambulance-profiles/{id}/documents/{documentId}"
    )
    public ResponseEntity<byte[]> downloadForReview(
            @PathVariable UUID id,
            @PathVariable UUID documentId
    ) {
        return download(service.downloadDocumentForReview(
                id, documentId
        ));
    }

    // View verified ambulances
    @GetMapping("/ambulance/verified")
    public List<PublicAmbulanceResponse> getVerifiedAmbulances() {
        return service.getVerifiedAmbulances();
    }

    // Extract authenticated user ID
    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    // Return license as a private attachment
    private ResponseEntity<byte[]> download(
            AmbulanceProfileService.DocumentDownload document
    ) {
        ContentDisposition disposition = ContentDisposition
                .attachment()
                .filename(
                        document.fileName(),
                        StandardCharsets.UTF_8
                )
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        document.contentType()
                ))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .body(document.data());
    }
}