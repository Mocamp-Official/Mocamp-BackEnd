package com.mocamp.mocamp_backend.service.login;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GoogleUserProfile {
    private String id;
    private String email;
    private String name;
    private String givenName;
    private String familyName;
    private String gender;
    private String picture;
    private String location;
}
