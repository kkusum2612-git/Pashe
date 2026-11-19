package com.pashe.app.medicine.medication;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserMedicineRepository {

    private final JdbcTemplate db;

    private static final String SELECT_MEDICINES = """
        SELECT um.*,
               COALESCE(
                   mc.brand_name,
                   um.custom_medicine_name
               ) AS medicine_name
        FROM user_medicines um
        LEFT JOIN medicine_catalog mc
            ON mc.id = um.medicine_id
        """;

    public UserMedicineRepository(JdbcTemplate db) {
        this.db = db;
    }

    public List<UserMedicine> findByUserId(UUID userId) {

        return db.query(
            SELECT_MEDICINES + """
                WHERE um.user_id = ?
                ORDER BY um.created_at DESC
                """,
            (rs, rowNum) -> mapRow(rs),
            userId
        );
    }

    public Optional<UserMedicine> findById(
            UUID userId,
            UUID id) {

        return db.query(
            SELECT_MEDICINES + """
                WHERE um.user_id = ? AND um.id = ?
                """,
            (rs, rowNum) -> mapRow(rs),
            userId,
            id
        ).stream().findFirst();
    }

    public void insert(
            UUID id,
            UUID userId,
            UserMedicineRequest request,
            String customName,
            String normalizedPattern,
            UUID previousMedicineId) {

        db.update("""
            INSERT INTO user_medicines (
                id,
                user_id,
                medicine_id,
                custom_medicine_name,
                dose_quantity,
                dose_unit,
                dose_pattern,
                meal_timing,
                as_needed,
                start_date,
                duration_days,
                critical,
                remaining_quantity,
                created_by,
                prescription_id,
                previous_medicine_id
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            id,
            userId,
            request.medicineId(),
            customName,
            request.doseQuantity(),
            request.doseUnit().strip(),
            normalizedPattern,
            request.mealTiming(),
            request.asNeeded(),
            request.startDate(),
            request.durationDays(),
            request.critical(),
            request.remainingQuantity(),
            userId,
            request.prescriptionId(),
            previousMedicineId
        );
    }

    public int stop(
            UUID userId,
            UUID id,
            String reason,
            LocalDate endDate) {

        return db.update("""
            UPDATE user_medicines
            SET status = 'STOPPED',
                stop_reason = ?,
                schedule_end_date = ?,
                ended_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
              AND user_id = ?
              AND status = 'ACTIVE'
            """,
            reason,
            endDate,
            id,
            userId
        );
    }

    public int finish(UUID userId, UUID id, LocalDate endDate, boolean replacement) {
        return db.update("""
            UPDATE user_medicines SET status = 'FINISHED', schedule_end_date = ?,
                ended_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
            WHERE user_id = ? AND id = ? AND status = 'ACTIVE' AND schedule_end_date IS NULL
            """, replacement ? endDate : null, userId, id);
    }

    public List<UUID> findOwnersWithCoursesNearEnd(LocalDate throughDate) {
        return db.query("""
            SELECT DISTINCT user_id FROM user_medicines
            WHERE status = 'ACTIVE' AND start_date + duration_days <= ?
            """, (rs, row) -> rs.getObject(1, UUID.class), throughDate);
    }

    private UserMedicine mapRow(ResultSet rs)
            throws SQLException {

        return new UserMedicine(
            rs.getObject("id", UUID.class),
            rs.getObject("medicine_id", UUID.class),
            rs.getString("medicine_name"),
            rs.getObject("medicine_id") == null,
            rs.getBigDecimal("dose_quantity"),
            rs.getString("dose_unit"),
            rs.getString("dose_pattern"),
            rs.getString("meal_timing"),
            rs.getBoolean("as_needed"),
            rs.getObject("start_date", LocalDate.class),
            rs.getObject("duration_days", Integer.class),
            rs.getBoolean("critical"),
            rs.getBigDecimal("remaining_quantity"),
            rs.getString("status"),
            rs.getString("stop_reason"),
            rs.getObject("ended_at", OffsetDateTime.class),
            rs.getObject("created_by", UUID.class),
            rs.getObject("created_at", OffsetDateTime.class),
            rs.getObject("updated_at", OffsetDateTime.class),
            rs.getObject("prescription_id", UUID.class),
            rs.getObject("previous_medicine_id", UUID.class),
            rs.getObject("schedule_end_date", LocalDate.class)
        );
    }
}