package com.chamcong.mapper;

import com.chamcong.dto.response.AccountResponse;
import com.chamcong.dto.response.ProfileResponse;
import com.chamcong.dto.response.UserInfoResponse;
import com.chamcong.dto.response.UserResponse;
import com.chamcong.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .rank(user.getRank())
                .isFirstLogin(user.getIsFirstLogin())
                .isActive(user.getIsActive())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public AccountResponse toAccountResponse(User user) {
        return AccountResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .rank(user.getRank())
                .isFirstLogin(user.getIsFirstLogin())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public UserInfoResponse toUserInfoResponse(User user) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .rank(user.getRank())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    public ProfileResponse toProfileResponse(User user) {
        return ProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .rank(user.getRank())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}

