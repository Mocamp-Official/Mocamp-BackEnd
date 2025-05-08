package com.mocamp.mocamp_backend.dto.naver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class NaverLoginResponse {

    private Long id;
    private String email;
    private String username;
    private String accessToken;
    private String refreshToken;
}
