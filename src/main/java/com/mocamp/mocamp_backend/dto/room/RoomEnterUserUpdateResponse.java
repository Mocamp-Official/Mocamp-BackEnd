package com.mocamp.mocamp_backend.dto.room;


import com.mocamp.mocamp_backend.dto.goal.GoalResponse;
import com.mocamp.mocamp_backend.dto.websocket.WebsocketMessageType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RoomEnterUserUpdateResponse {

    private WebsocketMessageType type;
    private Long userId;
    private String username;
    private List<GoalResponse> goals;

}
