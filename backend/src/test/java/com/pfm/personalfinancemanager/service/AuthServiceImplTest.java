package com.pfm.personalfinancemanager.service;

import com.pfm.personalfinancemanager.dto.request.RefreshTokenRequest;
import com.pfm.personalfinancemanager.dto.request.RegisterRequest;
import com.pfm.personalfinancemanager.dto.response.AuthResponse;
import com.pfm.personalfinancemanager.dto.response.UserResponse;
import com.pfm.personalfinancemanager.entity.RefreshToken;
import com.pfm.personalfinancemanager.entity.User;
import com.pfm.personalfinancemanager.enums.Role;
import com.pfm.personalfinancemanager.enums.UserStatus;
import com.pfm.personalfinancemanager.exception.DuplicateResourceException;
import com.pfm.personalfinancemanager.exception.UnauthorizedAccessException;
import com.pfm.personalfinancemanager.mapper.UserMapper;
import com.pfm.personalfinancemanager.repository.PasswordResetTokenRepository;
import com.pfm.personalfinancemanager.repository.RefreshTokenRepository;
import com.pfm.personalfinancemanager.repository.UserRepository;
import com.pfm.personalfinancemanager.security.JwtService;
import com.pfm.personalfinancemanager.security.TokenBlacklistService;
import com.pfm.personalfinancemanager.service.impl.AuthServiceImpl;
import com.pfm.personalfinancemanager.util.TokenHashUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_ShouldThrowDuplicateResourceExceptionWhenEmailExists() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Nguyen Van A")
                .email("EXISTING@pfm.local")
                .password("Password@123")
                .build();

        when(userRepository.existsByEmailIgnoreCase("existing@pfm.local")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request, "Chrome"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void register_ShouldEncodePasswordAndIssueTokens() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Nguyen Van A")
                .email("NewUser@pfm.local")
                .password("Password@123")
                .currency("VND")
                .build();

        User savedUser = User.builder()
                .id("user-1")
                .email("newuser@pfm.local")
                .password("encoded-pwd")
                .fullName("Nguyen Van A")
                .currency("VND")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.existsByEmailIgnoreCase("newuser@pfm.local")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("encoded-pwd");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateAccessToken(any())).thenReturn("jwt-access-token");
        when(jwtService.getRefreshExpirationMs()).thenReturn(604_800_000L);
        when(jwtService.getAccessExpirationMs()).thenReturn(900_000L);
        when(userMapper.toResponse(savedUser)).thenReturn(UserResponse.builder().id("user-1").email("newuser@pfm.local").build());

        AuthResponse response = authService.register(request, "Chrome");

        assertThat(response.getAccessToken()).isEqualTo("jwt-access-token");
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getUser().getEmail()).isEqualTo("newuser@pfm.local");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refreshToken_ShouldRevokeAllUserTokensWhenRevokedTokenIsReused() {
        String rawToken = "reused-raw-token";
        User user = User.builder().id("user-99").email("victim@pfm.local").status(UserStatus.ACTIVE).build();
        RefreshToken revokedToken = RefreshToken.builder()
                .id("rt-1")
                .user(user)
                .tokenHash(TokenHashUtil.sha256Hex(rawToken))
                .expiryDate(Instant.now().plusSeconds(3600))
                .revoked(true)
                .build();

        when(refreshTokenRepository.findByTokenHash(TokenHashUtil.sha256Hex(rawToken)))
                .thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest(rawToken), "Agent"))
                .isInstanceOf(UnauthorizedAccessException.class);

        verify(refreshTokenRepository).revokeAllByUserId("user-99");
    }
}

