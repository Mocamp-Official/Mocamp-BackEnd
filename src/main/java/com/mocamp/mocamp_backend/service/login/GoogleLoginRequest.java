package com.mocamp.mocamp_backend.service.login;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginRequest {
    private String code;

    private String clientId;

    private String clientSecret;

    private String redirectUri;

    private String grantType;
}