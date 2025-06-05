package com.mocamp.mocamp_backend.dto.goal;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.mocamp.mocamp_backend.dto.websocket.WebsocketMessageType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GoalResponse {
    private WebsocketMessageType type;
    private Long userId;
    private Long goalId;
    private String content;
    private Boolean isCompleted;

    public GoalResponse(Long goalId, String content, Boolean isCompleted) {
        this.goalId = goalId;
        this.content = content;
        this.isCompleted = isCompleted;
    }
}
