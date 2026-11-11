package com.pfm.personalfinancemanager.dto.response;

import com.pfm.personalfinancemanager.enums.Role;
import com.pfm.personalfinancemanager.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String currency;
    private Role role;
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
