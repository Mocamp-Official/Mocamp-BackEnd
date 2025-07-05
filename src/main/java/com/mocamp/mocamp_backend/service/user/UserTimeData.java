package com.mocamp.mocamp_backend.service.user;

import lombok.*;

import java.util.ArrayList;
import java.util.List;


@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserTimeData {
    private String date;
    private Long duration;

    @Builder.Default
    private List<GoalListData> userGoalList = new ArrayList<>();
}
