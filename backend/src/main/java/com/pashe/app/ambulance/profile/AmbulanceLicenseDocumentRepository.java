package com.pashe.app.ambulance.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AmbulanceLicenseDocumentRepository
        extends JpaRepository<AmbulanceLicenseDocument, UUID> {

    interface DocumentSummary {
        UUID getId();
        String getFileName();
        String getContentType();
        Instant getUploadedAt();
    }

    List<DocumentSummary> findAllByAmbulanceIdOrderByUploadedAtDesc(
            UUID ambulanceId
    );

    boolean existsByAmbulanceId(UUID ambulanceId);

    Optional<AmbulanceLicenseDocument> findByIdAndAmbulanceId(
            UUID id,
            UUID ambulanceId
    );
}