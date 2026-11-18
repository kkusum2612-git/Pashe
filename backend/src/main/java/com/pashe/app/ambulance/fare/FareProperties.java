package com.pashe.app.ambulance.fare;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class FareProperties {

    private final BigDecimal baseFare;
    private final BigDecimal ratePerKm;

    public FareProperties(
            @Value("${PASHE_AMBULANCE_BASE_FARE:500.00}")
            BigDecimal baseFare,

            @Value("${PASHE_AMBULANCE_RATE_PER_KM:40.00}")
            BigDecimal ratePerKm
    ) {
        if (baseFare.signum() < 0 || ratePerKm.signum() < 0) {
            throw new IllegalArgumentException(
                    "Fare rates cannot be negative"
            );
        }

        this.baseFare = baseFare.setScale(
                2, RoundingMode.UNNECESSARY
        );

        this.ratePerKm = ratePerKm.setScale(
                2, RoundingMode.UNNECESSARY
        );
    }

    public BigDecimal getBaseFare() {
        return baseFare;
    }

    public BigDecimal getRatePerKm() {
        return ratePerKm;
    }
}