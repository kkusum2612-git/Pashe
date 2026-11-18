package com.pashe.app.ambulance.fare;

import com.pashe.app.ambulance.fare.FareDtos.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FareServiceTest {

    private final RoutingService routingService =
            mock(RoutingService.class);

    private final FareProperties properties = new FareProperties(
            new BigDecimal("500.00"),
            new BigDecimal("40.00")
    );

    private final FareService service =
            new FareService(properties, routingService);

    @Test
    void calculatesCorrectFare() {

        FareResponse result = service.calculateFare(
                new BigDecimal("8500")
        );

        assertEquals(new BigDecimal("8.500"), result.distanceKm());
        assertEquals(new BigDecimal("340.00"), result.distanceCharge());
        assertEquals(new BigDecimal("840.00"), result.totalFare());
        assertEquals("BDT", result.currency());
    }

    @Test
    void zeroDistanceReturnsBaseFare() {

        FareResponse result = service.calculateFare(
                BigDecimal.ZERO
        );

        assertEquals(new BigDecimal("500.00"), result.totalFare());
    }

    @Test
    void rejectsNegativeDistance() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.calculateFare(new BigDecimal("-100"))
        );
    }

    @Test
    void estimatesFareUsingRoadDistance() {

        Coordinates pickup = new Coordinates(
                new BigDecimal("23.726"),
                new BigDecimal("90.399")
        );

        Coordinates destination = new Coordinates(
                new BigDecimal("23.746"),
                new BigDecimal("90.381")
        );

        when(routingService.getDistanceMeters(pickup, destination))
                .thenReturn(new BigDecimal("12000"));

        FareResponse result = service.estimateFare(
                new FareEstimateRequest(pickup, destination)
        );

        assertEquals(new BigDecimal("980.00"), result.totalFare());
    }
}