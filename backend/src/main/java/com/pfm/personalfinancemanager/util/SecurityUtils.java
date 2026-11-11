package com.pfm.personalfinancemanager.util;

import com.pfm.personalfinancemanager.exception.ErrorCode;
import com.pfm.personalfinancemanager.exception.UnauthorizedAccessException;
import com.pfm.personalfinancemanager.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new UnauthorizedAccessException(ErrorCode.UNAUTHORIZED, "Người dùng chưa được xác thực");
        }
        return userDetails;
    }

    public static String getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public static String getCurrentUserEmail() {
        return getCurrentUser().getEmail();
    }
}
