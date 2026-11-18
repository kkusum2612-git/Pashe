package com.pashe.app.guardian.sos;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SelfSosRepository {
    private final JdbcTemplate db;
    public SelfSosRepository(JdbcTemplate db) { this.db = db; }
    public record Request(UUID id, UUID recipient, String status, Instant deadline, UUID incident, String address) {}
    public record Incident(UUID id, UUID recipient, Instant openedAt, Instant resolvedAt, String address) {}
    public record Point(double latitude, double longitude, double accuracy, Instant observedAt) {}
    private static Timestamp ts(Instant value) { return Timestamp.from(value); }
    private final RowMapper<Request> requests = (r,n) -> new Request(r.getObject("id",UUID.class),r.getObject("recipient_id",UUID.class),r.getString("status"),r.getTimestamp("activation_deadline").toInstant(),r.getObject("incident_id",UUID.class),r.getString("pickup_address"));
    private final RowMapper<Incident> incidents = (r,n) -> new Incident(r.getObject("id",UUID.class),r.getObject("recipient_id",UUID.class),r.getTimestamp("opened_at").toInstant(),r.getTimestamp("resolved_at")==null?null:r.getTimestamp("resolved_at").toInstant(),r.getString("pickup_address"));
    public Optional<Request> request(UUID id) { return db.query("SELECT * FROM self_sos_requests WHERE id=?",requests,id).stream().findFirst(); }
    public Optional<Request> existing(UUID user,UUID key) {
        return db.query("SELECT * FROM self_sos_requests WHERE recipient_id=? AND (idempotency_key=? OR status='COUNTDOWN') ORDER BY (idempotency_key=?) DESC,created_at DESC LIMIT 1",requests,user,key,key).stream().findFirst();
    }
    public List<Request> latest(UUID user) { return db.query("SELECT * FROM self_sos_requests WHERE recipient_id=? ORDER BY created_at DESC,id LIMIT 1",requests,user); }
    public Request create(UUID user,UUID key,String address,Instant now) {
        UUID id=UUID.randomUUID();
        db.update("INSERT INTO self_sos_requests(id,recipient_id,idempotency_key,status,created_at,activation_deadline,pickup_address) VALUES (?,?,?,'COUNTDOWN',?,?,?)",id,user,key,ts(now),ts(now.plusSeconds(10)),address);
        return request(id).orElseThrow();
    }
    public void cancel(UUID id,Instant now) { db.update("UPDATE self_sos_requests SET status='CANCELED',canceled_at=? WHERE id=?",ts(now),id); }
    public void activate(UUID id,UUID incident) { db.update("UPDATE self_sos_requests SET status='ACTIVATED',incident_id=? WHERE id=?",incident,id); }
    public List<Request> due(Instant now) { return db.query("SELECT * FROM self_sos_requests WHERE status='COUNTDOWN' AND activation_deadline<=? ORDER BY activation_deadline LIMIT 100",requests,ts(now)); }
    public Optional<Incident> incident(UUID id) { return db.query("SELECT * FROM self_sos_incidents WHERE id=?",incidents,id).stream().findFirst(); }
    public Optional<Incident> active(UUID user) { return db.query("SELECT * FROM self_sos_incidents WHERE recipient_id=? AND resolved_at IS NULL",incidents,user).stream().findFirst(); }
    public Incident open(UUID user,String address,Instant now) {
        UUID id=UUID.randomUUID();
        db.update("INSERT INTO self_sos_incidents(id,recipient_id,opened_at,pickup_address) VALUES (?,?,?,?)",id,user,ts(now),address);
        return incident(id).orElseThrow();
    }
    public List<Incident> list(UUID actor,int page) {
        return db.query("""
            SELECT i.* FROM self_sos_incidents i WHERE recipient_id=? OR
              (resolved_at IS NULL AND EXISTS (SELECT 1 FROM care_circle_memberships m
               WHERE m.care_recipient_id=i.recipient_id AND m.invitee_id=?
               AND m.status='ACTIVE' AND m.relationship_type='GUARDIAN'))
            ORDER BY opened_at DESC,id LIMIT 50 OFFSET ?
            """,incidents,actor,actor,page*50);
    }
    public void resolve(UUID id,String reason,Instant now) { db.update("UPDATE self_sos_incidents SET resolved_at=?,resolution_reason=? WHERE id=?",ts(now),reason,id); }
    public Optional<Point> point(UUID user) {
        return db.query("SELECT * FROM self_sos_locations WHERE user_id=?",(r,n)->new Point(r.getDouble("latitude"),r.getDouble("longitude"),r.getDouble("accuracy"),r.getTimestamp("observed_at").toInstant()),user).stream().findFirst();
    }
    public boolean savePoint(UUID user,Point point) {
        return db.update("""
            INSERT INTO self_sos_locations VALUES (?,?,?,?,?) ON CONFLICT(user_id) DO UPDATE SET
            latitude=excluded.latitude,longitude=excluded.longitude,accuracy=excluded.accuracy,observed_at=excluded.observed_at
            WHERE self_sos_locations.observed_at<excluded.observed_at
            """,user,point.latitude(),point.longitude(),point.accuracy(),ts(point.observedAt()))==1;
    }
    public void clearPoint(UUID user) { db.update("DELETE FROM self_sos_locations WHERE user_id=?",user); }
    public void purgePoints(Instant cutoff) { db.update("DELETE FROM self_sos_locations WHERE observed_at<?",ts(cutoff)); }
}
