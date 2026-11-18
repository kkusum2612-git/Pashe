package com.pashe.app.ambulance.fare;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public final class FareDtos {

    private FareDtos() {}

    public record Coordinates(
            @NotNull
            @DecimalMin("-90")
            @DecimalMax("90")
            BigDecimal latitude,

            @NotNull
            @DecimalMin("-180")
            @DecimalMax("180")
            BigDecimal longitude
    ) {}

    public record LocationResponse(
            String label,
            BigDecimal latitude,
            BigDecimal longitude
    ) {}

    public record FareEstimateRequest(
            @NotNull @Valid Coordinates pickup,
            @NotNull @Valid Coordinates destination
    ) {}

    public record FareResponse(
            BigDecimal distanceKm,
            BigDecimal baseFare,
            BigDecimal ratePerKm,
            BigDecimal distanceCharge,
            BigDecimal totalFare,
            String currency
    ) {}
}