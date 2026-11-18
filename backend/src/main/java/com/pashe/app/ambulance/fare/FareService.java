package com.pashe.app.ambulance.fare;

import com.pashe.app.ambulance.fare.FareDtos.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FareService {

    private final FareProperties properties;
    private final RoutingService routingService;

    public FareService(
            FareProperties properties,
            RoutingService routingService
    ) {
        this.properties = properties;
        this.routingService = routingService;
    }

    public FareResponse estimateFare(FareEstimateRequest request) {

        BigDecimal distance = routingService.getDistanceMeters(
                request.pickup(),
                request.destination()
        );

        return calculateFare(distance);
    }

    public FareResponse calculateFare(BigDecimal distanceMeters) {

        if (distanceMeters == null || distanceMeters.signum() < 0) {
            throw new IllegalArgumentException("Invalid distance");
        }

        BigDecimal distanceKm = distanceMeters
                .movePointLeft(3)
                .setScale(3, RoundingMode.HALF_UP);

        BigDecimal baseFare = properties.getBaseFare();
        BigDecimal ratePerKm = properties.getRatePerKm();

        BigDecimal distanceCharge = distanceKm
                .multiply(ratePerKm)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalFare = baseFare.add(distanceCharge);

        return new FareResponse(
                distanceKm,
                baseFare,
                ratePerKm,
                distanceCharge,
                totalFare,
                "BDT"
        );
    }
}