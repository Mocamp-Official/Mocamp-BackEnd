package com.mocamp.mocamp_backend.controller;

import com.mocamp.mocamp_backend.dto.goal.GoalCompleteUpdateRequest;
import com.mocamp.mocamp_backend.dto.goal.GoalListRequest;
import com.mocamp.mocamp_backend.dto.notice.NoticeUpdateRequest;
import com.mocamp.mocamp_backend.service.goal.GoalHttpService;
import com.mocamp.mocamp_backend.service.goal.GoalSocketService;
import com.mocamp.mocamp_backend.service.room.RoomSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class RoomSocketController {
    private final RoomSocketService roomSocketService;
    private final GoalSocketService goalSocketService;

    @MessageMapping("/video/{roomId}")
    public void streamVideo(@Header("Authorization") String token,
                            @DestinationVariable("roomId") Long roomId) {
        return;
    }

    @MessageMapping("/audio/{roomId}")
    public void streamAudio(@Header("Authorization") String token,
                            @DestinationVariable("roomId") Long roomId) {
        return;
    }

    @MessageMapping("/data/goal/manage/{roomId}")
    public void manageGoal(@Payload GoalListRequest goalListRequest, @DestinationVariable("roomId") Long roomId) {
        goalSocketService.manageGoal(goalListRequest, roomId);
    }

    @MessageMapping("/data/goal/complete/{roomId}")
    public void pressGoal(@Payload GoalCompleteUpdateRequest goalCompleteUpdateRequest, @DestinationVariable("roomId") Long roomId) {
        goalSocketService.pressGoal(goalCompleteUpdateRequest, roomId);
    }

    @MessageMapping("/data/notice/{roomId}")
    public void UpdateNotice(@Payload NoticeUpdateRequest noticeUpdateRequest, @DestinationVariable("roomId") Long roomId) {
        roomSocketService.UpdateNotice(noticeUpdateRequest, roomId);
    }

}
