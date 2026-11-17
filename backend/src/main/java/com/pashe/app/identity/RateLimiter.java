package com.pashe.app.identity;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class RateLimiter {
    private final JdbcTemplate db;
    public RateLimiter(JdbcTemplate db) { this.db = db; }
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ResponseStatusException.class)
    public void check(String key, int limit) {
        Integer hits = db.queryForObject("""
            INSERT INTO auth_rate_limits(bucket_key,hits,expires_at) VALUES (?,1,CURRENT_TIMESTAMP + INTERVAL '15 minutes')
            ON CONFLICT(bucket_key) DO UPDATE SET
              hits=CASE WHEN auth_rate_limits.expires_at<CURRENT_TIMESTAMP THEN 1 ELSE auth_rate_limits.hits+1 END,
              expires_at=CASE WHEN auth_rate_limits.expires_at<CURRENT_TIMESTAMP THEN CURRENT_TIMESTAMP + INTERVAL '15 minutes' ELSE auth_rate_limits.expires_at END
            RETURNING hits
            """, Integer.class, TokenSupport.hash(key));
        if (hits != null && hits > limit) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
    }
}
