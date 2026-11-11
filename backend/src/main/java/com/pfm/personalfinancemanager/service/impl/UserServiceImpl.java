package com.pfm.personalfinancemanager.service.impl;

import com.pfm.personalfinancemanager.dto.request.ChangePasswordRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateProfileRequest;
import com.pfm.personalfinancemanager.dto.response.UserResponse;
import com.pfm.personalfinancemanager.entity.User;
import com.pfm.personalfinancemanager.exception.BusinessException;
import com.pfm.personalfinancemanager.exception.ResourceNotFoundException;
import com.pfm.personalfinancemanager.mapper.UserMapper;
import com.pfm.personalfinancemanager.repository.RefreshTokenRepository;
import com.pfm.personalfinancemanager.repository.UserRepository;
import com.pfm.personalfinancemanager.service.FileStorageService;
import com.pfm.personalfinancemanager.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final long MAX_AVATAR_SIZE_BYTES = 2L * 1024 * 1024; // 2 MB

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentProfile(String userId) {
        User user = findUserOrThrow(userId);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = findUserOrThrow(userId);
        user.setFullName(request.getFullName().trim());
        user.setCurrency(request.getCurrency().trim());
        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Mật khẩu xác nhận không khớp");
        }

        User user = findUserOrThrow(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Mật khẩu hiện tại không chính xác");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("Mật khẩu mới không được trùng với mật khẩu hiện tại");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(user.getId());
        log.info("Password changed for user {}", userId);
    }

    @Override
    @Transactional
    public UserResponse uploadAvatar(String userId, MultipartFile file) {
        User user = findUserOrThrow(userId);
        String oldAvatarUrl = user.getAvatarUrl();

        String newAvatarUrl = fileStorageService.storeImage(file, "avatars", userId, MAX_AVATAR_SIZE_BYTES);
        user.setAvatarUrl(newAvatarUrl);
        User updatedUser = userRepository.save(user);

        if (oldAvatarUrl != null && !oldAvatarUrl.equals(newAvatarUrl)) {
            fileStorageService.deleteByPublicUrl(oldAvatarUrl);
        }

        return userMapper.toResponse(updatedUser);
    }

    private User findUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}

