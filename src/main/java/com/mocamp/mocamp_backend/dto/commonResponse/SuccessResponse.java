package com.mocamp.mocamp_backend.dto.commonResponse;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SuccessResponse extends CommonResponse {
    public SuccessResponse(Integer code, Object message) {
        super(code, message);
    }
}
