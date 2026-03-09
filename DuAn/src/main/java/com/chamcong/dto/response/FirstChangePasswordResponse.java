package com.chamcong.dto.response;

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
public class FirstChangePasswordResponse {

    private String accessToken;
    private String refreshToken;
    private String message;
}

