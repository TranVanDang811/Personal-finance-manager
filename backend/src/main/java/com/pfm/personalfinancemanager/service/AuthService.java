package com.pfm.personalfinancemanager.service;

import com.pfm.personalfinancemanager.dto.request.ForgotPasswordRequest;
import com.pfm.personalfinancemanager.dto.request.LoginRequest;
import com.pfm.personalfinancemanager.dto.request.RefreshTokenRequest;
import com.pfm.personalfinancemanager.dto.request.RegisterRequest;
import com.pfm.personalfinancemanager.dto.request.ResetPasswordRequest;
import com.pfm.personalfinancemanager.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request, String deviceInfo);

    AuthResponse login(LoginRequest request, String deviceInfo);

    AuthResponse refreshToken(RefreshTokenRequest request, String deviceInfo);

    void logout(String accessToken, String refreshToken, String currentUserId);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}

