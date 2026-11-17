package com.pashe.app.identity;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class UserService {
    private final JdbcTemplate db;
    public UserService(JdbcTemplate db) { this.db = db; }
    public record Account(UUID id, String fullName, String email, String phone, String passwordHash, boolean emailVerified, boolean enabled) {}
    public record UserView(UUID id, String fullName, String email, String phone, boolean emailVerified, List<String> roles) {}
    private List<Account> query(String condition, Object value) {
        return db.query("SELECT * FROM users WHERE " + condition, (r, n) -> new Account(r.getObject("id", UUID.class), r.getString("full_name"), r.getString("email"), r.getString("phone_number"), r.getString("password_hash"), r.getBoolean("email_verified"), r.getBoolean("enabled")), value);
    }
    public Optional<Account> byEmail(String email) { return query("lower(email) = ?", normalize(email)).stream().findFirst(); }
    public Account byId(UUID id) { return query("id = ?", id).stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)); }
    public static String normalize(String email) { return email.strip().toLowerCase(Locale.ROOT); }
    public UserView view(UUID id) {
        Account a = byId(id);
        return new UserView(a.id(), a.fullName(), a.email(), a.phone(), a.emailVerified(), db.queryForList("SELECT r.name FROM roles r JOIN user_roles ur ON ur.role_id=r.id WHERE ur.user_id=? ORDER BY r.name", String.class, id));
    }
    @Transactional
    public UUID create(String name, String email, String phone, String hash, boolean verified) {
        UUID id = UUID.randomUUID();
        db.update("INSERT INTO users(id,full_name,email,phone_number,password_hash,email_verified) VALUES (?,?,?,?,?,?)", id, name.strip(), normalize(email), phone, hash, verified);
        db.update("INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE name='USER'", id);
        return id;
    }
    @Transactional
    public UserView update(UUID id, String name, String phone) {
        db.update("UPDATE users SET full_name=?,phone_number=?,updated_at=CURRENT_TIMESTAMP WHERE id=?", name.strip(), phone, id);
        return view(id);
    }
}
