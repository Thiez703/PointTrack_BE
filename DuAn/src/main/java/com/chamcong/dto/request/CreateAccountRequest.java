package com.chamcong.dto.request;

import com.chamcong.common.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(max = 255)
    private String fullName;

    @Size(max = 20)
    private String phone;

    @NotNull
    private Role role;

    @Size(max = 100)
    private String rank;
}

