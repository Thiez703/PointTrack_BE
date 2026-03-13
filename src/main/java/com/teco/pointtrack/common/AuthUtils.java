package com.teco.pointtrack.common;

import com.teco.pointtrack.dto.user.UserDetail;
import com.teco.pointtrack.security.CustomUserDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthUtils {

    public static UserDetail getUserDetail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        if (!(authentication.getPrincipal() instanceof CustomUserDetail customUserDetail)) {
            return null;
        }
        return customUserDetail.getUserDetail();
    }
}
