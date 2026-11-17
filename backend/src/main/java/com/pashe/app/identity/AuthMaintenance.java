package com.pashe.app.identity;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuthMaintenance {
    private final JdbcTemplate db;
    public AuthMaintenance(JdbcTemplate db) { this.db=db; }
    @Scheduled(fixedDelay=3600000, initialDelay=3600000)
    public void cleanup() {
        db.update("DELETE FROM auth_sessions WHERE expires_at<CURRENT_TIMESTAMP");
        db.update("DELETE FROM email_verification_tokens WHERE expires_at<CURRENT_TIMESTAMP");
        db.update("DELETE FROM password_reset_tokens WHERE expires_at<CURRENT_TIMESTAMP");
        db.update("DELETE FROM auth_rate_limits WHERE expires_at<CURRENT_TIMESTAMP");
    }
}
