package com.pfm.personalfinancemanager.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfm.personalfinancemanager.dto.request.CreateAccountRequest;
import com.pfm.personalfinancemanager.dto.request.CreateBudgetRequest;
import com.pfm.personalfinancemanager.dto.request.CreateRecurringTransactionRequest;
import com.pfm.personalfinancemanager.dto.request.RegisterRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateRecurringTransactionRequest;
import com.pfm.personalfinancemanager.dto.response.RecurringExecutionBatchResult;
import com.pfm.personalfinancemanager.enums.AccountType;
import com.pfm.personalfinancemanager.enums.RecurringFrequency;
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
import com.pfm.personalfinancemanager.scheduler.RecurringTransactionScheduler;
import com.pfm.personalfinancemanager.service.RecurringExecutionProcessor;
import com.pfm.personalfinancemanager.service.RecurringExecutionProcessor.OccurrenceExecutionResult;
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
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
class RecurringTransactionControllerIntegrationTest {

    private static final String SALARY_CAT_ID = "00000000-0000-0000-0001-000000000001";
    private static final String FOOD_CAT_ID = "00000000-0000-0000-0002-000000000001";
    private static final String TRANSPORT_CAT_ID = "00000000-0000-0000-0002-000000000002";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RecurringTransactionScheduler scheduler;

    @Autowired
    private RecurringExecutionProcessor executionProcessor;

    @Autowired
    private Clock clock;

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

        tokenUserA = registerAndGetToken("Recurring Alice", "rec.alice@pfm.local");
        tokenUserB = registerAndGetToken("Recurring Bob", "rec.bob@pfm.local");

        cashAccountA = createAccount(tokenUserA, "Ví tiền mặt", AccountType.CASH, new BigDecimal("5000000"));
        bankAccountA = createAccount(tokenUserA, "Ngân hàng VCB", AccountType.BANK_ACCOUNT, new BigDecimal("1000000"));
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

