package com.mocamp.mocamp_backend.service.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserRoomData {
    private Long roomId;
    private String roomName;
    private LocalDateTime startedAt;
    private LocalTime duration;
    private Boolean status;

    @Builder.Default
    private List<GoalListData> userGoalList = new ArrayList<>();
}
