package com.mocamp.mocamp_backend.controller;

import com.mocamp.mocamp_backend.dto.commonResponse.CommonResponse;
import com.mocamp.mocamp_backend.dto.goal.GoalCompleteUpdateRequest;
import com.mocamp.mocamp_backend.dto.goal.GoalListRequest;
import com.mocamp.mocamp_backend.service.goal.GoalSocketService;
import com.mocamp.mocamp_backend.service.room.RoomSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class RoomSocketController {
    private final SimpMessagingTemplate messagingTemplate;
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

    @MessageMapping("/data/notice/{roomId}")
    public void sendNotice(@Header("Authorization") String token,
                           @DestinationVariable("roomId") Long roomId) {

    }

    @MessageMapping("/data/goal/manage/{roomId}")
    public ResponseEntity<CommonResponse> manageGoal(@Payload GoalListRequest goalListRequest, @DestinationVariable("roomId") Long roomId) {
        return goalSocketService.manageGoal(goalListRequest, roomId);
    }

    @MessageMapping("/data/goal/complete/{roomId}")
    public ResponseEntity<CommonResponse> pressGoal(@Payload GoalCompleteUpdateRequest goalCompleteUpdateRequest, @DestinationVariable("roomId") Long roomId) {
        return goalSocketService.pressGoal(goalCompleteUpdateRequest, roomId);
    }
}
