package com.pfm.personalfinancemanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfm.personalfinancemanager.dto.request.CreateCategoryRequest;
import com.pfm.personalfinancemanager.dto.request.RegisterRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateCategoryRequest;
import com.pfm.personalfinancemanager.enums.CategoryType;
import com.pfm.personalfinancemanager.repository.CategoryRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CategoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

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
        categoryRepository.deleteAllCustomCategories();
        passwordResetTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        tokenUserA = registerAndGetToken("Cat User A", "cat.a@pfm.local");
        tokenUserB = registerAndGetToken("Cat User B", "cat.b@pfm.local");
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
    void systemAndCustomCategories_ShouldSupportCrudAndProtectSystemAndCrossUserData() throws Exception {
        // 1. Seeded system categories available (5 INCOME + 10 EXPENSE)
        mockMvc.perform(get("/api/v1/categories?type=INCOME")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].isSystem").value(true));

        mockMvc.perform(get("/api/v1/categories?type=EXPENSE")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(10));

        // 2. User A cannot create category with same name as system category in same type
        CreateCategoryRequest duplicateSystemReq = CreateCategoryRequest.builder()
                .name("Salary")
                .categoryType(CategoryType.INCOME)
                .build();

        mockMvc.perform(post("/api/v1/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateSystemReq)))
                .andExpect(status().isConflict());

        // 3. User A creates a custom category
        CreateCategoryRequest customReq = CreateCategoryRequest.builder()
                .name("Freelance Project")
                .categoryType(CategoryType.INCOME)
                .icon("laptop")
                .color("#0EA5E9")
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isSystem").value(false))
                .andExpect(jsonPath("$.data.name").value("Freelance Project"))
                .andReturn();

        String customCatId = objectMapper.readTree(createRes.getResponse().getContentAsString())
                .get("data").get("id").asText();

        // User A sees 6 INCOME categories; User B still sees only 5
        mockMvc.perform(get("/api/v1/categories?type=INCOME")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(6));

        mockMvc.perform(get("/api/v1/categories?type=INCOME")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5));

        // 4. User B cannot view, update or delete User A's custom category (IDOR check)
        mockMvc.perform(get("/api/v1/categories/" + customCatId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                .andExpect(status().isForbidden());

        UpdateCategoryRequest updateReq = UpdateCategoryRequest.builder()
                .name("Freelance Updated")
                .icon("code-bracket")
                .color("#2563EB")
                .build();

        mockMvc.perform(put("/api/v1/categories/" + customCatId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/categories/" + customCatId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                .andExpect(status().isForbidden());

        // 5. Neither user can edit or delete a system category
        String systemSalaryId = "00000000-0000-0000-0001-000000000001";
        mockMvc.perform(put("/api/v1/categories/" + systemSalaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/categories/" + systemSalaryId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isForbidden());

        // 6. User A updates and soft-deletes custom category
        mockMvc.perform(put("/api/v1/categories/" + customCatId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Freelance Updated"));

        mockMvc.perform(delete("/api/v1/categories/" + customCatId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/categories?type=INCOME")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5));
    }
}

