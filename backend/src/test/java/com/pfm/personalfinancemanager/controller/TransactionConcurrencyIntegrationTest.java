package com.pfm.personalfinancemanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfm.personalfinancemanager.dto.request.CreateAccountRequest;
import com.pfm.personalfinancemanager.dto.request.CreateTransactionRequest;
import com.pfm.personalfinancemanager.dto.request.RegisterRequest;
import com.pfm.personalfinancemanager.enums.AccountType;
import com.pfm.personalfinancemanager.enums.TransactionType;
import com.pfm.personalfinancemanager.exception.InsufficientBalanceException;
import com.pfm.personalfinancemanager.repository.AccountRepository;
import com.pfm.personalfinancemanager.repository.PasswordResetTokenRepository;
import com.pfm.personalfinancemanager.repository.RefreshTokenRepository;
import com.pfm.personalfinancemanager.repository.TransactionRepository;
import com.pfm.personalfinancemanager.repository.UserRepository;
import com.pfm.personalfinancemanager.service.TransactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionConcurrencyIntegrationTest {

    private static final String FOOD_CAT_ID = "00000000-0000-0000-0002-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionService transactionService;

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

    private String userId;
    private String cashAccountId;
    private String bankAccountId;

    @BeforeEach
    void setUp() throws Exception {
        cleanAll();

        RegisterRequest reg = RegisterRequest.builder()
                .fullName("Concurrent Tester")
                .email("concurrent@pfm.local")
                .password("StrongPass@2026")
                .build();

        MvcResult regRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        var regJson = objectMapper.readTree(regRes.getResponse().getContentAsString()).get("data");
        String accessToken = regJson.get("accessToken").asText();
        userId = regJson.get("user").get("id").asText();

        cashAccountId = createAccount(accessToken, "Cash Concurrent", AccountType.CASH, new BigDecimal("1000000.0000"));
        bankAccountId = createAccount(accessToken, "Bank Concurrent", AccountType.BANK_ACCOUNT, new BigDecimal("1000000.0000"));
    }

    @AfterEach
    void tearDown() {
        cleanAll();
    }

    private void cleanAll() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String createAccount(String token, String name, AccountType type, BigDecimal balance) throws Exception {
        CreateAccountRequest req = CreateAccountRequest.builder()
                .accountName(name)
                .accountType(type)
                .initialBalance(balance)
                .allowNegative(false)
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();
    }

    @Test
    void concurrentExpenses_ShouldMaintainExactBalanceAndPreventNegativeOverdraft() throws Exception {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger insufficientCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();
                try {
                    CreateTransactionRequest req = CreateTransactionRequest.builder()
                            .accountId(cashAccountId)
                            .categoryId(FOOD_CAT_ID)
                            .transactionType(TransactionType.EXPENSE)
                            .amount(new BigDecimal("150000.0000"))
                            .description("Concurrent spend #" + idx)
                            .transactionDate(Instant.now())
                            .build();
                    transactionService.createTransaction(userId, req, null);
                    successCount.incrementAndGet();
                } catch (InsufficientBalanceException ex) {
                    insufficientCount.incrementAndGet();
                }
                return null;
            }));
        }

        assertThat(readyLatch.await(10, TimeUnit.SECONDS)).isTrue();
        startLatch.countDown();

        for (Future<?> f : futures) {
            f.get(15, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // 1,000,000 / 150,000 = 6 max successes, leaving exactly 100,000.0000, 4 rejected
        assertThat(successCount.get()).isEqualTo(6);
        assertThat(insufficientCount.get()).isEqualTo(4);

        BigDecimal remainingBalance = accountRepository.findById(cashAccountId).orElseThrow().getCurrentBalance();
        assertThat(remainingBalance).isEqualByComparingTo("100000.0000");
    }

    @Test
    void bidirectionalConcurrentTransfers_ShouldCompleteWithoutDeadlockAndConserveTotalMoney() throws Exception {
        int pairs = 6;
        int totalThreads = pairs * 2;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch readyLatch = new CountDownLatch(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < pairs; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();
                transactionService.createTransaction(userId, CreateTransactionRequest.builder()
                        .accountId(cashAccountId)
                        .toAccountId(bankAccountId)
                        .transactionType(TransactionType.TRANSFER)
                        .amount(new BigDecimal("50000.0000"))
                        .description("Cash -> Bank #" + idx)
                        .transactionDate(Instant.now())
                        .build(), null);
                return null;
            }));

            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();
                transactionService.createTransaction(userId, CreateTransactionRequest.builder()
                        .accountId(bankAccountId)
                        .toAccountId(cashAccountId)
                        .transactionType(TransactionType.TRANSFER)
                        .amount(new BigDecimal("30000.0000"))
                        .description("Bank -> Cash #" + idx)
                        .transactionDate(Instant.now())
                        .build(), null);
                return null;
            }));
        }

        assertThat(readyLatch.await(10, TimeUnit.SECONDS)).isTrue();
        startLatch.countDown();

        for (Future<?> f : futures) {
            f.get(20, TimeUnit.SECONDS);
        }
        executor.shutdown();

        BigDecimal finalCash = accountRepository.findById(cashAccountId).orElseThrow().getCurrentBalance();
        BigDecimal finalBank = accountRepository.findById(bankAccountId).orElseThrow().getCurrentBalance();

        // Expected Cash = 1,000,000 - 6*50,000 + 6*30,000 = 880,000
        // Expected Bank = 1,000,000 + 6*50,000 - 6*30,000 = 1,120,000
        assertThat(finalCash).isEqualByComparingTo("880000.0000");
        assertThat(finalBank).isEqualByComparingTo("1120000.0000");
        assertThat(finalCash.add(finalBank)).isEqualByComparingTo("2000000.0000");
    }
}

