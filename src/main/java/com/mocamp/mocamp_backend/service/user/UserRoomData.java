package com.mocamp.mocamp_backend.service.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserRoomData {
    private Long roomId;
    private String roomName;
    private LocalDateTime startedAt;
    private LocalTime duration;
    private Boolean status;
}
