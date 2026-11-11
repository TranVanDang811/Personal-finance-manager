package com.pfm.personalfinancemanager.controller;

import com.pfm.personalfinancemanager.dto.request.ForgotPasswordRequest;
import com.pfm.personalfinancemanager.dto.request.LoginRequest;
import com.pfm.personalfinancemanager.dto.request.LogoutRequest;
import com.pfm.personalfinancemanager.dto.request.RefreshTokenRequest;
import com.pfm.personalfinancemanager.dto.request.RegisterRequest;
import com.pfm.personalfinancemanager.dto.request.ResetPasswordRequest;
import com.pfm.personalfinancemanager.dto.response.ApiResponse;
import com.pfm.personalfinancemanager.dto.response.AuthResponse;
import com.pfm.personalfinancemanager.security.JwtAuthenticationFilter;
import com.pfm.personalfinancemanager.service.AuthService;
import com.pfm.personalfinancemanager.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Đăng ký, đăng nhập, làm mới token, đăng xuất và khôi phục mật khẩu")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản mới")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        String deviceInfo = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        AuthResponse response = authService.register(request, deviceInfo);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký tài khoản thành công", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập bằng email và mật khẩu")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String deviceInfo = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        AuthResponse response = authService.login(request, deviceInfo);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Làm mới Access Token bằng Refresh Token (Rotation)")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        String deviceInfo = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        AuthResponse response = authService.refreshToken(request, deviceInfo);
        return ResponseEntity.ok(ApiResponse.success("Làm mới token thành công", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất và thu hồi token phiên hiện tại")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) LogoutRequest request,
            HttpServletRequest httpRequest
    ) {
        String accessToken = JwtAuthenticationFilter.extractBearerToken(httpRequest);
        String refreshToken = request != null ? request.getRefreshToken() : null;
        String currentUserId = SecurityUtils.getCurrentUserId();

        authService.logout(accessToken, refreshToken, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Yêu cầu đặt lại mật khẩu qua email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi"
        ));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Đặt lại mật khẩu bằng token xác thực")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Đặt lại mật khẩu thành công"));
    }
}

