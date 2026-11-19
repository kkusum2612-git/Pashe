package com.pashe.app.medicine.preferences;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MedicinePreferencesRepository {

    private final JdbcTemplate db;

    public MedicinePreferencesRepository(JdbcTemplate db) {
        this.db = db;
    }

    public Optional<MedicinePreferences> findByUserId(
            UUID userId) {

        return db.query("""
            SELECT breakfast_time,
                   lunch_time,
                   dinner_time,
                   bedtime,
                   timezone, ramadan_start, ramadan_end, sehri_time, iftar_time
            FROM medicine_preferences
            WHERE user_id = ?
            """,
            (rs, rowNum) -> new MedicinePreferences(
                rs.getObject("breakfast_time", LocalTime.class),
                rs.getObject("lunch_time", LocalTime.class),
                rs.getObject("dinner_time", LocalTime.class),
                rs.getObject("bedtime", LocalTime.class),
                rs.getString("timezone"),
                rs.getObject("ramadan_start", LocalDate.class) == null ? null : new MedicinePreferences.Ramadan(
                    rs.getObject("ramadan_start", LocalDate.class), rs.getObject("ramadan_end", LocalDate.class),
                    rs.getObject("sehri_time", LocalTime.class), rs.getObject("iftar_time", LocalTime.class)
                )
            ),
            userId
        ).stream().findFirst();
    }

    public void save(
            UUID userId,
            MedicinePreferences preferences) {

        var ramadan = preferences.ramadan();
        db.update("""
            INSERT INTO medicine_preferences (
                user_id,
                breakfast_time,
                lunch_time,
                dinner_time,
                bedtime,
                timezone, ramadan_start, ramadan_end, sehri_time, iftar_time
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (user_id)
            DO UPDATE SET
                breakfast_time = EXCLUDED.breakfast_time,
                lunch_time = EXCLUDED.lunch_time,
                dinner_time = EXCLUDED.dinner_time,
                bedtime = EXCLUDED.bedtime,
                timezone = EXCLUDED.timezone,
                ramadan_start = EXCLUDED.ramadan_start,
                ramadan_end = EXCLUDED.ramadan_end,
                sehri_time = EXCLUDED.sehri_time,
                iftar_time = EXCLUDED.iftar_time,
                updated_at = CURRENT_TIMESTAMP
            """,
            userId,
            preferences.breakfastTime(),
            preferences.lunchTime(),
            preferences.dinnerTime(),
            preferences.bedtime(),
            preferences.timezone(),
            ramadan == null ? null : ramadan.startDate(),
            ramadan == null ? null : ramadan.endDate(),
            ramadan == null ? null : ramadan.sehriTime(),
            ramadan == null ? null : ramadan.iftarTime()
        );
    }
}