package com.mocamp.mocamp_backend.service.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserGoalData {
    private String date;
    private Long amount;

    @Builder.Default
    private List<GoalListData> userGoalList = new ArrayList<>();
}
