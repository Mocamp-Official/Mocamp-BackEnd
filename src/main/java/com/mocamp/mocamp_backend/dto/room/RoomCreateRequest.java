package com.mocamp.mocamp_backend.dto.room;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RoomCreateRequest {
    private String roomName;
    private Integer capacity;
    private String description;
    private String duration;
    private String imagePath;
    private String micAvailability;

    private String micTurnedOn;
    private String camTurnedOn;
}
