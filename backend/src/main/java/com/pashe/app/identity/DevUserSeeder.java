package com.pashe.app.identity;

import org.springframework.boot.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevUserSeeder implements ApplicationRunner {
    private final UserService users;
    private final PasswordEncoder encoder;
    private final boolean enabled;
    private final String password;
    public DevUserSeeder(UserService users,PasswordEncoder encoder,@Value("${pashe.auth.seed-enabled:false}") boolean enabled,@Value("${pashe.auth.seed-password:}") String password) {
        this.users=users; this.encoder=encoder; this.enabled=enabled; this.password=password;
    }
    @Override public void run(ApplicationArguments args) {
        if (!enabled) return;
        if (password.length()<12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new IllegalStateException("Provide a 12–72 byte SEED_TEST_PASSWORD");
        if (users.byEmail("tester@pashe.local").isEmpty()) users.create("Pashe Test User","tester@pashe.local","01712345678",encoder.encode(password),true);
    }
}
