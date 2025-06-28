package com.mocamp.mocamp_backend.controller;

import com.mocamp.mocamp_backend.dto.delegation.DelegationUpdateRequest;
import com.mocamp.mocamp_backend.dto.goal.GoalCompleteUpdateRequest;
import com.mocamp.mocamp_backend.dto.goal.GoalListRequest;
import com.mocamp.mocamp_backend.dto.notice.NoticeUpdateRequest;
import com.mocamp.mocamp_backend.dto.resolution.ResolutionUpdateRequest;
import com.mocamp.mocamp_backend.dto.rtc.IceCandidateDto;
import com.mocamp.mocamp_backend.dto.rtc.SdpOfferRequest;
import com.mocamp.mocamp_backend.dto.secret.SecretUpdateRequest;
import com.mocamp.mocamp_backend.service.goal.GoalSocketService;
import com.mocamp.mocamp_backend.service.room.RoomSocketService;
import com.mocamp.mocamp_backend.service.rtc.WebRtcService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class RoomSocketController {
    private final RoomSocketService roomSocketService;
    private final GoalSocketService goalSocketService;
    private final WebRtcService webRtcService;

    @MessageMapping("/rtc/offer/{roomId}")
    public void processOffer(@Payload SdpOfferRequest sdpOfferRequest, @DestinationVariable("roomId") Long roomId) {
        webRtcService.processOffer(sdpOfferRequest, roomId);
    }

    @MessageMapping("/rtc/ice/{roomId}")
    public void receiveIceCandidate(@Payload IceCandidateDto iceCandidateDto, @DestinationVariable("roomId") Long roomId) {
        webRtcService.receiveIceCandidate(iceCandidateDto, roomId);
    }

    @MessageMapping("/data/goal/manage/{roomId}")
    public void manageGoal(@Payload GoalListRequest goalListRequest, @DestinationVariable("roomId") Long roomId, Principal principal) {
        goalSocketService.manageGoal(goalListRequest, roomId, principal);
    }

    @MessageMapping("/data/goal/complete/{roomId}")
    public void pressGoal(@Payload GoalCompleteUpdateRequest goalCompleteUpdateRequest, @DestinationVariable("roomId") Long roomId, Principal principal) {
        goalSocketService.pressGoal(goalCompleteUpdateRequest, roomId, principal);
    }

    @MessageMapping("/data/notice/{roomId}")
    public void UpdateNotice(@Payload NoticeUpdateRequest noticeUpdateRequest, @DestinationVariable("roomId") Long roomId, Principal principal) {
        roomSocketService.UpdateNotice(noticeUpdateRequest, roomId, principal);
    }

    @MessageMapping("/data/resolution/{roomId}")
    public void UpdateResolution(@Payload ResolutionUpdateRequest resolutionUpdateRequest, @DestinationVariable("roomId") Long roomId, Principal principal) {
        roomSocketService.UpdateResolution(resolutionUpdateRequest, roomId, principal);
    }

    @MessageMapping("/data/delegation/{roomId}")
    public void UpdateDelegation(@Payload DelegationUpdateRequest delegationUpdateRequest, @DestinationVariable("roomId") Long roomId, Principal principal) {
        roomSocketService.UpdateDelegation(delegationUpdateRequest, roomId, principal);
    }

}
