package com.mocamp.mocamp_backend.dto.room;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RoomCreateRequest {
    private String roomName;
    private Integer capacity;
    private String description;
    private String duration;
    private String imagePath;
    private Boolean micAvailability;

    private Boolean micTurnedOn;
    private Boolean camTurnedOn;
}
