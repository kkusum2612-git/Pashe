package com.pashe.app.blood.donor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DonorProfileRepository {

    private final JdbcTemplate db;

    public DonorProfileRepository(JdbcTemplate db) {
        this.db = db;
    }

    private DonorProfile mapRow(ResultSet rs)
            throws SQLException {

        return new DonorProfile(
            rs.getObject("user_id", UUID.class),
            rs.getString("blood_group"),
            rs.getString("area"),
            rs.getObject("last_donation_date", LocalDate.class),
            rs.getBoolean("available"),
            rs.getObject("created_at", OffsetDateTime.class),
            rs.getObject("updated_at", OffsetDateTime.class)
        );
    }

    public Optional<DonorProfile> findByUserId(UUID userId) {
        return db.query(
            "SELECT * FROM blood_donor_profiles WHERE user_id = ?",
            (rs, rowNum) -> mapRow(rs),
            userId
        ).stream().findFirst();
    }

    public DonorProfile save(
            UUID userId,
            DonorProfileRequest request) {

        return db.queryForObject("""
            INSERT INTO blood_donor_profiles (
                user_id,
                blood_group,
                area,
                last_donation_date,
                available
            )
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (user_id)
            DO UPDATE SET
                blood_group = EXCLUDED.blood_group,
                area = EXCLUDED.area,
                last_donation_date = EXCLUDED.last_donation_date,
                available = EXCLUDED.available,
                updated_at = CURRENT_TIMESTAMP
            RETURNING *
            """,
            (rs, rowNum) -> mapRow(rs),
            userId,
            request.bloodGroup(),
            request.area().strip(),
            request.lastDonationDate(),
            request.available()
        );
    }
}