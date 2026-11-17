package com.pashe.app.identity;

import java.time.*;
import java.util.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
    private final AuthService auth;
    private final UserService users;
    private final JwtEncoder encoder;
    private final RateLimiter limits;
    private final boolean secure;
    public AuthController(AuthService auth, UserService users, JwtEncoder encoder, RateLimiter limits, @Value("${pashe.auth.secure-cookies}") boolean secure) {
        this.auth=auth; this.users=users; this.encoder=encoder; this.limits=limits; this.secure=secure;
    }
    public record Register(@NotBlank @Size(max=100) String fullName, @NotBlank @Email @Size(max=254) String email,
        @NotBlank @Pattern(regexp="(?:\\+8801|01)[3-9]\\d{8}") String phone, @NotBlank @Size(min=8,max=72) String password) {}
    public record Credentials(@NotBlank @Email @Size(max=254) String email, @NotBlank @Size(max=72) String password) {}
    public record EmailRequest(@NotBlank @Email @Size(max=254) String email) {}
    public record TokenRequest(@NotBlank @Size(max=200) String token) {}
    public record ResetRequest(@NotBlank @Size(max=200) String token, @NotBlank @Size(min=8,max=72) String password) {}
    public record Profile(@NotBlank @Size(max=100) String fullName, @NotBlank @Pattern(regexp="(?:\\+8801|01)[3-9]\\d{8}") String phone) {}

    private void rate(HttpServletRequest req) { limits.check("ip:" + req.getRemoteAddr(),60); }
    private void passwordBytes(String password) { if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) throw new ResponseStatusException(HttpStatus.BAD_REQUEST); }
    @GetMapping("/auth/csrf") public Map<String,String> csrf(CsrfToken token) { return Map.of("token",token.getToken()); }
    @PostMapping("/auth/register") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@Valid @RequestBody Register data, HttpServletRequest req) { rate(req); passwordBytes(data.password()); auth.register(data.fullName(),data.email(),data.phone(),data.password()); }
    @PostMapping("/auth/login")
    public UserService.UserView login(@Valid @RequestBody Credentials data, HttpServletRequest req, HttpServletResponse res) {
        rate(req); passwordBytes(data.password()); return signedIn(auth.login(data.email(),data.password()),res);
    }
    @PostMapping("/auth/refresh")
    public UserService.UserView refresh(@CookieValue(name="PASHE_REFRESH",required=false) String refresh, HttpServletRequest req, HttpServletResponse res) {
        rate(req); var login=auth.refresh(refresh);
        if (login == null) { clear(res); throw new ResponseStatusException(HttpStatus.UNAUTHORIZED); }
        return signedIn(login,res);
    }
    @PostMapping("/auth/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@CookieValue(name="PASHE_REFRESH",required=false) String refresh, HttpServletResponse res) { auth.logout(refresh); clear(res); }
    @GetMapping("/auth/session") public UserService.UserView session(Authentication authentication) { return users.view(UUID.fromString(authentication.getName())); }
    @PostMapping("/auth/verify-email") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verify(@Valid @RequestBody TokenRequest data,HttpServletRequest req) { rate(req); auth.consumeEmail(data.token(),null); }
    @PostMapping("/auth/resend-verification") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resend(@Valid @RequestBody EmailRequest data,HttpServletRequest req) { rate(req); auth.requestEmail(data.email(),false); }
    @PostMapping("/auth/forgot-password") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgot(@Valid @RequestBody EmailRequest data,HttpServletRequest req) { rate(req); auth.requestEmail(data.email(),true); }
    @PostMapping("/auth/reset-password") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody ResetRequest data,HttpServletRequest req,HttpServletResponse res) { rate(req); passwordBytes(data.password()); auth.consumeEmail(data.token(),data.password()); clear(res); }
    @PatchMapping("/users/me") public UserService.UserView profile(@Valid @RequestBody Profile data, Authentication authentication) { return users.update(UUID.fromString(authentication.getName()),data.fullName(),data.phone()); }
    @GetMapping("/users/me") public UserService.UserView profile(Authentication authentication) { return session(authentication); }
    @GetMapping({"/admin/access","/ambulance/access","/pharmacy/access"}) public Map<String,String> access() { return Map.of("status","authorized"); }
    private UserService.UserView signedIn(AuthService.Login login,HttpServletResponse res) {
        Instant now=Instant.now();
        var claims=JwtClaimsSet.builder().issuer("pashe").subject(login.userId().toString()).issuedAt(now).expiresAt(now.plusSeconds(900)).claim("sid",login.sessionId().toString()).build();
        String jwt=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        cookie(res,"PASHE_ACCESS",jwt,"/api",900); cookie(res,"PASHE_REFRESH",login.refreshToken(),"/api/v1/auth",604800);
        return users.view(login.userId());
    }
    private void clear(HttpServletResponse res) { cookie(res,"PASHE_ACCESS","","/api",0); cookie(res,"PASHE_REFRESH","","/api/v1/auth",0); }
    private void cookie(HttpServletResponse res,String name,String value,String path,long seconds) {
        res.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from(name,value).httpOnly(true).secure(secure).sameSite("Strict").path(path).maxAge(seconds).build().toString());
    }
}
