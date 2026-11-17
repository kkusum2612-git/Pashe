package com.pashe.app.blood.donor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DonorProfile(
    UUID userId,
    String bloodGroup,
    String area,
    LocalDate lastDonationDate,
    boolean available,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}