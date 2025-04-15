package com.mocamp.mocamp_backend.dto.commonResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
public class ErrorResponse {

    private Integer code;
    private String message;
}
