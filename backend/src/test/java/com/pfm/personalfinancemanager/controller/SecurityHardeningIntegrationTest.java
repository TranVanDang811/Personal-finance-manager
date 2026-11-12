package com.pfm.personalfinancemanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfm.personalfinancemanager.dto.request.ForgotPasswordRequest;
import com.pfm.personalfinancemanager.dto.request.LoginRequest;
import com.pfm.personalfinancemanager.dto.request.RegisterRequest;
import com.pfm.personalfinancemanager.repository.PasswordResetTokenRepository;
import com.pfm.personalfinancemanager.repository.RefreshTokenRepository;
import com.pfm.personalfinancemanager.repository.UserRepository;
import com.pfm.personalfinancemanager.security.InMemoryRateLimiterService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.security.rate-limit.enabled=true",
        "app.security.rate-limit.max-requests=3",
        "app.security.rate-limit.window-seconds=60"
})
class SecurityHardeningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryRateLimiterService rateLimiterService;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        rateLimiterService.clear();
        passwordResetTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        rateLimiterService.clear();
        passwordResetTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void securityHeaders_ShouldBePresentOnApiResponses() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy", "camera=(), microphone=(), geolocation=()"));
    }

    @Test
    void authRateLimit_ShouldReturn429AfterExceedingThresholdAndIsolatePaths() throws Exception {
        LoginRequest badLogin = new LoginRequest("nonexistent@pfm.local", "WrongPass@123");
        String payload = objectMapper.writeValueAsString(badLogin);

        for (int i = 2; i >= 0; i--) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("X-RateLimit-Remaining", String.valueOf(i)));
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"));

        // Separate auth path bucket must remain unaffected
        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest("nobody@pfm.local");
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotReq)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Remaining", "2"));
    }

    @Test
    void tamperedJwt_ShouldReturn401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.tampered.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void uploadAvatarWithSpoofedMimeTypeAndPageSizeCap_ShouldBeEnforced() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .fullName("Security Tester")
                .email("sec.tester@pfm.local")
                .password("StrongPass@2026")
                .build();

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String accessToken = objectMapper.readTree(regResult.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

        // Spoofed executable/text payload masquerading as JPEG
        MockMultipartFile spoofedFile = new MockMultipartFile(
                "file",
                "evil.jpg",
                "image/jpeg",
                "MZ_NOT_A_REAL_IMAGE_PAYLOAD".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/users/me/avatar")
                        .file(spoofedFile)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));

        // Requesting oversized page (?size=500) must be clamped to max-page-size=100
        mockMvc.perform(get("/api/v1/transactions")
                        .param("size", "500")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageSize").value(100));
    }
}

