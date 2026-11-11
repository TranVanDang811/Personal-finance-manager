package com.pfm.personalfinancemanager.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfm.personalfinancemanager.dto.request.CreateAccountRequest;
import com.pfm.personalfinancemanager.dto.request.CreateBudgetRequest;
import com.pfm.personalfinancemanager.dto.request.CreateSavingGoalRequest;
import com.pfm.personalfinancemanager.dto.request.RegisterRequest;
import com.pfm.personalfinancemanager.dto.request.SavingGoalContributionRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateSavingGoalRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateTransactionRequest;
import com.pfm.personalfinancemanager.enums.AccountType;
import com.pfm.personalfinancemanager.enums.TransactionType;
import com.pfm.personalfinancemanager.repository.AccountRepository;
import com.pfm.personalfinancemanager.repository.BudgetRepository;
import com.pfm.personalfinancemanager.repository.NotificationRepository;
import com.pfm.personalfinancemanager.repository.PasswordResetTokenRepository;
import com.pfm.personalfinancemanager.repository.RefreshTokenRepository;
import com.pfm.personalfinancemanager.repository.SavingGoalContributionRepository;
import com.pfm.personalfinancemanager.repository.SavingGoalRepository;
import com.pfm.personalfinancemanager.repository.TransactionRepository;
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
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
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
class SavingGoalControllerIntegrationTest {

    private static final String FOOD_CAT_ID = "00000000-0000-0000-0002-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SavingGoalContributionRepository savingGoalContributionRepository;

    @Autowired
    private SavingGoalRepository savingGoalRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private TransactionRepository transactionRepository;

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
    private String cashAccountA;
    private String bankAccountA;

    @BeforeEach
    void setUp() throws Exception {
        cleanAll();

        tokenUserA = registerAndGetToken("Saving Alice", "saving.alice@pfm.local");
        tokenUserB = registerAndGetToken("Saving Bob", "saving.bob@pfm.local");

        cashAccountA = createAccount(tokenUserA, "Ví tiền mặt", AccountType.CASH, new BigDecimal("10000000"));
        bankAccountA = createAccount(tokenUserA, "Ngân hàng VCB", AccountType.BANK_ACCOUNT, new BigDecimal("10000000"));
    }

    @AfterEach
    void tearDown() {
        cleanAll();
    }

