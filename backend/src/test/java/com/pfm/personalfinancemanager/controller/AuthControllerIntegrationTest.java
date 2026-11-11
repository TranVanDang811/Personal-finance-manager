package com.pfm.personalfinancemanager.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfm.personalfinancemanager.dto.request.ForgotPasswordRequest;
import com.pfm.personalfinancemanager.dto.request.LoginRequest;
import com.pfm.personalfinancemanager.dto.request.LogoutRequest;
import com.pfm.personalfinancemanager.dto.request.RefreshTokenRequest;
import com.pfm.personalfinancemanager.dto.request.RegisterRequest;
import com.pfm.personalfinancemanager.dto.request.ResetPasswordRequest;
import com.pfm.personalfinancemanager.entity.PasswordResetToken;
import com.pfm.personalfinancemanager.entity.User;
import com.pfm.personalfinancemanager.repository.PasswordResetTokenRepository;
import com.pfm.personalfinancemanager.repository.RefreshTokenRepository;
import com.pfm.personalfinancemanager.repository.UserRepository;
import com.pfm.personalfinancemanager.util.TokenHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @BeforeEach
    void cleanDatabase() {
        passwordResetTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerLoginRefreshAndLogout_FullLifecycleShouldWorkSecurely() throws Exception {
        // 1. Register
        RegisterRequest registerReq = RegisterRequest.builder()
                .fullName("Tran Minh Tuan")
                .email("tuan.tran@pfm.local")
                .password("StrongPass@2026")
                .currency("VND")
                .build();

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("tuan.tran@pfm.local"))
                .andExpect(jsonPath("$.data.user.password").doesNotExist())
                .andReturn();

        // 2. Duplicate email should fail with 409
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RESOURCE"));

        // 3. Unauthenticated /me should fail with 401
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        // 4. Login with wrong password should fail with 401
        LoginRequest badLogin = new LoginRequest("tuan.tran@pfm.local", "WrongPass@123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));

        // 5. Login with right password
        LoginRequest goodLogin = new LoginRequest("tuan.tran@pfm.local", "StrongPass@2026");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goodLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        JsonNode loginData = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("data");
        String accessToken = loginData.get("accessToken").asText();
        String refreshToken = loginData.get("refreshToken").asText();

        // 6. Access protected /me with access token
        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("tuan.tran@pfm.local"));

        // 7. Refresh token rotation
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode refreshedData = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).get("data");
        String newAccessToken = refreshedData.get("accessToken").asText();
        String newRefreshToken = refreshedData.get("refreshToken").asText();
        assertThat(newRefreshToken).isNotEqualTo(refreshToken);

        // 8. Reusing old refresh token triggers reuse detection & revokes new refresh token too
        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(newRefreshToken))))
                .andExpect(status().isUnauthorized());

        // 9. Logout blacklists current access token
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LogoutRequest(newRefreshToken))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken))
                .andExpect(status().isUnauthorized());

        // Ensure initial register token was valid too
        assertThat(registerResult.getResponse().getContentAsString()).contains("accessToken");
    }

    @Test
    void forgotAndResetPassword_ShouldAllowLoginWithNewPasswordOnly() throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .fullName("Le Thi Hoa")
                .email("hoa.le@pfm.local")
                .password("OldPass@2026")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Forgot password always returns 200 even for unknown emails (anti-enumeration)
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("unknown@pfm.local"))))
                .andExpect(status().isOk());

        // Store a known reset token for the user to simulate clicking email link
        User user = userRepository.findByEmailIgnoreCase("hoa.le@pfm.local").orElseThrow();
        String rawResetToken = "known-reset-token-123456";
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .tokenHash(TokenHashUtil.sha256Hex(rawResetToken))
                .expiryDate(Instant.now().plusSeconds(900))
                .used(false)
                .build());

        ResetPasswordRequest resetReq = ResetPasswordRequest.builder()
                .token(rawResetToken)
                .newPassword("NewPass@2026")
                .confirmPassword("NewPass@2026")
                .build();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk());

        // Reusing reset token fails
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isBadRequest());

        // Old password fails, new password succeeds
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("hoa.le@pfm.local", "OldPass@2026"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("hoa.le@pfm.local", "NewPass@2026"))))
                .andExpect(status().isOk());
    }
}

