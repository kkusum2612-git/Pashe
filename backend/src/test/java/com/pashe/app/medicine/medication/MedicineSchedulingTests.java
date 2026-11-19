
package com.pashe.app.medicine.medication;

import com.pashe.app.medicine.preferences.MedicinePreferences;
import com.pashe.app.medicine.preferences.MedicinePreferencesService;
import com.pashe.app.medicine.schedule.MedicineScheduleRules;
import com.pashe.app.medicine.schedule.MedicineScheduleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MedicineSchedulingTests {
    private static final LocalDate START = LocalDate.of(2026, 11, 19);
    private static final MedicinePreferences ROUTINE = MedicinePreferences.defaults();
    private static final UUID OWNER = UUID.randomUUID();

    @Test
    void normalizesBanglaAndFractionalSlots() {
        assertEquals("1+0+0.5+1.25", UserMedicineService.normalizePattern(" ১ + ০ + ½ + ১.২৫ ", false));
        assertEquals("1+0+0.5", UserMedicineService.normalizePattern("1+0+0.500", false));
        assertNull(UserMedicineService.normalizePattern(null, true));
        assertThrows(ResponseStatusException.class, () -> UserMedicineService.normalizePattern("1+0+1", true));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1+1", "1+0+0+1+1", "1++1", "1+1+0+", "0+0+0", "-1+0+1", "1+0+0.0001", "1+2e1+1"})
    void rejectsInvalidPatterns(String pattern) {
        assertThrows(ResponseStatusException.class, () -> UserMedicineService.normalizePattern(pattern, false));
    }

    @Test
    void returnsFirstAndLastCourseDaysAndRetainsHistoricalFinishedDoses() {
        var service = mock(UserMedicineService.class);
        var preferences = mock(MedicinePreferencesService.class);
        when(preferences.getPreferences(OWNER)).thenReturn(ROUTINE);
        var finished = medicine("FINISHED", "0+0+1", 7, null);
        when(service.getMyMedicines(OWNER)).thenReturn(List.of(finished));
        var schedule = new MedicineScheduleService(service, preferences);

        assertTrue(schedule.getSchedule(OWNER, START.minusDays(1)).isEmpty());
        assertEquals(LocalTime.of(20, 15), schedule.getSchedule(OWNER, START).getFirst().scheduledAt().toLocalTime());
        assertEquals(1, schedule.getSchedule(OWNER, START.plusDays(6)).size());
        assertTrue(schedule.getSchedule(OWNER, START.plusDays(7)).isEmpty());
    }

    @Test
    void changesTimeWithoutChangingDoseIdAndRetainsPreviousRecordThroughDayBeforeReplacement() {
        var service = mock(UserMedicineService.class);
        var preferences = mock(MedicinePreferencesService.class);
        var changed = new MedicinePreferences(LocalTime.of(10, 0), ROUTINE.lunchTime(), ROUTINE.dinnerTime(),
            ROUTINE.bedtime(), ROUTINE.timezone(), null);
        when(preferences.getPreferences(OWNER)).thenReturn(ROUTINE, changed);
        var previous = medicine("FINISHED", "1+0+0", null, START.plusDays(1));
        when(service.getMyMedicines(OWNER)).thenReturn(List.of(previous));
        var schedule = new MedicineScheduleService(service, preferences);

        var before = schedule.getSchedule(OWNER, START).getFirst();
        var after = schedule.getSchedule(OWNER, START).getFirst();
        assertEquals(before.doseId(), after.doseId());
        assertEquals(LocalTime.of(10, 15), after.scheduledAt().toLocalTime());
        assertTrue(schedule.getSchedule(OWNER, START.plusDays(1)).isEmpty());
    }

    @Test
    void handlesMidnightInBothDirectionsAndDoesNotFinishWhileFinalDoseIsStillScheduled() {
        var routine = new MedicinePreferences(LocalTime.of(0, 10), ROUTINE.lunchTime(),
            LocalTime.of(23, 55), LocalTime.of(23, 59), ROUTINE.timezone(), null);
        var previousNight = MedicineScheduleRules.doses("1+0+0", BigDecimal.ONE, "BEFORE_MEAL", routine, START).getFirst();
        assertEquals(START.minusDays(1).atTime(23, 40), previousNight.scheduledAt().toLocalDateTime());

        var finalDoses = MedicineScheduleRules.doses("0+0+0.5+1", BigDecimal.valueOf(2), "AFTER_MEAL", routine, START);
        assertEquals(START.plusDays(1).atTime(0, 10), finalDoses.getFirst().scheduledAt().toLocalDateTime());
        assertEquals(0, BigDecimal.ONE.compareTo(finalDoses.getFirst().quantity()));
        assertEquals(START.atTime(23, 59), finalDoses.getLast().scheduledAt().toLocalDateTime());

        var active = medicine("ACTIVE", "0+0+0.5+1", 1, null);
        var finalInstant = finalDoses.getFirst().scheduledAt().toInstant();
        assertFalse(UserMedicineService.courseHasEnded(active, routine, finalInstant.minusSeconds(1)));
        assertTrue(UserMedicineService.courseHasEnded(active, routine, finalInstant.plusSeconds(1)));
        assertFalse(UserMedicineService.courseHasEnded(medicine("ACTIVE", "1+0+0", null, null), routine,
            finalInstant.plusSeconds(1)));
    }

    @Test
    void appliesRamadanOnlyOnInclusiveDatesAndLeavesNoonUnscheduled() {
        var ramadan = new MedicinePreferences.Ramadan(START, START.plusDays(2), LocalTime.of(4, 30), LocalTime.of(18, 0));
        var routine = new MedicinePreferences(ROUTINE.breakfastTime(), ROUTINE.lunchTime(), ROUTINE.dinnerTime(),
            ROUTINE.bedtime(), ROUTINE.timezone(), ramadan);

        for (var day : List.of(START, START.plusDays(2))) {
            var doses = MedicineScheduleRules.doses("1+1+1+1", BigDecimal.ONE, "BEFORE_MEAL", routine, day);
            assertEquals(day.atTime(4, 0), doses.getFirst().scheduledAt().toLocalDateTime());
            assertTrue(doses.get(1).fastingConflict());
            assertNull(doses.get(1).scheduledAt());
            assertEquals(day.atTime(17, 30), doses.get(2).scheduledAt().toLocalDateTime());
            assertEquals(day.atTime(22, 30), doses.getLast().scheduledAt().toLocalDateTime());
        }
        for (var day : List.of(START.minusDays(1), START.plusDays(3))) {
            var doses = MedicineScheduleRules.doses("0+1+0", BigDecimal.ONE, "ANYTIME", routine, day);
            assertFalse(doses.getFirst().fastingConflict());
            assertEquals(day.atTime(13, 0), doses.getFirst().scheduledAt().toLocalDateTime());
        }
        assertTrue(MedicineScheduleRules.doses(null, BigDecimal.ONE, "AFTER_MEAL", routine, START).isEmpty());
    }

    private UserMedicine medicine(String status, String pattern, Integer duration, LocalDate endDate) {
        return new UserMedicine(UUID.randomUUID(), null, "Synthetic medicine", true, BigDecimal.ONE, "tablet", pattern,
            "AFTER_MEAL", false, START, duration, false, null, status, null, null, OWNER, null, null, null, null, endDate);
    }
}
