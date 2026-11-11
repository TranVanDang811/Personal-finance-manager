package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.dto.request.ForgotPasswordRequest;
import com.pfm.personalfinancemanager.dto.request.LoginRequest;
import com.pfm.personalfinancemanager.dto.request.RefreshTokenRequest;
import com.pfm.personalfinancemanager.dto.request.RegisterRequest;
import com.pfm.personalfinancemanager.dto.request.ResetPasswordRequest;
import com.pfm.personalfinancemanager.dto.response.AuthResponse;
import com.pfm.personalfinancemanager.entity.PasswordResetToken;
import com.pfm.personalfinancemanager.entity.RefreshToken;
import com.pfm.personalfinancemanager.entity.User;
import com.pfm.personalfinancemanager.enums.Role;
import com.pfm.personalfinancemanager.enums.UserStatus;
import com.pfm.personalfinancemanager.exception.BusinessException;
import com.pfm.personalfinancemanager.exception.DuplicateResourceException;
import com.pfm.personalfinancemanager.exception.ErrorCode;
import com.pfm.personalfinancemanager.exception.UnauthorizedAccessException;
import com.pfm.personalfinancemanager.mapper.UserMapper;
import com.pfm.personalfinancemanager.repository.PasswordResetTokenRepository;
import com.pfm.personalfinancemanager.repository.RefreshTokenRepository;
import com.pfm.personalfinancemanager.repository.UserRepository;
import com.pfm.personalfinancemanager.security.CustomUserDetails;
import com.pfm.personalfinancemanager.security.JwtService;
import com.pfm.personalfinancemanager.security.TokenBlacklistService;
import com.pfm.personalfinancemanager.service.AuthService;
import com.pfm.personalfinancemanager.service.EmailService;
import com.pfm.personalfinancemanager.util.TokenHashUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserMapper userMapper;
    private final EmailService emailService;

    @Value("${app.jwt.password-reset-expiration-ms:900000}")
    private long passwordResetExpirationMs;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, String deviceInfo) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("Tài khoản với email '" + normalizedEmail + "' đã tồn tại");
        }

        User user = User.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .currency(StringUtils.hasText(request.getCurrency()) ? request.getCurrency() : "VND")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user: {}", savedUser.getId());
        return issueAuthTokens(savedUser, deviceInfo);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String deviceInfo) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (AuthenticationException ex) {
            throw new UnauthorizedAccessException(ErrorCode.INVALID_CREDENTIALS, "Email hoặc mật khẩu không chính xác");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new UnauthorizedAccessException(ErrorCode.INVALID_CREDENTIALS, "Không tìm thấy tài khoản"));

        return issueAuthTokens(user, deviceInfo);
    }

    @Override
    @Transactional(noRollbackFor = UnauthorizedAccessException.class)
    public AuthResponse refreshToken(RefreshTokenRequest request, String deviceInfo) {
        String tokenHash = TokenHashUtil.sha256Hex(request.getRefreshToken());
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedAccessException(ErrorCode.INVALID_TOKEN, "Refresh token không hợp lệ"));

        User user = storedToken.getUser();

        if (storedToken.isRevoked()) {
            // Token reuse detected: revoke all active sessions of this user as a safeguard.
            refreshTokenRepository.revokeAllByUserId(user.getId());
            log.warn("Refresh token reuse detected for user {}. All refresh tokens revoked.", user.getId());
            throw new UnauthorizedAccessException(ErrorCode.INVALID_TOKEN,
                    "Phiên đăng nhập không hợp lệ hoặc đã bị thu hồi, vui lòng đăng nhập lại");
        }

        if (storedToken.getExpiryDate().isBefore(Instant.now())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new UnauthorizedAccessException(ErrorCode.INVALID_TOKEN, "Refresh token đã hết hạn");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedAccessException(ErrorCode.UNAUTHORIZED, "Tài khoản hiện đang bị khóa hoặc ngừng hoạt động");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        return issueAuthTokens(user, deviceInfo);
    }

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken, String currentUserId) {
        if (StringUtils.hasText(accessToken)) {
            try {
                Claims claims = jwtService.parseAndValidateToken(accessToken);
                String jti = jwtService.extractJti(claims);
                Instant expiration = jwtService.extractExpiration(claims);
                Duration ttl = Duration.between(Instant.now(), expiration);
                if (!ttl.isNegative() && !ttl.isZero()) {
                    tokenBlacklistService.blacklistToken(jti, ttl);
                }
            } catch (JwtException | IllegalArgumentException ex) {
                log.debug("Logout access token already invalid or expired");
            }
        }

        if (StringUtils.hasText(refreshToken)) {
            String tokenHash = TokenHashUtil.sha256Hex(refreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(storedToken -> {
                if (storedToken.getUser().getId().equals(currentUserId)) {
                    storedToken.setRevoked(true);
                    refreshTokenRepository.save(storedToken);
                }
            });
        }
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(user -> {
            if (user.getStatus() == UserStatus.ACTIVE) {
                passwordResetTokenRepository.invalidateAllByUserId(user.getId());

                String rawToken = TokenHashUtil.generateSecureRandomToken(32);
                PasswordResetToken resetToken = PasswordResetToken.builder()
                        .user(user)
                        .tokenHash(TokenHashUtil.sha256Hex(rawToken))
                        .expiryDate(Instant.now().plusMillis(passwordResetExpirationMs))
                        .used(false)
                        .build();

                passwordResetTokenRepository.save(resetToken);
                emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), rawToken);
            }
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Mật khẩu xác nhận không khớp");
        }

        String tokenHash = TokenHashUtil.sha256Hex(request.getToken());
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_RESET_TOKEN, "Token đặt lại mật khẩu không hợp lệ"));

        if (resetToken.isUsed() || resetToken.getExpiryDate().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.INVALID_RESET_TOKEN, "Token đặt lại mật khẩu đã hết hạn hoặc đã được sử dụng");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        refreshTokenRepository.revokeAllByUserId(user.getId());
        log.info("Password reset completed for user {}", user.getId());
    }

    private AuthResponse issueAuthTokens(User user, String deviceInfo) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateAccessToken(userDetails);
        String rawRefreshToken = TokenHashUtil.generateSecureRandomToken(48);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHashUtil.sha256Hex(rawRefreshToken))
                .deviceInfo(truncateDeviceInfo(deviceInfo))
                .expiryDate(Instant.now().plusMillis(jwtService.getRefreshExpirationMs()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessExpirationMs() / 1000)
                .user(userMapper.toResponse(user))
                .build();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String truncateDeviceInfo(String deviceInfo) {
        if (deviceInfo == null) {
            return null;
        }
        return deviceInfo.length() > 255 ? deviceInfo.substring(0, 255) : deviceInfo;
    }
}

