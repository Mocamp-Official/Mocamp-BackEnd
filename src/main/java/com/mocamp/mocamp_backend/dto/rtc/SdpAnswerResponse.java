package com.mocamp.mocamp_backend.dto.rtc;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SdpAnswerResponse {
    private Long userId;
    private String sdpAnswer;
}
