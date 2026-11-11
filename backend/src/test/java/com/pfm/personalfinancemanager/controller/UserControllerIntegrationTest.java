package com.pfm.personalfinancemanager.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfm.personalfinancemanager.dto.request.ChangePasswordRequest;
import com.pfm.personalfinancemanager.dto.request.LoginRequest;
import com.pfm.personalfinancemanager.dto.request.RegisterRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateProfileRequest;
import com.pfm.personalfinancemanager.repository.PasswordResetTokenRepository;
import com.pfm.personalfinancemanager.repository.RefreshTokenRepository;
import com.pfm.personalfinancemanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {

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

    private String accessToken;

    @BeforeEach
    void setUpUser() throws Exception {
        passwordResetTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        RegisterRequest registerReq = RegisterRequest.builder()
                .fullName("Pham Van Cuong")
                .email("cuong.pham@pfm.local")
                .password("InitialPass@2026")
                .currency("VND")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        accessToken = data.get("accessToken").asText();
    }

    @Test
    void updateProfile_ShouldUpdateFullNameAndCurrency() throws Exception {
        UpdateProfileRequest updateReq = UpdateProfileRequest.builder()
                .fullName("Pham Van Cuong Updated")
                .currency("USD")
                .build();

        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Pham Van Cuong Updated"))
                .andExpect(jsonPath("$.data.currency").value("USD"));
    }

    @Test
    void changePassword_ShouldVerifyCurrentPasswordAndUpdateHash() throws Exception {
        ChangePasswordRequest changeReq = ChangePasswordRequest.builder()
                .currentPassword("InitialPass@2026")
                .newPassword("UpdatedPass@2026")
                .confirmPassword("UpdatedPass@2026")
                .build();

        mockMvc.perform(post("/api/v1/users/me/change-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeReq)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("cuong.pham@pfm.local", "UpdatedPass@2026"))))
                .andExpect(status().isOk());
    }

    @Test
    void uploadAvatar_ShouldAcceptValidPngAndRejectFakeImageBytes() throws Exception {
        // Valid PNG magic header bytes
        byte[] validPng = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D};
        MockMultipartFile validFile = new MockMultipartFile("file", "avatar.png", "image/png", validPng);

        mockMvc.perform(multipart("/api/v1/users/me/avatar")
                        .file(validFile)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarUrl").value(org.hamcrest.Matchers.startsWith("/uploads/avatars/")));

        // Fake PNG containing plain script bytes
        MockMultipartFile fakeFile = new MockMultipartFile(
                "file", "malware.png", "image/png", "<?php echo 'hack'; ?>".getBytes());

        mockMvc.perform(multipart("/api/v1/users/me/avatar")
                        .file(fakeFile)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
    }
}

