package com.pashe.app.identity;

import java.time.*;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.ObjectPostProcessor;


@Configuration
@EnableMethodSecurity
@EnableScheduling
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean SecretKeySpec signingKey(@Value("${pashe.auth.jwt-secret}") String secret) {
        byte[] key=Base64.getDecoder().decode(secret);
        if (key.length < 32) throw new IllegalArgumentException("JWT_SECRET must encode at least 32 random bytes");
        return new SecretKeySpec(key,"HmacSHA256");
    }
    @Bean JwtEncoder jwtEncoder(SecretKeySpec key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }
    @Bean JwtDecoder jwtDecoder(SecretKeySpec key) {
        var decoder=NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("pashe"));
        return decoder;
    }
    @Bean SecurityFilterChain security(HttpSecurity http, JdbcTemplate db, UserService users, @Value("${pashe.auth.secure-cookies}") boolean secure) throws Exception {
        var csrf=new CookieCsrfTokenRepository();
        csrf.setCookiePath("/api");
        csrf.setHeaderName("X-CSRF-TOKEN");
        csrf.setCookieCustomizer(cookie -> cookie.httpOnly(true).secure(secure).sameSite("Strict"));
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(c -> c
                .csrfTokenRepository(csrf)
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .withObjectPostProcessor(new ObjectPostProcessor<CsrfFilter>() {
                    @Override
                    public <O extends CsrfFilter> O postProcess(O filter) {
                        filter.setRequireCsrfProtectionMatcher(
                            CsrfFilter.DEFAULT_CSRF_MATCHER
                        );
                        return filter;
                    }
                }))
            .formLogin(c -> c.disable()).httpBasic(c -> c.disable()).logout(c -> c.disable())
            .requestCache(c -> c.disable())
            .authorizeHttpRequests(a -> a.requestMatchers("/api/v1/health", "/error").permitAll()
                .requestMatchers("/api/v1/auth/session").authenticated()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/users/me", "/api/v1/ws").authenticated()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET,"/api/v1/ambulance/verified").authenticated()
                .requestMatchers("/api/v1/ambulance/**").hasRole("AMBULANCE_OPERATOR")
                .requestMatchers("/api/v1/pharmacy/**").hasRole("PHARMACY_STAFF")
                .requestMatchers("/api/v1/blood/donor-profile").authenticated()
                .requestMatchers("/api/v1/guardian/**").authenticated()
                .anyRequest().denyAll())
            .exceptionHandling(e -> e.authenticationEntryPoint((req,res,ex) -> res.sendError(401)).accessDeniedHandler((req,res,ex) -> res.sendError(403)))
            .oauth2ResourceServer(o -> o.bearerTokenResolver(req -> {
                // Public recovery endpoints must work with an expired access cookie.
                String path=req.getRequestURI();
                if (path.startsWith("/api/v1/auth/") && !path.endsWith("/session")) return null;
                String authorization=req.getHeader("Authorization");
                if (authorization != null && authorization.startsWith("Bearer ")) return authorization.substring(7);
                if (req.getCookies()!=null) for (var cookie:req.getCookies()) if (cookie.getName().equals("PASHE_ACCESS")) return cookie.getValue();
                return null;
            }).jwt(j -> j.jwtAuthenticationConverter(jwt -> {
                try {
                    UUID id=UUID.fromString(jwt.getSubject());
                    UUID session=UUID.fromString(jwt.getClaimAsString("sid"));
                    Integer count=db.queryForObject("SELECT count(*) FROM auth_sessions WHERE id=? AND user_id=? AND revoked_at IS NULL AND expires_at>CURRENT_TIMESTAMP",Integer.class,session,id);
                    var account=users.byId(id);
                    if (count == null || count != 1 || !account.enabled() || !account.emailVerified()) throw new IllegalArgumentException();
                    return new JwtAuthenticationToken(jwt,users.view(id).roles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList(),id.toString());
                } catch (IllegalArgumentException ex) { throw new OAuth2AuthenticationException("invalid_token"); }
            })));
        return http.build();
    }
}
