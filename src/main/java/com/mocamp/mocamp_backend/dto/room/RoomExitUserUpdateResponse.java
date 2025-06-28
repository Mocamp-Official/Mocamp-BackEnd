package com.mocamp.mocamp_backend.dto.room;

import com.mocamp.mocamp_backend.dto.websocket.WebsocketMessageType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RoomExitUserUpdateResponse {
    private WebsocketMessageType type;
    private Long userId;
}
