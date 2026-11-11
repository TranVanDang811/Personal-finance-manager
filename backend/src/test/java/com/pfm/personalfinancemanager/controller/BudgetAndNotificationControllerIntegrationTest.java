package com.pfm.personalfinancemanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfm.personalfinancemanager.dto.request.CreateAccountRequest;
import com.pfm.personalfinancemanager.dto.request.CreateBudgetRequest;
import com.pfm.personalfinancemanager.dto.request.CreateTransactionRequest;
import com.pfm.personalfinancemanager.dto.request.RegisterRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateBudgetRequest;
import com.pfm.personalfinancemanager.enums.AccountType;
import com.pfm.personalfinancemanager.enums.TransactionType;
import com.pfm.personalfinancemanager.repository.AccountRepository;
import com.pfm.personalfinancemanager.repository.BudgetRepository;
import com.pfm.personalfinancemanager.repository.NotificationRepository;
import com.pfm.personalfinancemanager.repository.PasswordResetTokenRepository;
import com.pfm.personalfinancemanager.repository.RefreshTokenRepository;
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
class BudgetAndNotificationControllerIntegrationTest {

    private static final String SALARY_CAT_ID = "00000000-0000-0000-0001-000000000001";
    private static final String FOOD_CAT_ID = "00000000-0000-0000-0002-000000000001";
    private static final String TRANSPORT_CAT_ID = "00000000-0000-0000-0002-000000000002";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

        tokenUserA = registerAndGetToken("Budget Alice", "budget.alice@pfm.local");
        tokenUserB = registerAndGetToken("Budget Bob", "budget.bob@pfm.local");

