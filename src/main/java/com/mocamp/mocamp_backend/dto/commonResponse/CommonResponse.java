package com.mocamp.mocamp_backend.dto.commonResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CommonResponse {
    Integer code;
    Object message;
}
