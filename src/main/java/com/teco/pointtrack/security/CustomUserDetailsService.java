package com.teco.pointtrack.security;

import com.teco.pointtrack.dto.user.RoleDto;
import com.teco.pointtrack.dto.user.UserDetail;
import com.teco.pointtrack.entity.User;
import com.teco.pointtrack.entity.enums.UserStatus;
import com.teco.pointtrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new UsernameNotFoundException("Tài khoản không tồn tại"));

        Set<GrantedAuthority> authorities = buildAuthorities(user);

        UserDetail userDetailDto = UserDetail.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .contact(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .role(user.getRole() != null ? new RoleDto(user.getRole()) : null)
                .build();

        // BR-22: ACTIVE = enabled, INACTIVE = disabled
        boolean enabled = (user.getStatus() == UserStatus.ACTIVE);

        return new CustomUserDetail(user.getEmail(), user.getPasswordHash(), enabled, true, userDetailDto, authorities);
    }

    private Set<GrantedAuthority> buildAuthorities(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        if (user.getRole() == null) return authorities;

        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getSlug()));

        if (user.getRole().getPermissions() != null) {
            user.getRole().getPermissions().forEach(p ->
                    authorities.add(new SimpleGrantedAuthority(p.getCode()))
            );
        }
        return authorities;
    }
}
