package com.pfm.personalfinancemanager.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Getter
    @Value("${app.jwt.access-expiration-ms}")
    private long accessExpirationMs;

    @Getter
    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private SecretKey signingKey;

    @PostConstruct
    void init() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret key must be at least 256 bits (32 bytes)");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(CustomUserDetails userDetails) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(accessExpirationMs);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userDetails.getId())
                .claim("email", userDetails.getEmail())
                .claim("role", userDetails.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseAndValidateToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            parseAndValidateToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Invalid JWT token: {}", ex.getMessage());
            return false;
        }
    }

    public String extractUserId(Claims claims) {
        return claims.getSubject();
    }

    public String extractJti(Claims claims) {
        return claims.getId();
    }

    public Instant extractExpiration(Claims claims) {
        return claims.getExpiration().toInstant();
    }
}
