package com.mocamp.mocamp_backend.dto.CommonResponse;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class BaseResponse {

    private Integer code;
    private String message;
}
