package com.chamcong.controller;

import com.chamcong.common.ApiResponse;
import com.chamcong.dto.request.UpdateProfileRequest;
import com.chamcong.dto.response.ProfileResponse;
import com.chamcong.dto.response.UserResponse;
import com.chamcong.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Lấy thông tin profile user hiện tại")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        ProfileResponse response = userService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Cập nhật profile user hiện tại")
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ProfileResponse response = userService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", response));
    }

    @Operation(summary = "Lấy thông tin user hiện tại")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserResponse response = userService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Lấy thông tin user theo ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}

