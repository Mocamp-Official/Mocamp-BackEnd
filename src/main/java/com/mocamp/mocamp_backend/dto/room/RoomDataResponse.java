package com.mocamp.mocamp_backend.dto.room;

import com.mocamp.mocamp_backend.entity.RoomEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Builder
@Getter
@AllArgsConstructor
public class RoomDataResponse {
    private Long roomId;
    private String roomName;
    private String roomSeq;
    private Integer capacity;
    private Boolean status;
    private String notice;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalTime duration;
    private String imagePath;
    private Boolean micAvailability;

    public static RoomDataResponse convertEntityToDTO(RoomEntity roomEntity) {
        return RoomDataResponse.builder()
                .roomId(roomEntity.getRoomId())
                .roomName(roomEntity.getRoomName())
                .roomSeq(roomEntity.getRoomSeq())
                .capacity(roomEntity.getCapacity())
                .status(roomEntity.getStatus())
                .notice(roomEntity.getNotice())
                .startedAt(roomEntity.getStartedAt())
                .endedAt(roomEntity.getEndedAt())
                .duration(roomEntity.getDuration())
                .imagePath(roomEntity.getImage().getPath())
                .micAvailability(roomEntity.getMicAvailability())
                .build();
    }
}
