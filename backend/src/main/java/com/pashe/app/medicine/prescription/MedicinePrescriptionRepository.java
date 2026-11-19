
package com.pashe.app.medicine.prescription;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MedicinePrescriptionRepository {
    private final JdbcTemplate db;

    public MedicinePrescriptionRepository(JdbcTemplate db) {
        this.db = db;
    }

    public List<MedicinePrescription> findByUserId(UUID userId) {
        return db.query("""
            SELECT id, doctor_name, prescription_date, image_type IS NOT NULL AS has_image
            FROM medicine_prescriptions WHERE user_id = ?
            ORDER BY prescription_date DESC, id
            """, (rs, row) -> new MedicinePrescription(
                rs.getObject("id", UUID.class), rs.getString("doctor_name"),
                rs.getObject("prescription_date", LocalDate.class), rs.getBoolean("has_image")
            ), userId);
    }

    public void insert(UUID userId, MedicinePrescription prescription, Image image) {
        db.update("""
            INSERT INTO medicine_prescriptions (id, user_id, doctor_name, prescription_date, image, image_type)
            VALUES (?, ?, ?, ?, ?, ?)
            """, prescription.id(), userId, prescription.doctorName(), prescription.prescriptionDate(),
            image == null ? null : image.bytes(), image == null ? null : image.contentType());
    }

    public boolean exists(UUID userId, UUID id) {
        return Boolean.TRUE.equals(db.queryForObject("""
            SELECT EXISTS (SELECT 1 FROM medicine_prescriptions WHERE user_id = ? AND id = ?)
            """, Boolean.class, userId, id));
    }

    public Optional<Image> findImage(UUID userId, UUID id) {
        return db.query("""
            SELECT image, image_type FROM medicine_prescriptions
            WHERE user_id = ? AND id = ? AND image_type IS NOT NULL
            """, (rs, row) -> new Image(rs.getBytes("image"), rs.getString("image_type")), userId, id)
            .stream().findFirst();
    }

    public record Image(byte[] bytes, String contentType) {}
}
