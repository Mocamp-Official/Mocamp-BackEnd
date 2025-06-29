package com.mocamp.mocamp_backend.dto.goal;

import com.mocamp.mocamp_backend.dto.websocket.WebsocketMessageType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GoalListResponse {
    private WebsocketMessageType type;
    private Long userId;
    private List<GoalResponse> goals;
    private Boolean isSecret;
    private Boolean isMyGoal;

}
