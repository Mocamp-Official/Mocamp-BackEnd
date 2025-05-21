package com.mocamp.mocamp_backend.dto.loginResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class LoginResult {
    private String accessToken;
    private String refreshToken;
}
