package com.mocamp.mocamp_backend.dto.commonResponse;

import lombok.*;

@Getter
@RequiredArgsConstructor
public class ErrorResponse extends CommonResponse {
    public ErrorResponse(Integer code, String message) {
        super(code, message);
    }
}
