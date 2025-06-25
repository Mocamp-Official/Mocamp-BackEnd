package com.mocamp.mocamp_backend.dto.delegation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DelegationUpdateRequest {

    private Long newAdminId;
}
