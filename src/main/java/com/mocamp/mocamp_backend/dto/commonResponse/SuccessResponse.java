package com.mocamp.mocamp_backend.dto.commonResponse;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SuccessResponse<T> extends CommonResponse<T> {
    public SuccessResponse(Integer code, T message) {
        super(code, message);
    }
}
