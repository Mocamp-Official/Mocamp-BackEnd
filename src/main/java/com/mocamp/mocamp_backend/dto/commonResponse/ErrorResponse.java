package com.mocamp.mocamp_backend.dto.commonResponse;

import lombok.*;

@Getter
@RequiredArgsConstructor
public class ErrorResponse<T> extends CommonResponse<T> {
    public ErrorResponse(Integer code, T message) { super(code, message); }
}
