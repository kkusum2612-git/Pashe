package com.pashe.app.identity;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

final class TokenSupport {
    private static final SecureRandom RANDOM = new SecureRandom();
    private TokenSupport() {}
    static String random() { byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
