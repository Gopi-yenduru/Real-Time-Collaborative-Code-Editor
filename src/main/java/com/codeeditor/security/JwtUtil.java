package com.codeeditor.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * Issues and validates HS256 JWTs. The token carries the username (subject)
 * plus {@code uid} and {@code email} claims so downstream components — including
 * the WebSocket handshake — can authorize without an extra database round-trip.
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Getter
    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    public String generateToken(UserDetailsImpl user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("email", user.getEmail())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtExpirationMs))
                .signWith(key())
                .compact();
    }

    public String getUsername(String token) {
        return parse(token).getSubject();
    }

    public Long getUserId(String token) {
        Object uid = parse(token).get("uid");
        return uid == null ? null : ((Number) uid).longValue();
    }

    public String getEmail(String token) {
        Object email = parse(token).get("email");
        return email == null ? null : email.toString();
    }

    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
        }
        return false;
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }

    /**
     * Builds a 256-bit HMAC key from {@code jwt.secret}. A valid Base64 secret of
     * at least 32 bytes is used directly; anything else (e.g. a human-readable
     * passphrase) is hashed with SHA-256 so any configured value yields a valid key.
     */
    private SecretKey key() {
        try {
            byte[] decoded = Decoders.BASE64.decode(jwtSecret);
            if (decoded.length >= 32) {
                return Keys.hmacShaKeyFor(decoded);
            }
        } catch (Exception ignored) {
            // Not Base64 — fall through to deriving a key from the raw passphrase.
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(jwtSecret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
