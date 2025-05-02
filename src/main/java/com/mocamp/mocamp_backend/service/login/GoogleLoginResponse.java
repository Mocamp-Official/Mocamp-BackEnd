package com.mocamp.mocamp_backend.service.login;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginResponse {
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private String idToken;
    private String scope;
}