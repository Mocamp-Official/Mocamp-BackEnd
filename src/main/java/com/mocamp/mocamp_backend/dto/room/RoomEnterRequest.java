package com.mocamp.mocamp_backend.dto.room;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RoomEnterRequest {
    private Boolean micTurnedOn;
    private Boolean camTurnedOn;
}
