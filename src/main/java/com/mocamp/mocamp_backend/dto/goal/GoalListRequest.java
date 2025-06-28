package com.mocamp.mocamp_backend.dto.goal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GoalListRequest {
    private List<GoalRequest> createGoals;
    private List<Long> deleteGoals;
    private Boolean isSecret;
}
