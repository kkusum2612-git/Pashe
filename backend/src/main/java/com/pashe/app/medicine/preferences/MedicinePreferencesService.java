package com.pashe.app.medicine.preferences;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.util.UUID;

@Service
public class MedicinePreferencesService {

    private final MedicinePreferencesRepository repository;

    public MedicinePreferencesService(
            MedicinePreferencesRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public MedicinePreferences getPreferences(UUID userId) {

        return repository.findByUserId(userId)
            .orElseGet(MedicinePreferences::defaults);
    }

    @Transactional
    public MedicinePreferences savePreferences(
            UUID userId,
            MedicinePreferences preferences) {

        validate(preferences);

        repository.save(userId, preferences);

        return preferences;
    }

    private void validate(MedicinePreferences preferences) {

        if (preferences.breakfastTime() == null
                || preferences.lunchTime() == null
                || preferences.dinnerTime() == null
                || preferences.bedtime() == null) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "All routine times are required"
            );
        }

        var ramadan = preferences.ramadan();
        if (ramadan != null && (ramadan.startDate() == null || ramadan.endDate() == null
                || ramadan.sehriTime() == null || ramadan.iftarTime() == null
                || ramadan.endDate().isBefore(ramadan.startDate()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a complete Ramadan date range and meal times");
        }

        String timezone = preferences.timezone();

        if (timezone == null
                || timezone.length() > 60
                || !ZoneId.getAvailableZoneIds()
                    .contains(timezone)) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid timezone"
            );
        }
    }
}