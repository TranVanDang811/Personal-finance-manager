package com.pfm.personalfinancemanager.security;

import com.pfm.personalfinancemanager.entity.User;
import com.pfm.personalfinancemanager.enums.Role;
import com.pfm.personalfinancemanager.enums.UserStatus;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret",
                "test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256-signing");
        ReflectionTestUtils.setField(jwtService, "accessExpirationMs", 900_000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpirationMs", 604_800_000L);
        jwtService.init();

        User user = User.builder()
                .id("user-123")
                .email("tester@pfm.local")
                .password("encoded-password")
                .fullName("Tester")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();
        userDetails = new CustomUserDetails(user);
    }

    @Test
    void generateAndParseAccessToken_ShouldContainExpectedClaims() {
        String token = jwtService.generateAccessToken(userDetails);

        assertThat(jwtService.isTokenValid(token)).isTrue();
        Claims claims = jwtService.parseAndValidateToken(token);
        assertThat(jwtService.extractUserId(claims)).isEqualTo("user-123");
        assertThat(jwtService.extractJti(claims)).isNotBlank();
        assertThat(claims.get("email", String.class)).isEqualTo("tester@pfm.local");
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_USER");
    }

    @Test
    void isTokenValid_ShouldReturnFalseForTamperedToken() {
        String token = jwtService.generateAccessToken(userDetails);
        String tampered = token.substring(0, token.length() - 4) + "xxxx";

        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    void isTokenValid_ShouldReturnFalseForExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "accessExpirationMs", -1000L);
        String expiredToken = jwtService.generateAccessToken(userDetails);

        assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
    }
}