        cashAccountA = createAccount(tokenUserA, "Tiền mặt", AccountType.CASH, new BigDecimal("5000000"));
        bankAccountA = createAccount(tokenUserA, "Ngân hàng", AccountType.BANK_ACCOUNT, new BigDecimal("5000000"));
    }

    @AfterEach
    void tearDown() {
        cleanAll();
    }

    private void cleanAll() {
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

    private void createTransaction(
            String token,
            String accountId,
            String toAccountId,
            String categoryId,
            TransactionType type,
            BigDecimal amount,
            Instant date
    ) throws Exception {
        CreateTransactionRequest req = CreateTransactionRequest.builder()
                .accountId(accountId)
                .toAccountId(toAccountId)
                .categoryId(categoryId)
                .transactionType(type)
                .amount(amount)
                .description("Test " + type)
                .transactionDate(date)
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void budgetLifecycleAndAlertThresholds_ShouldTrigger80_100_Exceeded_ExcludeTransfersAndEnforceIsolation()
            throws Exception {
        // 1. Reject INCOME category for budget
        CreateBudgetRequest incomeBudgetReq = CreateBudgetRequest.builder()
                .categoryId(SALARY_CAT_ID)
                .month(11)
                .year(2026)
                .limitAmount(new BigDecimal("1000000"))
                .build();

        mockMvc.perform(post("/api/v1/budgets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomeBudgetReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_TYPE_MISMATCH"));

        // 2. Create Food & Drink budget (1,000,000) and Transport budget (500,000) for Nov 2026
        MvcResult foodBudgetRes = mockMvc.perform(post("/api/v1/budgets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateBudgetRequest.builder()
                                .categoryId(FOOD_CAT_ID)
                                .month(11)
                                .year(2026)
                                .limitAmount(new BigDecimal("1000000"))
                                .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.spentAmount").value(0))
                .andExpect(jsonPath("$.data.alertLevel").value("NORMAL"))
                .andReturn();

        String foodBudgetId = objectMapper.readTree(foodBudgetRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        // Duplicate budget in same category/period -> 409 CONFLICT
        mockMvc.perform(post("/api/v1/budgets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateBudgetRequest.builder()
                                .categoryId(FOOD_CAT_ID)
                                .month(11)
                                .year(2026)
                                .limitAmount(new BigDecimal("2000000"))
                                .build())))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/budgets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateBudgetRequest.builder()
                                .categoryId(TRANSPORT_CAT_ID)
                                .month(11)
                                .year(2026)
                                .limitAmount(new BigDecimal("500000"))
                                .build())))
                .andExpect(status().isCreated());

        Instant novDate = Instant.parse("2026-11-15T10:00:00Z");

        // 3. TRANSFER must NOT count toward budget spending
        createTransaction(tokenUserA, cashAccountA, bankAccountA, null,
                TransactionType.TRANSFER, new BigDecimal("2000000"), novDate);

        // Expense in different month (October 2026) must NOT affect November budget
        createTransaction(tokenUserA, cashAccountA, null, FOOD_CAT_ID,
                TransactionType.EXPENSE, new BigDecimal("900000"), Instant.parse("2026-10-25T10:00:00Z"));

        mockMvc.perform(get("/api/v1/budgets/" + foodBudgetId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.spentAmount").value(0))
                .andExpect(jsonPath("$.data.alertLevel").value("NORMAL"));

        // 4. Spend 700,000 (70%) -> NORMAL, 0 alerts
        createTransaction(tokenUserA, cashAccountA, null, FOOD_CAT_ID,
                TransactionType.EXPENSE, new BigDecimal("700000"), novDate);

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));

        // 5. Spend +100,000 -> total 800,000 (80.00%) -> WARNING + 1 notification
        createTransaction(tokenUserA, cashAccountA, null, FOOD_CAT_ID,
                TransactionType.EXPENSE, new BigDecimal("100000"), novDate);

        mockMvc.perform(get("/api/v1/budgets/" + foodBudgetId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(jsonPath("$.data.usagePercentage").value(80.00))
                .andExpect(jsonPath("$.data.alertLevel").value("WARNING"));

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        // Spend +50,000 -> 85% -> must NOT duplicate 80% warning notification
        createTransaction(tokenUserA, cashAccountA, null, FOOD_CAT_ID,
                TransactionType.EXPENSE, new BigDecimal("50000"), novDate);

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        // 6. Spend +150,000 -> total 1,000,000 (100.00%) -> REACHED + 2nd notification
        createTransaction(tokenUserA, cashAccountA, null, FOOD_CAT_ID,
                TransactionType.EXPENSE, new BigDecimal("150000"), novDate);

        mockMvc.perform(get("/api/v1/budgets/" + foodBudgetId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(jsonPath("$.data.alertLevel").value("REACHED"))
                .andExpect(jsonPath("$.data.remainingAmount").value(0));

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(jsonPath("$.data.unreadCount").value(2));

        // 7. Spend +200,000 -> total 1,200,000 (120.00%) -> EXCEEDED + 3rd notification
        createTransaction(tokenUserA, cashAccountA, null, FOOD_CAT_ID,
                TransactionType.EXPENSE, new BigDecimal("200000"), novDate);

        mockMvc.perform(get("/api/v1/budgets/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .param("month", "11")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalLimitAmount").value(1500000))
                .andExpect(jsonPath("$.data.totalSpentAmount").value(1200000))
                .andExpect(jsonPath("$.data.totalRemainingAmount").value(300000))
                .andExpect(jsonPath("$.data.overallUsagePercentage").value(80.00))
                .andExpect(jsonPath("$.data.exceededCount").value(1));

        MvcResult notifListRes = mockMvc.perform(get("/api/v1/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .param("isRead", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andReturn();

        String firstNotifId = objectMapper.readTree(notifListRes.getResponse().getContentAsString())
                .get("data").get("content").get(0).get("id").asText();

        // 8. IDOR check: User B cannot access User A's budget or notification
        mockMvc.perform(get("/api/v1/budgets/" + foodBudgetId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/notifications/" + firstNotifId + "/read")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                .andExpect(status().isForbidden());

        // 9. Mark single read, read-all, and delete notification
        mockMvc.perform(patch("/api/v1/notifications/" + firstNotifId + "/read")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isRead").value(true));

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(jsonPath("$.data.unreadCount").value(2));

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(jsonPath("$.data.unreadCount").value(0));

        mockMvc.perform(delete("/api/v1/notifications/" + firstNotifId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk());

        // 10. Update budget limit from 1,000,000 -> 1,500,000 and delete budget
        mockMvc.perform(put("/api/v1/budgets/" + foodBudgetId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateBudgetRequest.builder()
                                .limitAmount(new BigDecimal("1500000"))
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.usagePercentage").value(80.00))
                .andExpect(jsonPath("$.data.alertLevel").value("WARNING"));

        mockMvc.perform(delete("/api/v1/budgets/" + foodBudgetId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk());
    }
}

