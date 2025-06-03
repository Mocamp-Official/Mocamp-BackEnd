package com.mocamp.mocamp_backend.dto.rtc;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IceCandidateDto {
    private String candidate;
    private String sdpMid;
    private int sdpMLineIndex;
    private Long userId;
}
