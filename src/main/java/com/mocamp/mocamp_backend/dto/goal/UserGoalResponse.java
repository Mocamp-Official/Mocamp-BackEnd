package com.mocamp.mocamp_backend.dto.goal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class UserGoalResponse {
    private Long userId;
    private List<GoalResponse> goals;
}
