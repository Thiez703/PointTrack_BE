package com.teco.pointtrack.config;

import com.teco.pointtrack.common.AuthUtils;
import com.teco.pointtrack.dto.user.UserDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

@Configuration
public class AuditorAwareImpl {

    @Bean(name = "auditorAware")
    public AuditorAware<Long> auditorAware() {
        return () -> {
            UserDetail userDetail = AuthUtils.getUserDetail();
            if (userDetail == null) {
                return Optional.of(9999L);
            }
            return Optional.of(userDetail.getId());
        };
    }
}
