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
public class AccountResponse {

    private UUID id;
    private String email;
    private String fullName;
    private String phone;
    private Role role;
    private String rank;
    private Boolean isFirstLogin;
    private Boolean isActive;
    private LocalDateTime createdAt;
}

