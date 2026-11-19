package com.pashe.app.blood.request;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BloodRequestRepository {

    private final JdbcTemplate db;

    public BloodRequestRepository(JdbcTemplate db) {
        this.db = db;
    }

    private BloodRequest mapRow(ResultSet rs)
            throws SQLException {

        return new BloodRequest(
            rs.getObject("id", UUID.class),
            rs.getObject("requester_user_id", UUID.class),
            rs.getString("blood_group"),
            rs.getInt("required_units"),
            rs.getString("hospital_name"),
            rs.getString("urgency"),
            rs.getObject("expires_at", OffsetDateTime.class),
            rs.getInt("accepted_donors_count"),
            rs.getInt("confirmed_units"),
            rs.getString("status"),
            rs.getObject("created_at", OffsetDateTime.class),
            rs.getObject("updated_at", OffsetDateTime.class)
        );
    }

    public BloodRequest create(
            UUID userId,
            BloodRequestCreateRequest request) {

        UUID requestId = UUID.randomUUID();

        return db.queryForObject("""
            INSERT INTO blood_requests (
                id,
                requester_user_id,
                blood_group,
                required_units,
                hospital_name,
                urgency,
                expires_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING *
            """,
            (rs, rowNum) -> mapRow(rs),
            requestId,
            userId,
            request.bloodGroup(),
            request.requiredUnits(),
            request.hospitalName().strip(),
            request.urgency(),
            request.expiresAt()
        );
    }

    public Optional<BloodRequest> findByIdForRequester(
            UUID requestId,
            UUID userId) {

        return db.query("""
            SELECT *
            FROM blood_requests
            WHERE id = ?
              AND requester_user_id = ?
            """,
            (rs, rowNum) -> mapRow(rs),
            requestId,
            userId
        ).stream().findFirst();
    }

    public List<BloodRequest> findByRequesterUserId(
            UUID userId) {

        return db.query("""
            SELECT *
            FROM blood_requests
            WHERE requester_user_id = ?
            ORDER BY created_at DESC, id DESC
            """,
            (rs, rowNum) -> mapRow(rs),
            userId
        );
    }

    public int fulfillCompletedRequests() {
        return db.update("""
            UPDATE blood_requests
            SET status = 'FULFILLED',
                updated_at = CURRENT_TIMESTAMP
            WHERE status = 'ACTIVE'
              AND expires_at > CURRENT_TIMESTAMP
              AND confirmed_units >= required_units
            """);
    }
    public int expireOverdueRequests() {

        return db.update("""
            UPDATE blood_requests
            SET status = 'EXPIRED',
                updated_at = CURRENT_TIMESTAMP
            WHERE status = 'ACTIVE'
              AND expires_at <= CURRENT_TIMESTAMP
            """);
    }
}