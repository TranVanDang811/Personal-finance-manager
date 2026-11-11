package com.pfm.personalfinancemanager.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfm.personalfinancemanager.dto.request.CreateAccountRequest;
import com.pfm.personalfinancemanager.dto.request.RegisterRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateAccountRequest;
import com.pfm.personalfinancemanager.enums.AccountStatus;
import com.pfm.personalfinancemanager.enums.AccountType;
import com.pfm.personalfinancemanager.repository.AccountRepository;
import com.pfm.personalfinancemanager.repository.PasswordResetTokenRepository;
import com.pfm.personalfinancemanager.repository.RefreshTokenRepository;
import com.pfm.personalfinancemanager.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
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

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private String tokenUserA;
    private String tokenUserB;

    @BeforeEach
    void setUp() throws Exception {
        cleanAll();

        tokenUserA = registerAndGetToken("User A", "user.a@pfm.local");
        tokenUserB = registerAndGetToken("User B", "user.b@pfm.local");
    }

    @AfterEach
    void tearDown() {
        cleanAll();
    }

    private void cleanAll() {
        accountRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String registerAndGetToken(String name, String email) throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .fullName(name)
                .email(email)
                .password("StrongPass@2026")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
    }

    @Test
    void crudAndSummary_ShouldManageAccountsAndEnforceOwnershipIsolation() throws Exception {
        // 1. User A creates Cash account (1,500,000) and Bank account (8,500,000)
        CreateAccountRequest cashReq = CreateAccountRequest.builder()
                .accountName("Ví Tiền Mặt")
                .accountType(AccountType.CASH)
                .initialBalance(new BigDecimal("1500000"))
                .description("Chi tiêu hằng ngày")
                .build();

        MvcResult cashRes = mockMvc.perform(post("/api/v1/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cashReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.currentBalance").value(1500000))
                .andReturn();

        String cashAccountId = objectMapper.readTree(cashRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        CreateAccountRequest bankReq = CreateAccountRequest.builder()
                .accountName("Vietcombank")
                .accountType(AccountType.BANK_ACCOUNT)
                .initialBalance(new BigDecimal("8500000"))
                .build();

        mockMvc.perform(post("/api/v1/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bankReq)))
                .andExpect(status().isCreated());

        // 2. Duplicate name for User A rejected (409), while User B can use the same name
        mockMvc.perform(post("/api/v1/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cashReq)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cashReq)))
                .andExpect(status().isCreated());

        // 3. Summary returns exact BigDecimal sum (10,000,000)
        mockMvc.perform(get("/api/v1/accounts/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalActiveBalance").value(10000000))
                .andExpect(jsonPath("$.data.activeAccountCount").value(2));

        // 4. Filter by account type
        mockMvc.perform(get("/api/v1/accounts?type=BANK_ACCOUNT")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].accountName").value("Vietcombank"));

        // 5. IDOR checks: User B cannot view, update, archive or delete User A's account
        mockMvc.perform(get("/api/v1/accounts/" + cashAccountId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED_OWNERSHIP"));

        UpdateAccountRequest updateReq = UpdateAccountRequest.builder()
                .accountName("Hacked Account")
                .accountType(AccountType.CASH)
                .allowNegative(false)
                .status(AccountStatus.ACTIVE)
                .build();

        mockMvc.perform(put("/api/v1/accounts/" + cashAccountId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/accounts/" + cashAccountId + "/archive")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/accounts/" + cashAccountId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                .andExpect(status().isForbidden());

        // 6. User A updates and archives account (archived account excluded from totalActiveBalance)
        UpdateAccountRequest validUpdate = UpdateAccountRequest.builder()
                .accountName("Ví Tiền Mặt Chính")
                .accountType(AccountType.CASH)
                .description("Đã cập nhật")
                .allowNegative(false)
                .status(AccountStatus.ACTIVE)
                .build();

        mockMvc.perform(put("/api/v1/accounts/" + cashAccountId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountName").value("Ví Tiền Mặt Chính"));

        mockMvc.perform(patch("/api/v1/accounts/" + cashAccountId + "/archive")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        mockMvc.perform(get("/api/v1/accounts/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalActiveBalance").value(8500000))
                .andExpect(jsonPath("$.data.activeAccountCount").value(1))
                .andExpect(jsonPath("$.data.archivedAccountCount").value(1));

        // 7. User A deletes account (soft delete -> subsequent GET returns 404)
        mockMvc.perform(delete("/api/v1/accounts/" + cashAccountId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/accounts/" + cashAccountId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isNotFound());
    }
}
