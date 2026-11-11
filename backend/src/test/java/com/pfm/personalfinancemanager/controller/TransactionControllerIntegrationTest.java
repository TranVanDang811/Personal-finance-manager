package com.pfm.personalfinancemanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfm.personalfinancemanager.dto.request.CreateAccountRequest;
import com.pfm.personalfinancemanager.dto.request.CreateTransactionRequest;
import com.pfm.personalfinancemanager.dto.request.RegisterRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateTransactionRequest;
import com.pfm.personalfinancemanager.enums.AccountType;
import com.pfm.personalfinancemanager.enums.TransactionType;
import com.pfm.personalfinancemanager.repository.AccountRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerIntegrationTest {

    private static final String SALARY_CAT_ID = "00000000-0000-0000-0001-000000000001";
    private static final String FOOD_CAT_ID = "00000000-0000-0000-0002-000000000001";
    private static final String TRANSPORT_CAT_ID = "00000000-0000-0000-0002-000000000002";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private String cashAccountB;

    @BeforeEach
    void setUp() throws Exception {
        cleanAll();

        tokenUserA = registerAndGetToken("Alice", "alice.tx@pfm.local");
        tokenUserB = registerAndGetToken("Bob", "bob.tx@pfm.local");

        cashAccountA = createAccount(tokenUserA, "Ví Tiền Mặt", AccountType.CASH, new BigDecimal("1000000"), false);
        bankAccountA = createAccount(tokenUserA, "Thẻ Ngân Hàng", AccountType.BANK_ACCOUNT, new BigDecimal("500000"), true);
        cashAccountB = createAccount(tokenUserB, "Ví Bob", AccountType.CASH, new BigDecimal("2000000"), false);
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

    private String registerAndGetToken(String fullName, String email) throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .fullName(fullName)
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

    private String createAccount(
            String token,
            String name,
            AccountType type,
            BigDecimal initialBalance,
            boolean allowNegative
    ) throws Exception {
        CreateAccountRequest req = CreateAccountRequest.builder()
                .accountName(name)
                .accountType(type)
                .initialBalance(initialBalance)
                .allowNegative(allowNegative)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("id").asText();
    }

    @Test
    void fullTransactionLifecycle_BalancesFiltersAttachmentIdempotencyAndSecurity() throws Exception {
        // 1. Idempotency: failed request releases lock so retry with valid payload succeeds
        CreateTransactionRequest overdraftAttempt = CreateTransactionRequest.builder()
                .accountId(cashAccountA)
                .categoryId(FOOD_CAT_ID)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("5000000"))
                .description("Chi quá tay")
                .transactionDate(Instant.parse("2026-11-10T08:00:00Z"))
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .header("X-Idempotency-Key", "tx-key-retryable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overdraftAttempt)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_BALANCE"));

        // Retry with the same key now valid INCOME (+500,000 -> Cash = 1,500,000)
        CreateTransactionRequest incomeReq = CreateTransactionRequest.builder()
                .accountId(cashAccountA)
                .categoryId(SALARY_CAT_ID)
                .transactionType(TransactionType.INCOME)
                .amount(new BigDecimal("500000"))
                .description("Lương tháng 11")
                .note("Tiền thưởng dự án")
                .transactionDate(Instant.parse("2026-11-10T08:00:00Z"))
                .build();

        MvcResult incomeRes = mockMvc.perform(post("/api/v1/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .header("X-Idempotency-Key", "tx-key-retryable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomeReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount").value(500000))
                .andExpect(jsonPath("$.data.categoryName").value("Salary"))
                .andReturn();

        String incomeTxId = objectMapper.readTree(incomeRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        // Duplicate submission with completed idempotency key -> 409 CONFLICT
        mockMvc.perform(post("/api/v1/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .header("X-Idempotency-Key", "tx-key-retryable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomeReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_CONFLICT"));

        // 2. Bank account with allowNegative=true allows spending below zero (500k - 800k = -300k)
        CreateTransactionRequest bankExpenseReq = CreateTransactionRequest.builder()
                .accountId(bankAccountA)
                .categoryId(TRANSPORT_CAT_ID)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("800000"))
                .description("Vé máy bay công tác")
                .transactionDate(Instant.parse("2026-11-10T09:00:00Z"))
                .build();

        MvcResult expenseRes = mockMvc.perform(post("/api/v1/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bankExpenseReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String bankExpenseTxId = objectMapper.readTree(expenseRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        mockMvc.perform(get("/api/v1/accounts/" + bankAccountA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentBalance").value(-300000));

        // 3. Validation guards: category mismatch, same-account transfer, cross-user transfer
        CreateTransactionRequest mismatchReq = CreateTransactionRequest.builder()
                .accountId(cashAccountA)
                .categoryId(FOOD_CAT_ID)
                .transactionType(TransactionType.INCOME)
                .amount(new BigDecimal("100000"))
                .description("Danh mục chi dùng cho thu")
                .transactionDate(Instant.parse("2026-11-10T10:00:00Z"))
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mismatchReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_TYPE_MISMATCH"));

        CreateTransactionRequest foreignTransferReq = CreateTransactionRequest.builder()
                .accountId(cashAccountA)
                .toAccountId(cashAccountB)
                .transactionType(TransactionType.TRANSFER)
                .amount(new BigDecimal("100000"))
                .description("Chuyển sang ví Bob")
                .transactionDate(Instant.parse("2026-11-10T10:00:00Z"))
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(foreignTransferReq)))
                .andExpect(status().isForbidden());

        // 4. Valid Transfer: Cash (1,500k) -> Bank (-300k), amount 400k => Cash 1,100k, Bank +100k
        CreateTransactionRequest transferReq = CreateTransactionRequest.builder()
                .accountId(cashAccountA)
                .toAccountId(bankAccountA)
                .transactionType(TransactionType.TRANSFER)
                .amount(new BigDecimal("400000"))
                .description("Bù số dư âm thẻ ngân hàng")
                .transactionDate(Instant.parse("2026-11-10T11:00:00Z"))
                .build();

        MvcResult transferRes = mockMvc.perform(post("/api/v1/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.toAccountName").value("Thẻ Ngân Hàng"))
                .andReturn();

        String transferTxId = objectMapper.readTree(transferRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        // 5. Update Transfer: 400k -> 500k => Cash reverts +400k (1,500k) then -500k = 1,000k; Bank = 200k
        UpdateTransactionRequest updateTransferReq = UpdateTransactionRequest.builder()
                .accountId(cashAccountA)
                .toAccountId(bankAccountA)
                .transactionType(TransactionType.TRANSFER)
                .amount(new BigDecimal("500000"))
                .description("Bù thêm tiền vào thẻ")
                .note("Chuyển nội bộ")
                .transactionDate(Instant.parse("2026-11-10T11:30:00Z"))
                .build();

        mockMvc.perform(put("/api/v1/transactions/" + transferTxId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateTransferReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(500000));

        mockMvc.perform(get("/api/v1/accounts/" + cashAccountA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(jsonPath("$.data.currentBalance").value(1000000));

        mockMvc.perform(get("/api/v1/accounts/" + bankAccountA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(jsonPath("$.data.currentBalance").value(200000));

        // 6. Search, Filter, Pagination & Account Transaction History
        mockMvc.perform(get("/api/v1/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .param("keyword", "dự án")
                        .param("transactionType", "INCOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(incomeTxId));

        // Filtering/history by bankAccountA includes BOTH outgoing expense & incoming transfer
        mockMvc.perform(get("/api/v1/accounts/" + bankAccountA + "/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));

        // 7. Upload PNG attachment on transaction
        byte[] validPngHeader = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13};
        MockMultipartFile receiptFile = new MockMultipartFile(
                "file", "receipt.png", MediaType.IMAGE_PNG_VALUE, validPngHeader
        );

        mockMvc.perform(multipart("/api/v1/transactions/" + bankExpenseTxId + "/attachment")
                        .file(receiptFile)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attachmentUrl", startsWith("/uploads/attachments/")))
                .andExpect(jsonPath("$.data.attachmentUrl", containsString(".png")));

        // 8. IDOR check: User B cannot view, update, upload, or delete User A's transactions
        mockMvc.perform(get("/api/v1/transactions/" + incomeTxId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/transactions/" + bankExpenseTxId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                .andExpect(status().isForbidden());

        // 9. Delete transaction reverts balance: deleting bankExpenseTxId (800k EXPENSE) -> Bank 200k + 800k = 1,000k
        mockMvc.perform(delete("/api/v1/transactions/" + bankExpenseTxId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/accounts/" + bankAccountA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(jsonPath("$.data.currentBalance").value(1000000));

        // 10. Archived account cannot accept new transactions
        mockMvc.perform(patch("/api/v1/accounts/" + cashAccountA + "/archive")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk());

        CreateTransactionRequest archivedReq = CreateTransactionRequest.builder()
                .accountId(cashAccountA)
                .categoryId(FOOD_CAT_ID)
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("10000"))
                .description("Chi vào tài khoản đã lưu trữ")
                .transactionDate(Instant.now())
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(archivedReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
    }
}

