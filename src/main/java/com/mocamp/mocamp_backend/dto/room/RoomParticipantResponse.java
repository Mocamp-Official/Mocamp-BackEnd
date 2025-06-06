package com.mocamp.mocamp_backend.dto.room;

import com.mocamp.mocamp_backend.dto.goal.GoalResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class RoomParticipantResponse {
    private Long userId;
    private String userSeq;
    private String username;
    private String resolution;
    private List<GoalResponse> goals;
}
