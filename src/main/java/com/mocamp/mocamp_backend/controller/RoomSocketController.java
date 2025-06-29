package com.mocamp.mocamp_backend.controller;

import com.mocamp.mocamp_backend.dto.delegation.DelegationUpdateRequest;
import com.mocamp.mocamp_backend.dto.goal.GoalCompleteUpdateRequest;
import com.mocamp.mocamp_backend.dto.goal.GoalListRequest;
import com.mocamp.mocamp_backend.dto.notice.NoticeUpdateRequest;
import com.mocamp.mocamp_backend.dto.resolution.ResolutionUpdateRequest;
import com.mocamp.mocamp_backend.dto.rtc.IceCandidateDto;
import com.mocamp.mocamp_backend.dto.rtc.SdpOfferRequest;
import com.mocamp.mocamp_backend.dto.status.StatusDTO;
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
    public void updateNotice(@Payload NoticeUpdateRequest noticeUpdateRequest, @DestinationVariable("roomId") Long roomId, Principal principal) {
        roomSocketService.updateNotice(noticeUpdateRequest, roomId, principal);
    }

    @MessageMapping("/data/resolution/{roomId}")
    public void updateResolution(@Payload ResolutionUpdateRequest resolutionUpdateRequest, @DestinationVariable("roomId") Long roomId, Principal principal) {
        roomSocketService.updateResolution(resolutionUpdateRequest, roomId, principal);
    }

    @MessageMapping("/data/delegation/{roomId}")
    public void UpdateDelegation(@Payload DelegationUpdateRequest delegationUpdateRequest, @DestinationVariable("roomId") Long roomId, Principal principal) {
        roomSocketService.updateDelegation(delegationUpdateRequest, roomId, principal);
    }

    @MessageMapping("/data/work-status/{roomId}")
    public void UpdateWorkStatus(@Payload StatusDTO statusDTO, @DestinationVariable("roomId") Long roomId, Principal principal) {
        roomSocketService.updateWorkStatus(statusDTO, roomId, principal);
    }

    @MessageMapping("/data/cam-status/{roomId}")
    public void UpdateCamStatus(@Payload StatusDTO statusDTO, @DestinationVariable("roomId") Long roomId, Principal principal) {
        roomSocketService.updateCamStatus(statusDTO, roomId, principal);
    }

    @MessageMapping("/data/mic-status/{roomId}")
    public void UpdateMicStatus(@Payload StatusDTO statusDTO, @DestinationVariable("roomId") Long roomId, Principal principal) {
        roomSocketService.updateMicStatus(statusDTO, roomId, principal);
    }




}
