package com.pashe.app.guardian.carecircle;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CareCircleRepository {
    private final JdbcTemplate db;
    public CareCircleRepository(JdbcTemplate db) { this.db=db; }
    public record Membership(UUID id,UUID careRecipientId,UUID inviteeId,String relationshipType,String status,Instant recipientConsentedAt,Instant inviteeConsentedAt,Instant createdAt) {}
    private final org.springframework.jdbc.core.RowMapper<Membership> mapper=(r,n) -> new Membership(r.getObject("id",UUID.class),r.getObject("care_recipient_id",UUID.class),r.getObject("invitee_id",UUID.class),r.getString("relationship_type"),r.getString("status"),r.getTimestamp("recipient_consented_at").toInstant(),r.getTimestamp("invitee_consented_at")==null?null:r.getTimestamp("invitee_consented_at").toInstant(),r.getTimestamp("created_at").toInstant());
    public List<Membership> list(UUID actor,int page) { return db.query("SELECT * FROM care_circle_memberships WHERE care_recipient_id=? OR invitee_id=? ORDER BY created_at DESC,id LIMIT 50 OFFSET ?",mapper,actor,actor,page*50); }
    public Optional<Membership> find(UUID id) { return db.query("SELECT * FROM care_circle_memberships WHERE id=?",mapper,id).stream().findFirst(); }
    public void lock(UUID recipient) { db.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",r -> {},recipient.toString()); }
    public List<Membership> active(UUID recipient) { return db.query("SELECT * FROM care_circle_memberships WHERE care_recipient_id=? AND status='ACTIVE'",mapper,recipient); }
    public void insert(UUID id,UUID owner,UUID invitee,String type,Instant now) {
        db.update("INSERT INTO care_circle_memberships(id,care_recipient_id,invitee_id,inviter_id,relationship_type,status,recipient_consented_at,created_at,updated_at) VALUES (?,?,?,?,?,'PENDING',?,?,?)",id,owner,invitee,owner,type,Timestamp.from(now),Timestamp.from(now),Timestamp.from(now));
        event(id,owner,"INVITED",now);
    }
    public void transition(UUID id,UUID actor,String status,Instant now) {
        db.update("UPDATE care_circle_memberships SET status=?,updated_at=?,invitee_consented_at=CASE WHEN ?='ACTIVE' THEN CAST(? AS TIMESTAMPTZ) ELSE invitee_consented_at END,ended_at=CASE WHEN ?='ACTIVE' THEN NULL ELSE CAST(? AS TIMESTAMPTZ) END WHERE id=?",status,Timestamp.from(now),status,Timestamp.from(now),status,Timestamp.from(now),id);
        event(id,actor,status,now);
    }
    private void event(UUID id,UUID actor,String action,Instant now) { db.update("INSERT INTO care_circle_events VALUES (?,?,?,?,?)",UUID.randomUUID(),id,actor,action,Timestamp.from(now)); }
}
