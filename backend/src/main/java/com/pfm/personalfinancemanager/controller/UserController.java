package com.pfm.personalfinancemanager.controller;

import com.pfm.personalfinancemanager.dto.request.ChangePasswordRequest;
import com.pfm.personalfinancemanager.dto.request.UpdateProfileRequest;
import com.pfm.personalfinancemanager.dto.response.ApiResponse;
import com.pfm.personalfinancemanager.dto.response.UserResponse;
import com.pfm.personalfinancemanager.service.UserService;
import com.pfm.personalfinancemanager.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Quản lý thông tin cá nhân, mật khẩu và ảnh đại diện của người dùng")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin hồ sơ người dùng hiện tại")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentProfile() {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(userService.getCurrentProfile(userId)));
    }

    @PutMapping("/me")
    @Operation(summary = "Cập nhật thông tin cá nhân")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        UserResponse response = userService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin cá nhân thành công", response));
    }

    @PostMapping("/me/change-password")
    @Operation(summary = "Đổi mật khẩu tài khoản hiện tại")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công"));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tải lên ảnh đại diện")
    public ResponseEntity<ApiResponse<UserResponse>> uploadAvatar(
            @RequestParam("file") MultipartFile file
    ) {
        String userId = SecurityUtils.getCurrentUserId();
        UserResponse response = userService.uploadAvatar(userId, file);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật ảnh đại diện thành công", response));
    }
}

