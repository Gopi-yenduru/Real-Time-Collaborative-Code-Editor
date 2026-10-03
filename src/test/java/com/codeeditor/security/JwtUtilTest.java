package com.codeeditor.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = "unit-test-secret-long-enough-to-derive-a-256-bit-key";

    private JwtUtil jwtUtil;

    private static JwtUtil withSecret(String secret, long expirationMs) {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "jwtSecret", secret);
        ReflectionTestUtils.setField(util, "jwtExpirationMs", expirationMs);
        return util;
    }

    private static UserDetailsImpl alice() {
        return new UserDetailsImpl(42L, "alice", "alice@example.com", null);
    }

    @BeforeEach
    void setUp() {
        jwtUtil = withSecret(SECRET, 60_000L);
    }

    @Test
    void tokenCarriesTheClaimsDownstreamComponentsRelyOn() {
        String token = jwtUtil.generateToken(alice());

        // The WebSocket handshake authorizes from these claims alone, with no DB hit.
        assertEquals("alice", jwtUtil.getUsername(token));
        assertEquals(42L, jwtUtil.getUserId(token));
        assertEquals("alice@example.com", jwtUtil.getEmail(token));
        assertTrue(jwtUtil.validate(token));
    }

    @Test
    void expiredTokenIsRejected() {
        // A negative lifetime means the token is issued already expired.
        String token = withSecret(SECRET, -60_000L).generateToken(alice());

        assertFalse(jwtUtil.validate(token));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String foreign = withSecret("a-completely-different-secret-of-sufficient-length", 60_000L)
                .generateToken(alice());

        assertFalse(jwtUtil.validate(foreign));
    }

    @Test
    void tamperedPayloadIsRejected() {
        String token = jwtUtil.generateToken(alice());
        String[] parts = token.split("[.]");
        // Re-signing is impossible without the key, so a edited payload must fail.
        String tampered = parts[0] + "." + parts[1].substring(0, parts[1].length() - 2) + "AA." + parts[2];

        assertFalse(jwtUtil.validate(tampered));
    }

    @Test
    void garbageIsRejectedRatherThanThrowing() {
        assertFalse(jwtUtil.validate("not-a-jwt"));
        assertFalse(jwtUtil.validate(""));
    }

    @Test
    void aBase64SecretIsUsedDirectlyAndStillRoundTrips() {
        // 32 raw bytes, Base64-encoded: the form the README recommends in production.
        String base64Key = Base64.getEncoder()
                .encodeToString("0123456789abcdef0123456789abcdef".getBytes());
        JwtUtil util = withSecret(base64Key, 60_000L);

        String token = util.generateToken(alice());

        assertTrue(util.validate(token));
        assertEquals(42L, util.getUserId(token));
    }
}