    private void cleanAll() {
        savingGoalContributionRepository.deleteAll();
        savingGoalRepository.deleteAll();
        notificationRepository.deleteAll();
        budgetRepository.deleteAll();
        transactionRepository.deleteAll();
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

        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(res.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
    }

    private String createAccount(String token, String name, AccountType type, BigDecimal balance) throws Exception {
        CreateAccountRequest req = CreateAccountRequest.builder()
                .accountName(name)
                .accountType(type)
                .initialBalance(balance)
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(res.getResponse().getContentAsString())
                .get("data").get("id").asText();
    }

    @Test
    void savingGoalFullLifecycle_DepositWithdrawCancelDeleteAndIsolation_ShouldWorkEndToEnd() throws Exception {
        // 1. Create an EXPENSE budget for Nov 2026 to verify SavingGoal transfers do NOT count as expense spending
        MvcResult budgetRes = mockMvc.perform(post("/api/v1/budgets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateBudgetRequest.builder()
                                .categoryId(FOOD_CAT_ID)
                                .month(11)
                                .year(2026)
                                .limitAmount(new BigDecimal("1000000"))
                                .build())))
                .andExpect(status().isCreated())
                .andReturn();
        String foodBudgetId = objectMapper.readTree(budgetRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        // 2. Create saving goal (target 10,000,000, default color #10B981)
        CreateSavingGoalRequest createGoalReq = CreateSavingGoalRequest.builder()
                .name("Quỹ mua Laptop")
                .targetAmount(new BigDecimal("10000000"))
                .startDate(LocalDate.of(2026, 1, 1))
                .targetDate(LocalDate.of(2026, 12, 31))
                .description("Tiết kiệm nâng cấp máy làm việc")
                .build();

        MvcResult createGoalRes = mockMvc.perform(post("/api/v1/saving-goals")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createGoalReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Quỹ mua Laptop"))
                .andExpect(jsonPath("$.data.currentAmount").value(0))
                .andExpect(jsonPath("$.data.remainingAmount").value(10000000))
                .andExpect(jsonPath("$.data.progressPercentage").value(0.00))
                .andExpect(jsonPath("$.data.color").value("#10B981"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andReturn();

        String goalId = objectMapper.readTree(createGoalRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        // List & status filter check
        mockMvc.perform(get("/api/v1/saving-goals")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        // 3. First deposit 4,000,000 from Cash account in Nov 2026
        SavingGoalContributionRequest deposit1 = SavingGoalContributionRequest.builder()
                .accountId(cashAccountA)
                .amount(new BigDecimal("4000000"))
                .note("Tiết kiệm tháng 11")
                .contributionDate(Instant.parse("2026-11-05T10:00:00Z"))
                .build();

        mockMvc.perform(post("/api/v1/saving-goals/" + goalId + "/deposit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deposit1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentAmount").value(4000000))
                .andExpect(jsonPath("$.data.remainingAmount").value(6000000))
                .andExpect(jsonPath("$.data.progressPercentage").value(40.00))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        // Verify Cash account balance debited (10,000,000 -> 6,000,000)
        mockMvc.perform(get("/api/v1/accounts/" + cashAccountA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentBalance").value(6000000));

        // Verify Expense budget spentAmount remains 0
        mockMvc.perform(get("/api/v1/budgets/" + foodBudgetId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.spentAmount").value(0));

        // Verify linked TRANSFER transaction appears in Cash account history & is protected against manual edit/delete
        MvcResult accountTxRes = mockMvc.perform(get("/api/v1/accounts/" + cashAccountA + "/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].transactionType").value("TRANSFER"))
                .andReturn();

        String linkedTxId = objectMapper.readTree(accountTxRes.getResponse().getContentAsString())
                .get("data").get("content").get(0).get("id").asText();

        UpdateTransactionRequest hijackUpdateReq = UpdateTransactionRequest.builder()
                .accountId(cashAccountA)
                .toAccountId(bankAccountA)
                .transactionType(TransactionType.TRANSFER)
                .amount(new BigDecimal("100000"))
                .description("Cố tình sửa giao dịch mục tiêu")
                .transactionDate(Instant.parse("2026-11-05T10:00:00Z"))
                .build();

        mockMvc.perform(put("/api/v1/transactions/" + linkedTxId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hijackUpdateReq)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/v1/transactions/" + linkedTxId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isBadRequest());

        // 4. Deposit exceeding account balance -> 422 UNPROCESSABLE ENTITY
        mockMvc.perform(post("/api/v1/saving-goals/" + goalId + "/deposit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SavingGoalContributionRequest.builder()
                                .accountId(cashAccountA)
                                .amount(new BigDecimal("99000000"))
                                .build())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_BALANCE"));

        // 5. Second deposit 6,000,000 from Bank account -> reaches 100% COMPLETED + SAVING_GOAL_REACHED notification
        mockMvc.perform(post("/api/v1/saving-goals/" + goalId + "/deposit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SavingGoalContributionRequest.builder()
                                .accountId(bankAccountA)
                                .amount(new BigDecimal("6000000"))
                                .note("Hoàn thành mục tiêu")
                                .contributionDate(Instant.parse("2026-11-08T10:00:00Z"))
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentAmount").value(10000000))
                .andExpect(jsonPath("$.data.remainingAmount").value(0))
                .andExpect(jsonPath("$.data.progressPercentage").value(100.00))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(get("/api/v1/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .param("isRead", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].notificationType").value("SAVING_GOAL_REACHED"))
                .andExpect(jsonPath("$.data.content[0].referenceId").value(goalId));

        // 6. Partial withdraw 2,500,000 back to Cash account -> refunds Cash (6M -> 8.5M) & reverts COMPLETED -> IN_PROGRESS
        mockMvc.perform(post("/api/v1/saving-goals/" + goalId + "/withdraw")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SavingGoalContributionRequest.builder()
                                .accountId(cashAccountA)
                                .amount(new BigDecimal("2500000"))
                                .note("Cần tiền mặt gấp")
                                .contributionDate(Instant.parse("2026-11-10T10:00:00Z"))
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentAmount").value(7500000))
                .andExpect(jsonPath("$.data.remainingAmount").value(2500000))
                .andExpect(jsonPath("$.data.progressPercentage").value(75.00))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        mockMvc.perform(get("/api/v1/accounts/" + cashAccountA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentBalance").value(8500000));

        // 7. Contribution history pagination (page=0, size=2 -> total 3 items sorted DESC by contributionDate)
        MvcResult contribRes = mockMvc.perform(get("/api/v1/saving-goals/" + goalId + "/contributions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].contributionType").value("WITHDRAW"))
                .andExpect(jsonPath("$.data.content[1].contributionType").value("DEPOSIT"))
                .andReturn();

        JsonNode firstContrib = objectMapper.readTree(contribRes.getResponse().getContentAsString())
                .get("data").get("content").get(0);
        assertThat(firstContrib.get("transactionId").asText()).isNotBlank();

        // 8. Update saving goal metadata
        mockMvc.perform(put("/api/v1/saving-goals/" + goalId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateSavingGoalRequest.builder()
                                .name("Quỹ mua MacBook Pro")
                                .targetAmount(new BigDecimal("15000000"))
                                .startDate(LocalDate.of(2026, 1, 1))
                                .targetDate(LocalDate.of(2026, 12, 31))
                                .color("#3B82F6")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Quỹ mua MacBook Pro"))
                .andExpect(jsonPath("$.data.progressPercentage").value(50.00))
                .andExpect(jsonPath("$.data.color").value("#3B82F6"));

        // 9. IDOR check: User B cannot view, update, deposit, withdraw, cancel, delete, or read contributions
        mockMvc.perform(get("/api/v1/saving-goals/" + goalId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/saving-goals/" + goalId + "/withdraw")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SavingGoalContributionRequest.builder()
                                .accountId(cashAccountA)
                                .amount(new BigDecimal("100000"))
                                .build())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/saving-goals/" + goalId + "/contributions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/saving-goals/" + goalId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                .andExpect(status().isForbidden());

        // 10. Delete blocked while currentAmount > 0 -> cancel -> deposit blocked -> withdraw remaining -> delete succeeds
        mockMvc.perform(delete("/api/v1/saving-goals/" + goalId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/v1/saving-goals/" + goalId + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(post("/api/v1/saving-goals/" + goalId + "/deposit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SavingGoalContributionRequest.builder()
                                .accountId(cashAccountA)
                                .amount(new BigDecimal("100000"))
                                .build())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/saving-goals/" + goalId + "/withdraw")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SavingGoalContributionRequest.builder()
                                .accountId(bankAccountA)
                                .amount(new BigDecimal("7500000"))
                                .note("Tất toán mục tiêu đã hủy")
                                .contributionDate(Instant.parse("2026-11-11T09:00:00Z"))
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentAmount").value(0))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // Bank balance restored: 10M - 6M + 7.5M = 11.5M
        mockMvc.perform(get("/api/v1/accounts/" + bankAccountA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentBalance").value(11500000));

        // Now delete succeeds cleanly even with historical contribution records
        mockMvc.perform(delete("/api/v1/saving-goals/" + goalId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/saving-goals/" + goalId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isNotFound());
    }
}

