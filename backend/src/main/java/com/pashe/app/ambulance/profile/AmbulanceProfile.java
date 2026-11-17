package com.pashe.app.ambulance.profile;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ambulance_profiles")
public class AmbulanceProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "operator_id", nullable = false)
    private UUID operatorId;

    @Column(name = "registration_number",
            nullable = false, unique = true, length = 50)
    private String registrationNumber;

    @Column(name = "vehicle_name", nullable = false, length = 100)
    private String vehicleName;

    @Column(name = "vehicle_type", nullable = false, length = 50)
    private String vehicleType;

    @Column(name = "is_available", nullable = false)
    private boolean available = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus
            = VerificationStatus.PENDING;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AmbulanceProfile() {
    }

    public AmbulanceProfile(
            UUID operatorId,
            String registrationNumber,
            String vehicleName,
            String vehicleType
    ) {
        this.operatorId = operatorId;
        this.registrationNumber = registrationNumber;
        this.vehicleName = vehicleName;
        this.vehicleType = vehicleType;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOperatorId() {
        return operatorId;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getVehicleName() {
        return vehicleName;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public boolean isAvailable() {
        return available;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setAvailable(boolean available) {
        if (available && verificationStatus != VerificationStatus.APPROVED) {
            throw new IllegalStateException("Ambulance is not verified");
        }

        this.available = available;
    }

    public void approve(UUID adminId, String notes) {
        this.verificationStatus = VerificationStatus.APPROVED;
        this.reviewedBy = adminId;
        this.reviewedAt = Instant.now();
        this.reviewNotes = notes;
    }

    public void reject(UUID adminId, String notes) {
        this.verificationStatus = VerificationStatus.REJECTED;
        this.reviewedBy = adminId;
        this.reviewedAt = Instant.now();
        this.reviewNotes = notes;
        this.available = false;
    }

    public void resetVerification() {
        this.verificationStatus = VerificationStatus.PENDING;
        this.available = false;
        this.reviewedBy = null;
        this.reviewedAt = null;
        this.reviewNotes = null;
        this.updatedAt = Instant.now();
    }
    public void updateDetails(
        String registrationNumber,
        String vehicleName,
        String vehicleType
    ) {
        this.registrationNumber = registrationNumber;
        this.vehicleName = vehicleName;
        this.vehicleType = vehicleType;

        resetVerification();
    }
}