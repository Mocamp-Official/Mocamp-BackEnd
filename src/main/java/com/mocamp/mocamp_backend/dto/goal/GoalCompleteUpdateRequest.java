package com.mocamp.mocamp_backend.dto.goal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GoalCompleteUpdateRequest {
    private Long goalId;
    private Boolean isCompleted;
}
