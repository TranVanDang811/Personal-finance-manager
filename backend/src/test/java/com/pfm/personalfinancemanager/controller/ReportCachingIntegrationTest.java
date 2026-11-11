package com.pfm.personalfinancemanager.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfm.personalfinancemanager.dto.request.CreateAccountRequest;
import com.pfm.personalfinancemanager.dto.request.CreateTransactionRequest;
import com.pfm.personalfinancemanager.dto.request.RegisterRequest;
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
import com.pfm.personalfinancemanager.service.AccountService;
import com.pfm.personalfinancemanager.service.ReportCacheService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.cache.type=simple")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportCachingIntegrationTest {

    private static final String SALARY_CAT_ID = "00000000-0000-0000-0001-000000000001";
    private static final String FOOD_CAT_ID = "00000000-0000-0000-0002-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReportCacheService reportCacheService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @SpyBean
    private TransactionRepository transactionRepository;

    @Autowired
    private SavingGoalContributionRepository savingGoalContributionRepository;

    @Autowired
    private SavingGoalRepository savingGoalRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private BudgetRepository budgetRepository;

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
    private String userAId;
    private String userBId;
    private String bankAccountA;

    @BeforeEach
    void setUp() throws Exception {
        cleanAll();

        tokenUserA = registerAndGetToken("Cache Alice", "cache.alice@pfm.local");
        tokenUserB = registerAndGetToken("Cache Bob", "cache.bob@pfm.local");
        userAId = userRepository.findByEmailIgnoreCase("cache.alice@pfm.local").orElseThrow().getId();
        userBId = userRepository.findByEmailIgnoreCase("cache.bob@pfm.local").orElseThrow().getId();

        bankAccountA = createAccount(tokenUserA, "Bank Alice", AccountType.BANK_ACCOUNT, new BigDecimal("5000000"));
        createAccount(tokenUserB, "Bank Bob", AccountType.BANK_ACCOUNT, new BigDecimal("5000000"));

        createTransaction(tokenUserA, bankAccountA, SALARY_CAT_ID, TransactionType.INCOME,
                "10000000", "Lương", "2026-11-01T08:00:00Z");
        createTransaction(tokenUserA, bankAccountA, FOOD_CAT_ID, TransactionType.EXPENSE,
                "2000000", "Ăn trưa", "2026-11-02T12:00:00Z");

        Mockito.clearInvocations(transactionRepository);
    }

    @AfterEach
    void tearDown() {
        cleanAll();
    }

    private void cleanAll() {
        cacheManager.getCacheNames().forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
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
            String categoryId,
            TransactionType type,
            String amount,
            String description,
            String isoDate
    ) throws Exception {
        CreateTransactionRequest req = CreateTransactionRequest.builder()
                .accountId(accountId)
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
    void cacheHitRollbackSafetyAndPerUserEviction_ShouldWorkEndToEnd() throws Exception {
        // 1. Cold read: computes & caches Dashboard for User A and establishes version for User B
        MvcResult firstRes = mockMvc.perform(get("/api/v1/reports/dashboard")
                        .param("month", "11")
                        .param("year", "2026")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode firstData = objectMapper.readTree(firstRes.getResponse().getContentAsString()).get("data");
        assertThat(firstData.get("monthlyExpense").decimalValue()).isEqualByComparingTo("2000000");
        assertThat(firstData.get("totalActiveAccountBalance").decimalValue()).isEqualByComparingTo("13000000");

        verify(transactionRepository, times(2)).sumAmountsByTypeForPeriod(eq(userAId), any(), any());

        String versionABefore = reportCacheService.getUserCacheVersion(userAId);
        String versionBBefore = reportCacheService.getUserCacheVersion(userBId);

        // 2. Warm read: must hit cache without touching TransactionRepository
        Mockito.clearInvocations(transactionRepository);
        mockMvc.perform(get("/api/v1/reports/dashboard")
                        .param("month", "11")
                        .param("year", "2026")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk());

        verifyNoInteractions(transactionRepository);

        // 3. Rolled-back @EvictUserReports operation must NOT rotate cache version (afterCommit safety)
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.executeWithoutResult(status -> {
            accountService.createAccount(userAId, CreateAccountRequest.builder()
                    .accountName("Ví tạm rollback")
                    .accountType(AccountType.CASH)
                    .initialBalance(new BigDecimal("1000000"))
                    .build());
            status.setRollbackOnly();
        });

        assertThat(reportCacheService.getUserCacheVersion(userAId)).isEqualTo(versionABefore);
        mockMvc.perform(get("/api/v1/reports/dashboard")
                        .param("month", "11")
                        .param("year", "2026")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk());
        verifyNoInteractions(transactionRepository);

        // 4. Committed write for User A rotates ONLY User A's version & forces fresh recalculation
        createTransaction(tokenUserA, bankAccountA, FOOD_CAT_ID, TransactionType.EXPENSE,
                "1000000", "Ăn tối", "2026-11-03T19:00:00Z");

        assertThat(reportCacheService.getUserCacheVersion(userAId)).isNotEqualTo(versionABefore);
        assertThat(reportCacheService.getUserCacheVersion(userBId)).isEqualTo(versionBBefore);

        Mockito.clearInvocations(transactionRepository);
        MvcResult afterWriteRes = mockMvc.perform(get("/api/v1/reports/dashboard")
                        .param("month", "11")
                        .param("year", "2026")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode updatedData = objectMapper.readTree(afterWriteRes.getResponse().getContentAsString()).get("data");
        assertThat(updatedData.get("monthlyExpense").decimalValue()).isEqualByComparingTo("3000000");
        assertThat(updatedData.get("totalActiveAccountBalance").decimalValue()).isEqualByComparingTo("12000000");
        verify(transactionRepository, atLeastOnce()).sumAmountsByTypeForPeriod(eq(userAId), any(), any());
    }
}

