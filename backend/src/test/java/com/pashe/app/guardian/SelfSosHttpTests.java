package com.pashe.app.guardian;

import java.util.UUID;
import com.pashe.app.identity.UserService;
import com.pashe.app.guardian.sos.SelfSosService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties="pashe.guardian.scheduling-enabled=false")
@AutoConfigureMockMvc
@Transactional
class SelfSosHttpTests {
    @Autowired MockMvc mvc;@Autowired UserService users;@Autowired PasswordEncoder passwords;@Autowired SelfSosService service;
    @Test void jwtCsrfOwnershipAndLogout()throws Exception{
        mvc.perform(get("/api/v1/guardian/incidents")).andExpect(status().isUnauthorized());
        String email=UUID.randomUUID()+"@example.test",password="Demo-Test-2026!";
        users.create("Synthetic owner",email,"01712345678",passwords.encode(password),true);
        var cookies=mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType("application/json").content("{\"email\":\""+email+"\",\"password\":\""+password+"\"}")).andExpect(status().isOk()).andReturn().getResponse().getCookies();
        mvc.perform(post("/api/v1/guardian/sos/requests").cookie(cookies).contentType("application/json").content("{\"idempotencyKey\":\""+UUID.randomUUID()+"\"}")).andExpect(status().isForbidden());
        UUID other=users.create("Synthetic other",UUID.randomUUID()+"@example.test","01712345678","not-a-login-hash",true);
        var pending=service.start(other,UUID.randomUUID(),null);
        mvc.perform(get("/api/v1/guardian/sos/requests/"+pending.id()).cookie(cookies)).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/auth/logout").cookie(cookies).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/guardian/incidents").cookie(cookies)).andExpect(status().isUnauthorized());
    }
}
