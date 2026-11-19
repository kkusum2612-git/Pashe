
package com.pashe.app.medicine.schedule;

import com.pashe.app.medicine.preferences.MedicinePreferences;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public final class MedicineScheduleRules {
    private MedicineScheduleRules() {}

    public enum Slot { MORNING, NOON, NIGHT, BEDTIME }

    public record Dose(Slot slot, BigDecimal quantity, OffsetDateTime scheduledAt, String meal,
                       boolean fastingConflict) {}

    public static List<Dose> doses(String pattern, BigDecimal baseQuantity, String mealTiming,
                                   MedicinePreferences preferences, LocalDate date) {
        if (pattern == null) {
            return List.of();
        }
        var zone = ZoneId.of(preferences.timezone());
        var ramadan = preferences.ramadan();
        boolean fasting = ramadan != null && ramadan.includes(date);
        List<LocalTime> times = List.of(
            fasting ? ramadan.sehriTime() : preferences.breakfastTime(),
            preferences.lunchTime(),
            fasting ? ramadan.iftarTime() : preferences.dinnerTime(),
            preferences.bedtime()
        );
        List<String> meals = List.of(fasting ? "sehri" : "breakfast", "lunch",
            fasting ? "iftar" : "dinner", "bedtime");

        var doses = new ArrayList<Dose>();
        String[] slots = pattern.split("\\+");
        for (int i = 0; i < slots.length; i++) {
            BigDecimal multiplier = new BigDecimal(slots[i]);
            if (multiplier.signum() == 0) {
                continue;
            }
            Slot slot = Slot.values()[i];
            BigDecimal quantity = baseQuantity.multiply(multiplier).stripTrailingZeros();
            boolean conflict = fasting && slot == Slot.NOON;
            var time = date.atTime(times.get(i)).atZone(zone);
            if (slot != Slot.BEDTIME) {
                time = switch (mealTiming) {
                    case "BEFORE_MEAL" -> time.minusMinutes(30);
                    case "AFTER_MEAL" -> time.plusMinutes(15);
                    default -> time;
                };
            }
            doses.add(new Dose(slot, quantity, conflict ? null : time.toOffsetDateTime(), meals.get(i), conflict));
        }
        return List.copyOf(doses);
    }
}
