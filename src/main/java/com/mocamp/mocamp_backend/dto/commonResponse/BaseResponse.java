package com.mocamp.mocamp_backend.dto.commonResponse;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class BaseResponse {

    private Integer code;
    private String message;
}
