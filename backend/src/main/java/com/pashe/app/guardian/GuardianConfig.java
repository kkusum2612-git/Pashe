package com.pashe.app.guardian;
import java.time.Clock;
import org.springframework.context.annotation.*;
@Configuration
public class GuardianConfig {
    @Bean public Clock guardianClock() { return Clock.systemUTC(); }
}
