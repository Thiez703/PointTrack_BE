package com.teco.pointtrack.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * FR-02: Response đăng nhập thành công
 * forcePasswordChange = true → FE redirect sang trang đổi MK (FR-03)
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    String accessToken;
    String refreshToken;

    Long userId;
    String fullName;
    String email;
    String avatarUrl;
    String role;

    /** BR-02: true khi NV đăng nhập lần đầu với MK tạm */
    boolean forcePasswordChange;
}
