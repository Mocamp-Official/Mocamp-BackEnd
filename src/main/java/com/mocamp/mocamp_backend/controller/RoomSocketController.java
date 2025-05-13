package com.mocamp.mocamp_backend.controller;

import com.mocamp.mocamp_backend.service.room.RoomSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class RoomSocketController {
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomSocketService roomSocketService;

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
}
