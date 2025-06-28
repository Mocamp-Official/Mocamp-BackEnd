package com.mocamp.mocamp_backend.dto.user;

import com.mocamp.mocamp_backend.service.user.UserGoalData;
import com.mocamp.mocamp_backend.service.user.UserRoomData;
import com.mocamp.mocamp_backend.service.user.UserTimeData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long userId;
    private String username;
    private String imagePath;
    private Long totalDurationMinute;
    private Long totalNumberOfGoals;
    private List<UserRoomData> roomList;
    private List<UserTimeData> timeList;
    private List<UserGoalData> goalList;
}
