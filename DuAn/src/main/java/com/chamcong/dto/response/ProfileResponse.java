package com.chamcong.dto.response;

import com.chamcong.common.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    private UUID id;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private Role role;
    private String rank;
    private LocalDateTime lastLoginAt;
}

