package com.chamcong.service;

import com.chamcong.dto.request.CreateAccountRequest;
import com.chamcong.dto.response.AccountResponse;

import java.util.UUID;

public interface AccountService {

    AccountResponse createAccount(CreateAccountRequest request, UUID createdByUserId);
}

