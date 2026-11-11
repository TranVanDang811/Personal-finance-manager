package com.pfm.personalfinancemanager.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DummyExceptionController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void handleResourceNotFound_ShouldReturn404WithStandardApiResponse() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void handleInsufficientBalance_ShouldReturn422WithStandardApiResponse() throws Exception {
        mockMvc.perform(get("/test/insufficient-balance"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_BALANCE"));
    }

    @Test
    void handleValidationErrors_ShouldReturn400WithFieldErrors() throws Exception {
        String invalidPayload = "{\"name\":\"\",\"amount\":-100}";

        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    void handleUnexpectedException_ShouldReturn500WithoutLeakingDetails() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage()));
    }

    @RestController
    static class DummyExceptionController {
        @GetMapping("/test/not-found")
        void throwNotFound() {
            throw new ResourceNotFoundException("Account", "id", "abc-123");
        }

        @GetMapping("/test/insufficient-balance")
        void throwInsufficientBalance() {
            throw new InsufficientBalanceException("Số dư ví tiền mặt không đủ");
        }

        @PostMapping("/test/validate")
        void validateBody(@Valid @RequestBody DummyPayload payload) {
        }

        @GetMapping("/test/unexpected")
        void throwUnexpected() {
            throw new RuntimeException("Sensitive DB credential leak test");
        }
    }

    record DummyPayload(
            @NotBlank(message = "Tên không được để trống") String name,
            @Positive(message = "Số tiền phải lớn hơn 0") BigDecimal amount
    ) {
    }
}

