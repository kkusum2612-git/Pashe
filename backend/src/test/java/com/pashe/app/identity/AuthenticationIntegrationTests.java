package com.pashe.app.identity;

import java.util.UUID;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthenticationIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserService users;
    @Autowired PasswordEncoder passwords;
    @Autowired AuthService auth;
    @Autowired JdbcTemplate db;
    @Autowired RateLimiter limits;
    @MockitoBean AuthMailService mail;
    private final String password="Test-Password-2026!";
    private String email() { return UUID.randomUUID()+"@example.test"; }
    private UUID create(String email,boolean verified) { return users.create("Test User",email,"01712345678",passwords.encode(password),verified); }
    private Cookie[] login(String email) throws Exception {
        var result=mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType("application/json").content("{\"email\":\""+email+"\",\"password\":\""+password+"\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.roles[0]").value("USER")).andReturn();
        return result.getResponse().getCookies();
    }
    @Test void registrationVerificationAndSingleUseLink() throws Exception {
        String email=email();
        mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType("application/json")
            .content("{\"fullName\":\"Test User\",\"email\":\""+email+"\",\"phone\":\"01712345678\",\"password\":\""+password+"\"}"))
            .andExpect(status().isNoContent());
        var token=ArgumentCaptor.forClass(String.class); verify(mail).send(eq(email),token.capture(),eq(false));
        assertNotEquals(password,users.byEmail(email).orElseThrow().passwordHash());
        mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType("application/json").content("{\"email\":\""+email+"\",\"password\":\""+password+"\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/verify-email").with(csrf()).contentType("application/json").content("{\"token\":\""+token.getValue()+"\"}")).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/verify-email").with(csrf()).contentType("application/json").content("{\"token\":\""+token.getValue()+"\"}")).andExpect(status().isGone());
        login(email);
    }
    @Test void sessionProfileRolesAndLogout() throws Exception {
        String email=email(); UUID id=create(email,true); Cookie[] cookies=login(email);
        mvc.perform(get("/api/v1/auth/session").cookie(cookies)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));
        mvc.perform(patch("/api/v1/users/me").cookie(cookies).with(csrf()).contentType("application/json").content("{\"fullName\":\"Updated Name\",\"phone\":\"01812345678\",\"roles\":[\"ADMIN\"]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Updated Name"));
        mvc.perform(get("/api/v1/admin/access").cookie(cookies)).andExpect(status().isForbidden());
        db.update("INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE name='ADMIN'",id);
        mvc.perform(get("/api/v1/admin/access").cookie(cookies)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/logout").cookie(cookies).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/auth/session").cookie(cookies)).andExpect(status().isUnauthorized());
    }
    @Test void refreshRotatesAndReplayRevokesFamily() {
        String email=email(); create(email,true);
        var first=auth.login(email,password); var next=auth.refresh(first.refreshToken());
        assertNotNull(next); assertNotEquals(first.refreshToken(),next.refreshToken());
        assertNull(auth.refresh(first.refreshToken()));
        assertNull(auth.refresh(next.refreshToken()));
    }
    @Test void passwordResetRevokesAccessAndRefresh() throws Exception {
        String email=email(); create(email,true); Cookie[] cookies=login(email);
        auth.requestEmail(email,true);
        var token=ArgumentCaptor.forClass(String.class); verify(mail).send(eq(email),token.capture(),eq(true));
        auth.consumeEmail(token.getValue(),"New-Password-2026!");
        mvc.perform(get("/api/v1/auth/session").cookie(cookies)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").cookie(cookies).with(csrf())).andExpect(status().isUnauthorized());
        assertTrue(passwords.matches("New-Password-2026!",users.byEmail(email).orElseThrow().passwordHash()));
    }
    @Test void expiredEmailTokenIsRejected() {
        String email=email(); UUID id=create(email,false); auth.requestEmail(email,false);
        var token=ArgumentCaptor.forClass(String.class); verify(mail).send(eq(email),token.capture(),eq(false));
        db.update("UPDATE email_verification_tokens SET expires_at=CURRENT_TIMESTAMP - INTERVAL '1 minute' WHERE user_id=?",id);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,() -> auth.consumeEmail(token.getValue(),null));
        assertFalse(users.byId(id).emailVerified());
    }
    @Test void anonymousCsrfAndValidationBoundaries() throws Exception {
        mvc.perform(get("/api/v1/health")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isString());
        mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType("application/json").content("{}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/auth/forgot-password").with(csrf()).contentType("application/json").content("{\"email\":\""+email()+"\"}")).andExpect(status().isNoContent());
        verifyNoInteractions(mail);
    }
    @Test void databaseRateLimitPersistsRejections() {
        String key="test:"+UUID.randomUUID();
        limits.check(key,2); limits.check(key,2);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,() -> limits.check(key,2));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,() -> limits.check(key,2));
    }
}
