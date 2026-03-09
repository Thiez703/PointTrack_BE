package com.chamcong.service;

import com.chamcong.dto.request.UpdateProfileRequest;
import com.chamcong.dto.response.ProfileResponse;
import com.chamcong.dto.response.UserResponse;

import java.util.UUID;

public interface UserService {

    UserResponse getCurrentUser(String email);

    UserResponse getUserById(UUID id);

    ProfileResponse getProfile(String email);

    ProfileResponse updateProfile(String email, UpdateProfileRequest request);
}

