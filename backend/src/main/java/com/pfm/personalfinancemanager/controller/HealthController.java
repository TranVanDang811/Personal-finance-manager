package com.pfm.personalfinancemanager.controller;

import com.pfm.personalfinancemanager.dto.response.ApiResponse;
import com.pfm.personalfinancemanager.dto.response.HealthCheckResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.Arrays;

@Slf4j
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "Health Check", description = "Kiểm tra trạng thái hoạt động của hệ thống và kết nối cơ sở dữ liệu")
public class HealthController {

    private final DataSource dataSource;
    private final Environment environment;

    @GetMapping
    @Operation(summary = "Kiểm tra trạng thái hệ thống", description = "Trả về trạng thái API service và kết nối tới Database")
    public ResponseEntity<ApiResponse<HealthCheckResponse>> checkHealth() {
        String dbStatus = checkDatabaseConnection();
        String activeProfile = Arrays.stream(environment.getActiveProfiles())
                .findFirst()
                .orElse("default");

        HealthCheckResponse response = HealthCheckResponse.builder()
                .status("UP")
                .service("personal-finance-manager")
                .version("1.0.0")
                .profile(activeProfile)
                .databaseStatus(dbStatus)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Hệ thống đang hoạt động ổn định", response));
    }

    private String checkDatabaseConnection() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2) ? "UP" : "DOWN";
        } catch (Exception ex) {
            log.warn("Database health check failed: {}", ex.getMessage());
            return "DOWN";
        }
    }
}

