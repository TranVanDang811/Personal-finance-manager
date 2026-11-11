package com.pfm.personalfinancemanager.service;

import com.pfm.personalfinancemanager.dto.request.ChangePasswordRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateProfileRequest;
import com.pfm.personalfinancemanager.dto.response.UserResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserResponse getCurrentProfile(String userId);

    UserResponse updateProfile(String userId, UpdateProfileRequest request);

    void changePassword(String userId, ChangePasswordRequest request);

    UserResponse uploadAvatar(String userId, MultipartFile file);
}

