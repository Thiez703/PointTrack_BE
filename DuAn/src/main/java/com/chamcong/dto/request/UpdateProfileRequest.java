package com.chamcong.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 255)
    private String fullName;

    @Pattern(regexp = "^(\\+84|84|0)(3[2-9]|5[2689]|7[06-9]|8[1-9]|9[0-46-9])\\d{7}$")
    private String phone;

    @URL
    @Size(max = 500)
    private String avatarUrl;
}

