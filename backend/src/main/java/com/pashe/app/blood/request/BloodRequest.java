package com.pashe.app.blood.request;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BloodRequest(
    UUID id,
    UUID requesterUserId,
    String bloodGroup,
    int requiredUnits,
    String hospitalName,
    String urgency,
    OffsetDateTime expiresAt,
    int acceptedDonorsCount,
    int confirmedUnits,
    String status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}