package com.mocamp.mocamp_backend.dto.room;

import com.mocamp.mocamp_backend.entity.RoomEntity;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Builder
@RequiredArgsConstructor
public class RoomResponse {
    private Long roomId;
    private String roomName;
    private Integer capacity;
    private String notice;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalTime duration;
    private String imagePath;
    private String micAvailability;

    public static RoomResponse convertEntityToDTO(RoomEntity roomEntity, String micAvailability) {
        return RoomResponse.builder()
                .roomId(roomEntity.getRoomId())
                .roomName(roomEntity.getRoomName())
                .capacity(roomEntity.getCapacity())
                .notice(roomEntity.getNotice())
                .startedAt(roomEntity.getStartedAt())
                .endedAt(roomEntity.getEndedAt())
                .duration(roomEntity.getDuration())
                .imagePath(roomEntity.getImage().getPath())
                .micAvailability(micAvailability)
                .build();
    }
}
