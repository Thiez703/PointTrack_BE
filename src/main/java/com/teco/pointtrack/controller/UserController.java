package com.teco.pointtrack.controller;

import com.teco.pointtrack.dto.common.ApiResponse;
import com.teco.pointtrack.dto.user.UserTypeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/user")
@Tag(name = "User Management", description = "Quản lý người dùng")
public class UserController {

    @Operation(summary = "Lấy danh sách toàn bộ người dùng", 
               description = "Trả về dữ liệu theo cấu trúc UserType của Frontend")
    @GetMapping("/getAll")
    public ResponseEntity<ApiResponse<List<UserTypeResponse>>> getAllUsers() {
        // Mock dữ liệu khớp với FE UserType
        List<UserTypeResponse> users = List.of(
            UserTypeResponse.builder()
                .id(1L)
                .email("admin@pointtrack.com")
                .displayName("Quản trị viên")
                .gender("MALE")
                .avatar("https://i.pravatar.cc/150?u=1")
                .bio("I am the system administrator.")
                .onlineStatus("ONLINE")
                .build(),
            UserTypeResponse.builder()
                .id(2L)
                .email("employee@pointtrack.com")
                .displayName("Nhân viên mẫu")
                .gender("FEMALE")
                .avatar("https://i.pravatar.cc/150?u=2")
                .bio("Passionate about work.")
                .onlineStatus("OFFLINE")
                .build()
        );
        
        return ResponseEntity.ok(ApiResponse.success(users, "Lấy danh sách người dùng thành công"));
    }
}
