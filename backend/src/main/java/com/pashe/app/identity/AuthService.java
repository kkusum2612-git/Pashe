package com.pashe.app.identity;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class AuthService {
    private final JdbcTemplate db;
    private final UserService users;
    private final PasswordEncoder passwords;
    private final AuthMailService mail;
    private final RateLimiter limits;
    private final String dummyHash;
    public AuthService(JdbcTemplate db, UserService users, PasswordEncoder passwords, AuthMailService mail, RateLimiter limits) {
        this.db=db; this.users=users; this.passwords=passwords; this.mail=mail; this.limits=limits;
        dummyHash=passwords.encode(TokenSupport.random());
    }
    public record Login(UUID userId, UUID sessionId, String refreshToken) {}
    @Transactional
    public void register(String name, String email, String phone, String password) {
        limits.check("register:" + UserService.normalize(email), 3);
        if (users.byEmail(email).isPresent()) return; // Do not disclose account existence.
        UUID id=users.create(name,email,phone,passwords.encode(password),false);
        issueEmail(id,email,false);
    }
    @Transactional
    public Login login(String email, String password) {
        limits.check("login:" + UserService.normalize(email), 10);
        db.queryForList("SELECT id FROM users WHERE lower(email)=? FOR UPDATE", UserService.normalize(email));
        var account=users.byEmail(email);
        boolean matches=passwords.matches(password, account.map(UserService.Account::passwordHash).orElse(dummyHash));
        if (account.isEmpty() || !matches || !account.get().enabled()) throw new ResponseStatusException(UNAUTHORIZED);
        if (!account.get().emailVerified()) throw new ResponseStatusException(FORBIDDEN);
        UUID session=UUID.randomUUID();
        db.update("INSERT INTO auth_sessions(id,user_id,expires_at) VALUES (?,?,CURRENT_TIMESTAMP + INTERVAL '7 days')", session, account.get().id());
        return new Login(account.get().id(), session, newRefresh(session));
    }
    private String newRefresh(UUID session) {
        String raw=TokenSupport.random();
        db.update("INSERT INTO refresh_tokens(token_hash,session_id) VALUES (?,?)", TokenSupport.hash(raw),session);
        return raw;
    }
    @Transactional
    public Login refresh(String raw) {
        if (raw == null || raw.length() > 200) return null;
        var rows=db.queryForList("""
            SELECT s.id,s.user_id,s.revoked_at,s.expires_at,t.used_at FROM refresh_tokens t
            JOIN auth_sessions s ON s.id=t.session_id WHERE t.token_hash=? FOR UPDATE OF s,t
            """,TokenSupport.hash(raw));
        if (rows.isEmpty()) return null;
        var row=rows.getFirst();
        UUID session=(UUID)row.get("id");
        if (row.get("used_at") != null) {
            // Return instead of throwing: replay revocation must commit.
            db.update("UPDATE auth_sessions SET revoked_at=CURRENT_TIMESTAMP WHERE id=?",session);
            return null;
        }
        var user=users.byId((UUID)row.get("user_id"));
        if (row.get("revoked_at") != null || ((java.sql.Timestamp)row.get("expires_at")).toInstant().isBefore(Instant.now()) || !user.enabled() || !user.emailVerified()) return null;
        db.update("UPDATE refresh_tokens SET used_at=CURRENT_TIMESTAMP WHERE token_hash=?",TokenSupport.hash(raw));
        return new Login(user.id(),session,newRefresh(session));
    }
    @Transactional
    public void logout(String refresh) {
        if (refresh != null) db.update("UPDATE auth_sessions SET revoked_at=CURRENT_TIMESTAMP WHERE id IN (SELECT session_id FROM refresh_tokens WHERE token_hash=?)",TokenSupport.hash(refresh));
    }
    @Transactional
    public void requestEmail(String email, boolean reset) {
        limits.check((reset ? "reset:" : "verify:") + UserService.normalize(email),3);
        users.byEmail(email).filter(UserService.Account::enabled).filter(a -> reset || !a.emailVerified()).ifPresent(a -> issueEmail(a.id(),a.email(),reset));
    }
    private void issueEmail(UUID id, String email, boolean reset) {
        // Lock user first to serialize with password changes and token issuance.
        db.queryForObject("SELECT id FROM users WHERE id=? FOR UPDATE",UUID.class,id);
        String table=reset ? "password_reset_tokens" : "email_verification_tokens";
        db.update("UPDATE " + table + " SET used_at=CURRENT_TIMESTAMP WHERE user_id=? AND used_at IS NULL",id);
        String raw=TokenSupport.random();
        db.update("INSERT INTO " + table + " (id,user_id,token_hash,expires_at) VALUES (?,?,?,?)",UUID.randomUUID(),id,TokenSupport.hash(raw),java.sql.Timestamp.from(Instant.now().plus(reset ? Duration.ofMinutes(30) : Duration.ofHours(24))));
        mail.send(email,raw,reset);
    }
    @Transactional
    public void consumeEmail(String raw, String password) {
        String table=password == null ? "email_verification_tokens" : "password_reset_tokens";
        var ids=db.queryForList("SELECT user_id FROM " + table + " WHERE token_hash=?",UUID.class,TokenSupport.hash(raw));
        if (ids.isEmpty()) throw new ResponseStatusException(GONE);
        UUID id=ids.getFirst();
        db.queryForObject("SELECT id FROM users WHERE id=? FOR UPDATE",UUID.class,id);
        int changed=db.update("UPDATE " + table + " SET used_at=CURRENT_TIMESTAMP WHERE token_hash=? AND used_at IS NULL AND expires_at>CURRENT_TIMESTAMP",TokenSupport.hash(raw));
        if (changed != 1) throw new ResponseStatusException(GONE);
        if (password == null) db.update("UPDATE users SET email_verified=TRUE,updated_at=CURRENT_TIMESTAMP WHERE id=?",id);
        else {
            db.update("UPDATE users SET password_hash=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",passwords.encode(password),id);
            db.update("UPDATE auth_sessions SET revoked_at=CURRENT_TIMESTAMP WHERE user_id=?",id);
            db.update("UPDATE password_reset_tokens SET used_at=CURRENT_TIMESTAMP WHERE user_id=? AND used_at IS NULL",id);
        }
    }
}
