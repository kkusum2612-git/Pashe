package com.pashe.app.ambulance.profile;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AmbulanceProfileDtos {

    private AmbulanceProfileDtos() {
    }

    // Register a new ambulance
    public record RegistrationRequest(

            @NotBlank
            @Size(max = 50)
            String registrationNumber,

            @NotBlank
            @Size(max = 100)
            String vehicleName,

            @NotBlank
            @Size(max = 50)
            String vehicleType
    ) {}

    // Add equipment
    public record EquipmentRequest(

            @NotBlank
            @Size(max = 100)
            String equipmentName,

            @NotNull
            @Positive
            Integer quantity
    ) {}

    // Change ambulance availability
    public record AvailabilityRequest(

            @NotNull
            Boolean available
    ) {}

    // Administrator verification
    public record ReviewRequest(

            @NotNull
            VerificationStatus verificationStatus,

            @Size(max = 1000)
            String notes
    ) {}

    // Equipment information
    public record EquipmentResponse(
            UUID id,
            String equipmentName,
            int quantity
    ) {}

    // License document metadata
    public record DocumentResponse(
            UUID id,
            String fileName,
            String contentType,
            Instant uploadedAt
    ) {}

    // Full profile for owner and administrator
    public record ProfileResponse(
            UUID id,
            UUID operatorId,
            String registrationNumber,
            String vehicleName,
            String vehicleType,
            boolean available,
            VerificationStatus verificationStatus,
            UUID reviewedBy,
            Instant reviewedAt,
            String reviewNotes,
            Instant createdAt,
            Instant updatedAt,
            List<EquipmentResponse> equipment,
            List<DocumentResponse> documents
    ) {}

    // Limited profile for regular users
    public record PublicAmbulanceResponse(
            UUID id,
            String registrationNumber,
            String vehicleName,
            String vehicleType,
            boolean available,
            List<EquipmentResponse> equipment
    ) {}
}