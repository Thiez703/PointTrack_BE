package com.chamcong.service.impl;

import com.chamcong.common.enums.ErrorCode;
import com.chamcong.dto.request.CreateAccountRequest;
import com.chamcong.dto.response.AccountResponse;
import com.chamcong.exception.AppException;
import com.chamcong.mapper.UserMapper;
import com.chamcong.model.User;
import com.chamcong.repository.UserRepository;
import com.chamcong.service.AccountService;
import com.chamcong.utils.EmailService;
import com.chamcong.utils.PasswordGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final EmailService emailService;

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request, UUID createdByUserId) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String tempPassword = PasswordGenerator.generate(10);

        User creator = userRepository.findById(createdByUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(request.getRole())
                .rank(request.getRank())
                .isFirstLogin(true)
                .isActive(true)
                .createdBy(creator)
                .build();

        user = userRepository.save(user);

        emailService.sendAccountCreatedEmail(user.getEmail(), user.getFullName(), tempPassword);

        log.info("Account created for {} by admin {}", user.getEmail(), createdByUserId);

        return userMapper.toAccountResponse(user);
    }
}

