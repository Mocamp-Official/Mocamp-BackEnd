package com.mocamp.mocamp_backend.dto.commonResponse;

import lombok.*;

@Getter
@RequiredArgsConstructor
public class ErrorResponse extends CommonResponse {
    public ErrorResponse(Integer code, Object message) { super(code, message); }
}