    private String createRecurring(CreateRecurringTransactionRequest request) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/recurring-transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString())
                .get("data").get("id").asText();
    }

    @Test
    void recurringEngineAndApiLifecycle_ShouldExecuteCatchUpTriggerBudgetPreventDuplicatesAndEnforceIsolation()
            throws Exception {
        LocalDate today = LocalDate.now(clock);

        // 1. Validation checks: category mismatch, same account transfer, endDate < startDate
        mockMvc.perform(post("/api/v1/recurring-transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateRecurringTransactionRequest.builder()
                                .accountId(cashAccountA)
                                .categoryId(SALARY_CAT_ID)
                                .transactionType(TransactionType.EXPENSE)
                                .amount(new BigDecimal("100000"))
                                .description("Sai danh mục")
                                .frequency(RecurringFrequency.MONTHLY)
                                .startDate(today)
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_TYPE_MISMATCH"));

        mockMvc.perform(post("/api/v1/recurring-transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateRecurringTransactionRequest.builder()
                                .accountId(cashAccountA)
                                .toAccountId(cashAccountA)
                                .transactionType(TransactionType.TRANSFER)
                                .amount(new BigDecimal("100000"))
                                .description("Trùng tài khoản")
                                .frequency(RecurringFrequency.WEEKLY)
                                .startDate(today)
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SAME_ACCOUNT_TRANSFER"));

        // 2. Set up Food budget (1,000,000) for current UTC month to verify recurring EXPENSE integrates with budgets
        MvcResult budgetRes = mockMvc.perform(post("/api/v1/budgets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateBudgetRequest.builder()
                                .categoryId(FOOD_CAT_ID)
                                .month(today.getMonthValue())
                                .year(today.getYear())
                                .limitAmount(new BigDecimal("1000000"))
                                .build())))
                .andExpect(status().isCreated())
                .andReturn();
        String foodBudgetId = objectMapper.readTree(budgetRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        // 3. Create 3 due recurring rules:
        // (a) MONTHLY EXPENSE 900,000 on Food starting today -> should fire 1x & trigger BUDGET_WARNING (90%)
        String expenseRuleId = createRecurring(CreateRecurringTransactionRequest.builder()
                .accountId(cashAccountA)
                .categoryId(FOOD_CAT_ID)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("900000"))
                .description("Gói cơm văn phòng hàng tháng")
                .frequency(RecurringFrequency.MONTHLY)
                .startDate(today)
                .build());

        // (b) DAILY INCOME 10,000,000 starting yesterday -> should catch up 2x (yesterday + today = +20,000,000)
        createRecurring(CreateRecurringTransactionRequest.builder()
                .accountId(bankAccountA)
                .categoryId(SALARY_CAT_ID)
                .transactionType(TransactionType.INCOME)
                .amount(new BigDecimal("10000000"))
                .description("Thu nhập tự động hàng ngày")
                .frequency(RecurringFrequency.DAILY)
                .startDate(today.minusDays(1))
                .build());

        // (c) MONTHLY EXPENSE 50,000,000 (insufficient balance!) starting today -> isolated failure + 1 SYSTEM notification
        createRecurring(CreateRecurringTransactionRequest.builder()
                .accountId(cashAccountA)
                .categoryId(TRANSPORT_CAT_ID)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("50000000"))
                .description("Chi phí vượt số dư")
                .frequency(RecurringFrequency.MONTHLY)
                .startDate(today)
                .build());

        // 4. Run scheduled batch execution (Run #1)
        RecurringExecutionBatchResult batch1 = scheduler.executeScheduledRecurringTransactions();
        assertThat(batch1.getTotalRulesScanned()).isEqualTo(3);
        assertThat(batch1.getExecutedCount()).isEqualTo(3);
        assertThat(batch1.getFailedCount()).isEqualTo(1);

        // Verify balances: Cash 5M - 900k = 4.1M; Bank 1M + 20M = 21M
        mockMvc.perform(get("/api/v1/accounts/" + cashAccountA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentBalance").value(4100000));

        mockMvc.perform(get("/api/v1/accounts/" + bankAccountA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentBalance").value(21000000));

        // Verify Budget spent updated to 900,000 (WARNING 90%)
        mockMvc.perform(get("/api/v1/budgets/" + foodBudgetId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.spentAmount").value(900000))
                .andExpect(jsonPath("$.data.alertLevel").value("WARNING"));

        // Verify Notifications: 3 RECURRING_EXECUTED + 1 BUDGET_WARNING + 1 SYSTEM failure = 5
        mockMvc.perform(get("/api/v1/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(5));

        // 5. Duplicate Execution Prevention: Run #2 on the exact same date must execute 0 rules and emit 0 new notifications!
        RecurringExecutionBatchResult batch2 = scheduler.executeScheduledRecurringTransactions();
        assertThat(batch2.getExecutedCount()).isZero();
        assertThat(batch2.getFailedCount()).isEqualTo(1); // Failing rule remains pending & fails safely again

        // Failure alert deduplicated for today -> still 5 notifications total; balances untouched
        mockMvc.perform(get("/api/v1/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(5));

        mockMvc.perform(get("/api/v1/accounts/" + cashAccountA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentBalance").value(4100000));

        // Manual execute WITHOUT allowEarly=true on already-advanced rule -> rejected cleanly
        mockMvc.perform(post("/api/v1/recurring-transactions/" + expenseRuleId + "/execute")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isBadRequest());

        // 6. Future TRANSFER recurring rule -> Update -> Early Execute -> Concurrent race prevention
        LocalDate futureStart = today.plusDays(7);
        String transferRuleId = createRecurring(CreateRecurringTransactionRequest.builder()
                .accountId(bankAccountA)
                .toAccountId(cashAccountA)
                .transactionType(TransactionType.TRANSFER)
                .amount(new BigDecimal("500000"))
                .description("Chuyển tiền hàng tuần")
                .frequency(RecurringFrequency.WEEKLY)
                .startDate(futureStart)
                .build());

        mockMvc.perform(put("/api/v1/recurring-transactions/" + transferRuleId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateRecurringTransactionRequest.builder()
                                .accountId(bankAccountA)
                                .toAccountId(cashAccountA)
                                .transactionType(TransactionType.TRANSFER)
                                .amount(new BigDecimal("600000"))
                                .description("Chuyển tiền tuần (đã cập nhật)")
                                .frequency(RecurringFrequency.WEEKLY)
                                .startDate(futureStart)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(600000))
                .andExpect(jsonPath("$.data.nextExecutionDate").value(futureStart.toString()));

        // Execute manually with allowEarly=true -> moves Bank 21M -> 20.4M, Cash 4.1M -> 4.7M, next = today + 14 days
        mockMvc.perform(post("/api/v1/recurring-transactions/" + transferRuleId + "/execute")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .param("allowEarly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recurringTransactionId").value(transferRuleId))
                .andExpect(jsonPath("$.data.amount").value(600000));

        LocalDate nextWeeklyDate = futureStart.plusWeeks(1);
        // Simulate 2 cluster nodes racing simultaneously on nextWeeklyDate
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Boolean>> tasks = List.of(
                () -> attemptConcurrentOccurrence(transferRuleId, nextWeeklyDate, ready, start),
                () -> attemptConcurrentOccurrence(transferRuleId, nextWeeklyDate, ready, start)
        );
        List<Future<Boolean>> futures = new ArrayList<>();
        for (Callable<Boolean> task : tasks) {
            futures.add(pool.submit(task));
        }
        ready.await();
        start.countDown();
        int succeeded = 0;
        for (Future<Boolean> future : futures) {
            if (Boolean.TRUE.equals(future.get())) {
                succeeded++;
            }
        }
        pool.shutdown();
        assertThat(succeeded).isEqualTo(1);

        // Only ONE additional 600,000 transfer committed: Bank = 19.8M, Cash = 5.3M
        mockMvc.perform(get("/api/v1/accounts/" + bankAccountA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentBalance").value(19800000));

        mockMvc.perform(get("/api/v1/accounts/" + cashAccountA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentBalance").value(5300000));

        // 7. IDOR Protection (User B -> 403 Forbidden)
        mockMvc.perform(get("/api/v1/recurring-transactions/" + transferRuleId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/recurring-transactions/" + transferRuleId + "/pause")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/recurring-transactions/" + transferRuleId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                .andExpect(status().isForbidden());

        // 8. Pause -> Resume -> Cancel -> Delete (preserving generated transactions with recurringTransactionId cleared)
        mockMvc.perform(patch("/api/v1/recurring-transactions/" + transferRuleId + "/pause")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAUSED"));

        mockMvc.perform(patch("/api/v1/recurring-transactions/" + transferRuleId + "/resume")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(patch("/api/v1/recurring-transactions/" + transferRuleId + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(delete("/api/v1/recurring-transactions/" + transferRuleId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk());

        // Historical transactions remain in ledger (1 EXPENSE + 2 TRANSFER = 3 on Cash A), with recurringTransactionId nullified
        MvcResult cashTxRes = mockMvc.perform(get("/api/v1/accounts/" + cashAccountA + "/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andReturn();

        JsonNode content = objectMapper.readTree(cashTxRes.getResponse().getContentAsString())
                .get("data").get("content");
        boolean foundClearedTransfer = false;
        for (JsonNode txNode : content) {
            if ("TRANSFER".equals(txNode.get("transactionType").asText())) {
                assertThat(txNode.get("recurringTransactionId").isNull()).isTrue();
                foundClearedTransfer = true;
            }
        }
        assertThat(foundClearedTransfer).isTrue();
    }

    private boolean attemptConcurrentOccurrence(
            String recurringId,
            LocalDate date,
            CountDownLatch ready,
            CountDownLatch start
    ) throws Exception {
        ready.countDown();
        start.await();
        try {
            OccurrenceExecutionResult res = executionProcessor.executeSingleOccurrence(recurringId, date);
            return res.executed();
        } catch (RuntimeException ex) {
            return false;
        }
    }
}

