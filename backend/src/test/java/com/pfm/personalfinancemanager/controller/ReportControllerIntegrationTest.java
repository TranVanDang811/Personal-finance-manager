package com.pfm.personalfinancemanager.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfm.personalfinancemanager.dto.request.CreateAccountRequest;
import com.pfm.personalfinancemanager.dto.request.CreateBudgetRequest;
import com.pfm.personalfinancemanager.dto.request.CreateSavingGoalRequest;
import com.pfm.personalfinancemanager.dto.request.CreateTransactionRequest;
import com.pfm.personalfinancemanager.dto.request.RegisterRequest;
import com.pfm.personalfinancemanager.dto.request.SavingGoalContributionRequest;
import com.pfm.personalfinancemanager.enums.AccountType;
import com.pfm.personalfinancemanager.enums.TransactionType;
import com.pfm.personalfinancemanager.repository.AccountRepository;
import com.pfm.personalfinancemanager.repository.BudgetRepository;
import com.pfm.personalfinancemanager.repository.NotificationRepository;
import com.pfm.personalfinancemanager.repository.PasswordResetTokenRepository;
import com.pfm.personalfinancemanager.repository.RecurringTransactionRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerIntegrationTest {

    private static final String SALARY_CAT_ID = "00000000-0000-0000-0001-000000000001";
    private static final String FOOD_CAT_ID = "00000000-0000-0000-0002-000000000001";
    private static final String TRANSPORT_CAT_ID = "00000000-0000-0000-0002-000000000002";

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
    private RecurringTransactionRepository recurringTransactionRepository;

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

        tokenUserA = registerAndGetToken("Report Alice", "report.alice@pfm.local");
        tokenUserB = registerAndGetToken("Report Bob", "report.bob@pfm.local");

        cashAccountA = createAccount(tokenUserA, "Ví tiền mặt", AccountType.CASH, new BigDecimal("5000000"));
        bankAccountA = createAccount(tokenUserA, "Ngân hàng VCB", AccountType.BANK_ACCOUNT, new BigDecimal("15000000"));
        String archivedAccountA = createAccount(tokenUserA, "Ví cũ", AccountType.CASH, new BigDecimal("3000000"));
        mockMvc.perform(patch("/api/v1/accounts/" + archivedAccountA + "/archive")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk());

        // 10/2026: Income 10M, Expense 2M
        createTransaction(tokenUserA, bankAccountA, null, SALARY_CAT_ID, TransactionType.INCOME,
                "10000000", "Lương tháng 10", "2026-10-05T08:00:00Z");
        createTransaction(tokenUserA, cashAccountA, null, FOOD_CAT_ID, TransactionType.EXPENSE,
                "2000000", "Ăn uống tháng 10", "2026-10-12T12:00:00Z");

        // 11/2026: Income 15M, Expense (Food 3M + Transport 1M), Transfer 1M
        createTransaction(tokenUserA, bankAccountA, null, SALARY_CAT_ID, TransactionType.INCOME,
                "15000000", "Lương tháng 11", "2026-11-01T09:00:00Z");
        createTransaction(tokenUserA, bankAccountA, null, FOOD_CAT_ID, TransactionType.EXPENSE,
                "3000000", "Siêu thị tháng 11", "2026-11-03T12:00:00Z");
        createTransaction(tokenUserA, cashAccountA, null, TRANSPORT_CAT_ID, TransactionType.EXPENSE,
                "1000000", "Xăng xe tháng 11", "2026-11-03T18:00:00Z");
        createTransaction(tokenUserA, bankAccountA, cashAccountA, null, TransactionType.TRANSFER,
                "1000000", "Rút tiền mặt", "2026-11-05T10:00:00Z");

        // Budget in 11/2026 for Food (3M / 3.5M = 85.71% -> warningCount = 1)
        mockMvc.perform(post("/api/v1/budgets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateBudgetRequest.builder()
                                .categoryId(FOOD_CAT_ID)
                                .month(11)
                                .year(2026)
                                .limitAmount(new BigDecimal("3500000"))
                                .build())))
                .andExpect(status().isCreated());

        // Saving goal: target 10M, deposit 4M from Bank A -> active accounts = 35M, goals = 4M, netWorth = 39M
        MvcResult goalRes = mockMvc.perform(post("/api/v1/saving-goals")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateSavingGoalRequest.builder()
                                .name("Quỹ Dự Phòng")
                                .targetAmount(new BigDecimal("10000000"))
                                .startDate(LocalDate.of(2026, 1, 1))
                                .targetDate(LocalDate.of(2026, 12, 31))
                                .build())))
                .andExpect(status().isCreated())
                .andReturn();
        String goalId = objectMapper.readTree(goalRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        mockMvc.perform(post("/api/v1/saving-goals/" + goalId + "/deposit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SavingGoalContributionRequest.builder()
                                .accountId(bankAccountA)
                                .amount(new BigDecimal("4000000"))
                                .note("Trích quỹ tháng 11")
                                .contributionDate(Instant.parse("2026-11-06T10:00:00Z"))
                                .build())))
                .andExpect(status().isOk());

        // User B data (must never leak into User A reports)
        String cashAccountB = createAccount(tokenUserB, "Ví Bob", AccountType.CASH, new BigDecimal("50000000"));
        createTransaction(tokenUserB, cashAccountB, null, FOOD_CAT_ID, TransactionType.EXPENSE,
                "9999000", "Chi tiêu của Bob", "2026-11-04T10:00:00Z");
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
        recurringTransactionRepository.deleteAll();
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
            String amount,
            String description,
            String isoDate
    ) throws Exception {
        CreateTransactionRequest req = CreateTransactionRequest.builder()
                .accountId(accountId)
                .toAccountId(toAccountId)
                .categoryId(categoryId)
                .transactionType(type)
                .amount(new BigDecimal(amount))
                .description(description)
                .transactionDate(Instant.parse(isoDate))
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void allReportEndpoints_ShouldReturnAccurateAggregationsIsolateUsersAndValidateParams() throws Exception {
        // 1. Dashboard Summary (11/2026)
        MvcResult dashRes = mockMvc.perform(get("/api/v1/reports/dashboard")
                        .param("month", "11")
                        .param("year", "2026")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.month").value(11))
                .andExpect(jsonPath("$.data.year").value(2026))
                .andExpect(jsonPath("$.data.budgetOverview.totalBudgets").value(1))
                .andExpect(jsonPath("$.data.budgetOverview.warningCount").value(1))
                .andExpect(jsonPath("$.data.savingGoalOverview.inProgressCount").value(1))
                .andExpect(jsonPath("$.data.topExpenseCategories.length()").value(2))
                .andExpect(jsonPath("$.data.recentTransactions.length()").value(5))
                .andReturn();

        JsonNode dash = objectMapper.readTree(dashRes.getResponse().getContentAsString()).get("data");
        assertThat(dash.get("totalActiveAccountBalance").decimalValue()).isEqualByComparingTo("35000000");
        assertThat(dash.get("totalSavedInGoals").decimalValue()).isEqualByComparingTo("4000000");
        assertThat(dash.get("netWorth").decimalValue()).isEqualByComparingTo("39000000");
        assertThat(dash.get("monthlyIncome").decimalValue()).isEqualByComparingTo("15000000");
        assertThat(dash.get("monthlyExpense").decimalValue()).isEqualByComparingTo("4000000");
        assertThat(dash.get("netCashflow").decimalValue()).isEqualByComparingTo("11000000");
        assertThat(dash.get("savingRatePercentage").decimalValue()).isEqualByComparingTo("73.33");
        assertThat(dash.get("previousMonthIncome").decimalValue()).isEqualByComparingTo("10000000");
        assertThat(dash.get("previousMonthExpense").decimalValue()).isEqualByComparingTo("2000000");
        assertThat(dash.get("incomeChangePercentage").decimalValue()).isEqualByComparingTo("50.00");
        assertThat(dash.get("expenseChangePercentage").decimalValue()).isEqualByComparingTo("100.00");

        // 2. Category Breakdown (EXPENSE in 11/2026 -> Food 75.00%, Transport 25.00%)
        MvcResult breakdownRes = mockMvc.perform(get("/api/v1/reports/category-breakdown")
                        .param("type", "EXPENSE")
                        .param("month", "11")
                        .param("year", "2026")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionType").value("EXPENSE"))
                .andExpect(jsonPath("$.data.totalTransactionCount").value(2))
                .andExpect(jsonPath("$.data.categories.length()").value(2))
                .andReturn();

        JsonNode breakdown = objectMapper.readTree(breakdownRes.getResponse().getContentAsString()).get("data");
        assertThat(breakdown.get("totalAmount").decimalValue()).isEqualByComparingTo("4000000");
        assertThat(breakdown.get("categories").get(0).get("amount").decimalValue()).isEqualByComparingTo("3000000");
        assertThat(breakdown.get("categories").get(0).get("percentage").decimalValue()).isEqualByComparingTo("75.00");
        assertThat(breakdown.get("categories").get(1).get("amount").decimalValue()).isEqualByComparingTo("1000000");
        assertThat(breakdown.get("categories").get(1).get("percentage").decimalValue()).isEqualByComparingTo("25.00");

        // 3. Cashflow Trend (3 months ending 11/2026 -> 09/2026 zero-filled, 10/2026, 11/2026)
        MvcResult trendRes = mockMvc.perform(get("/api/v1/reports/cashflow-trend")
                        .param("months", "3")
                        .param("month", "11")
                        .param("year", "2026")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.monthsCount").value(3))
                .andExpect(jsonPath("$.data.startPeriod").value("2026-09"))
                .andExpect(jsonPath("$.data.endPeriod").value("2026-11"))
                .andExpect(jsonPath("$.data.points.length()").value(3))
                .andExpect(jsonPath("$.data.points[0].label").value("09/2026"))
                .andExpect(jsonPath("$.data.points[1].label").value("10/2026"))
                .andExpect(jsonPath("$.data.points[2].label").value("11/2026"))
                .andReturn();

        JsonNode trend = objectMapper.readTree(trendRes.getResponse().getContentAsString()).get("data");
        assertThat(trend.get("totalIncome").decimalValue()).isEqualByComparingTo("25000000");
        assertThat(trend.get("totalExpense").decimalValue()).isEqualByComparingTo("6000000");
        assertThat(trend.get("totalNetSavings").decimalValue()).isEqualByComparingTo("19000000");
        assertThat(trend.get("points").get(0).get("income").decimalValue()).isEqualByComparingTo("0");
        assertThat(trend.get("points").get(1).get("income").decimalValue()).isEqualByComparingTo("10000000");
        assertThat(trend.get("points").get(2).get("expense").decimalValue()).isEqualByComparingTo("4000000");

        // 4. Period Summary (2026-11-01 to 2026-11-06: includes 1M account transfer + 4M saving goal deposit = 5M)
        MvcResult periodRes = mockMvc.perform(get("/api/v1/reports/period-summary")
                        .param("startDate", "2026-11-01")
                        .param("endDate", "2026-11-06")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dailySeries.length()").value(6))
                .andReturn();

        JsonNode period = objectMapper.readTree(periodRes.getResponse().getContentAsString()).get("data");
        assertThat(period.get("totalIncome").decimalValue()).isEqualByComparingTo("15000000");
        assertThat(period.get("totalExpense").decimalValue()).isEqualByComparingTo("4000000");
        assertThat(period.get("totalTransfer").decimalValue()).isEqualByComparingTo("5000000");
        assertThat(period.get("netCashflow").decimalValue()).isEqualByComparingTo("11000000");
        assertThat(period.get("dailySeries").get(2).get("date").asText()).isEqualTo("2026-11-03");
        assertThat(period.get("dailySeries").get(2).get("expense").decimalValue()).isEqualByComparingTo("4000000");

        // 5. Validation errors -> 400 Bad Request
        mockMvc.perform(get("/api/v1/reports/category-breakdown")
                        .param("type", "TRANSFER")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/reports/cashflow-trend")
                        .param("months", "25")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/reports/period-summary")
                        .param("startDate", "2026-11-10")
                        .param("endDate", "2026-11-01")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/reports/period-summary")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2026-01-02")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/reports/dashboard")
                        .param("month", "13")
                        .param("year", "2026")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isBadRequest());

        // 6. User B isolation verification
        MvcResult bobDashRes = mockMvc.perform(get("/api/v1/reports/dashboard")
                        .param("month", "11")
                        .param("year", "2026")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode bobDash = objectMapper.readTree(bobDashRes.getResponse().getContentAsString()).get("data");
        assertThat(bobDash.get("monthlyExpense").decimalValue()).isEqualByComparingTo("9999000");
        assertThat(bobDash.get("monthlyIncome").decimalValue()).isEqualByComparingTo("0");
    }
}

