package com.mocamp.mocamp_backend.dto.goal;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GoalResponse {
    private Long goalId;
    private String content;
    private boolean isCompleted;
}
