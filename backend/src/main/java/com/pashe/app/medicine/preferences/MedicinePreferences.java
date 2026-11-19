package com.pashe.app.medicine.preferences;

import java.time.LocalDate;
import java.time.LocalTime;

public record MedicinePreferences(
    LocalTime breakfastTime,
    LocalTime lunchTime,
    LocalTime dinnerTime,
    LocalTime bedtime,
    String timezone,
    Ramadan ramadan
) {

    public record Ramadan(LocalDate startDate, LocalDate endDate, LocalTime sehriTime, LocalTime iftarTime) {
        public boolean includes(LocalDate date) {
            return !date.isBefore(startDate) && !date.isAfter(endDate);
        }
    }

    public static MedicinePreferences defaults() {
        return new MedicinePreferences(
            LocalTime.of(8, 0),
            LocalTime.of(13, 0),
            LocalTime.of(20, 0),
            LocalTime.of(22, 30),
            "Asia/Dhaka",
            null
        );
    }
}