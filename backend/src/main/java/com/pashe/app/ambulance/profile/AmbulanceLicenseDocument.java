package com.pashe.app.ambulance.profile;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ambulance_license_documents")
public class AmbulanceLicenseDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ambulance_id", nullable = false)
    private UUID ambulanceId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_data", nullable = false, columnDefinition = "BYTEA")
    private byte[] fileData;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    protected AmbulanceLicenseDocument() {
    }

    public AmbulanceLicenseDocument(
            UUID ambulanceId,
            String fileName,
            String contentType,
            byte[] fileData
    ) {
        this.ambulanceId = ambulanceId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileData = fileData.clone();
    }

    @PrePersist
    protected void onUpload() {
        uploadedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getAmbulanceId() {
        return ambulanceId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getFileData() {
        return fileData.clone();
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}