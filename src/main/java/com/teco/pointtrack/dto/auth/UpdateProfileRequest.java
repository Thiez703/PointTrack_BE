package com.teco.pointtrack.dto.auth;

import lombok.Data;

/**
 * PUT /api/v1/auth/profile
 * FR-07: NV chỉ được sửa phoneNumber, avatarUrl
 * KHÔNG cho sửa: email, role, salaryLevel (chỉ Admin mới sửa được)
 */
@Data
public class UpdateProfileRequest {
    String phoneNumber;
    String avatarUrl;
}
