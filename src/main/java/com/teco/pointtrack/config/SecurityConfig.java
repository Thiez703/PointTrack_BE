package com.teco.pointtrack.config;

import com.teco.pointtrack.exception.CustomAccessDeniedHandler;
import com.teco.pointtrack.security.JWTFilter;
import com.teco.pointtrack.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final JwtUtils jwtUtils;
    // Lưu ý: Nếu bạn chưa có class JwtAuthenticationEntryPoint thì nó sẽ báo đỏ dòng này nhé
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Value("${app.cors.allowed-origins}")
    private String allowedOriginPatterns;

    /** Các endpoint không cần JWT (Context-path /api được xử lý bởi server.servlet.context-path) */
    private static final String[] PUBLIC_MATCHERS = {
            // Auth endpoints
            "/v1/auth/**",
            "/auth/**",

            // Swagger / API docs
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**",
            "/swagger-resources/**", "/webjars/**", "/swagger-custom.css",

            // Static Resources
            "/uploads/**",

            // Health check
            "/actuator/**"
    };

    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults()) // Quan trọng: Bật CORS filter
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new JWTFilter(jwtUtils), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_MATCHERS).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .logout(logout -> logout.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()));

        return http.build();
    }

    // -------------------------------------------------------------------------
    // ĐÂY LÀ BEAN PASSWORD ENCODER VỪA ĐƯỢC THÊM VÀO ĐỂ FIX LỖI DATA SEEDER
    // -------------------------------------------------------------------------
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 1. Chỉ định origin duy nhất (Không dùng *)
        config.setAllowedOrigins(List.of("http://localhost:3000"));

        // 2. Cho phép credentials (để FE gửi kèm cookie/auth header)
        config.setAllowCredentials(true);

        // 3. Cho phép tất cả methods & headers
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));

        config.setMaxAge(3600L); // Cache kết quả Preflight OPTIONS trong 1 tiếng

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}