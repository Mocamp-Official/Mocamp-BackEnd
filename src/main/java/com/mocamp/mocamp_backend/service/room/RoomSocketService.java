package com.mocamp.mocamp_backend.service.room;

import com.mocamp.mocamp_backend.dto.commonResponse.ErrorResponse;
import com.mocamp.mocamp_backend.dto.delegation.DelegationUpdateRequest;
import com.mocamp.mocamp_backend.dto.delegation.DelegationUpdateResponse;
import com.mocamp.mocamp_backend.dto.notice.NoticeUpdateRequest;
import com.mocamp.mocamp_backend.dto.notice.NoticeUpdateResponse;
import com.mocamp.mocamp_backend.dto.resolution.ResolutionUpdateRequest;
import com.mocamp.mocamp_backend.dto.resolution.ResolutionUpdateResponse;
import com.mocamp.mocamp_backend.dto.status.StatusDTO;
import com.mocamp.mocamp_backend.dto.websocket.WebsocketErrorMessage;
import com.mocamp.mocamp_backend.dto.websocket.WebsocketMessageType;
import com.mocamp.mocamp_backend.entity.JoinedRoomEntity;
import com.mocamp.mocamp_backend.entity.RoomEntity;
import com.mocamp.mocamp_backend.entity.UserEntity;
import com.mocamp.mocamp_backend.repository.JoinedRoomRepository;
import com.mocamp.mocamp_backend.repository.RoomRepository;
import com.mocamp.mocamp_backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomSocketService {

    private static final String ROOM_NOT_FOUND_MESSAGE = "방을 찾을 수 없습니다";
    private static final String ROOM_NOT_ACTIVE_MESSAGE = "활동 중인 방이 아닙니다";
    private static final String ROOM_NOT_ADMIN_MESSAGE = "방장이 아닙니다";
    private static final String USER_NOT_FOUND_MESSAGE = "유저정보 조회에 실패했습니다";
    private static final String USER_NOT_IN_ROOM_MESSAGE = "해당 방에 참여 중인 유저가 아닙니다";
    private static final String USER_NOT_HOST_MESSAGE = "해당 방의 방장이 아닙니다";
    private static final String ANOTHER_USER_STATUS_TOGGLE_MESSAGE = "다른 유저의 상태를 토글했습니다";

    private final RoomRepository roomRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final JoinedRoomRepository joinedRoomRepository;
    private final UserRepository userRepository;

    /**
     * 모캠프 방 공지사항 수정하는 메서드
     * @param noticeUpdateRequest 수정하고자 하는 공지사항
     * @param roomId room ID
     */
    @Transactional
    public void updateNotice(NoticeUpdateRequest noticeUpdateRequest, Long roomId, Principal principal) {
        String email = principal.getName();
        UserEntity user = userRepository.findUserByEmail(email).orElse(null);
        if (user == null) {
            log.warn("[유저 조회 실패] userId: {}", user.getUserId());
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(404, new WebsocketErrorMessage(user.getUserId(), USER_NOT_FOUND_MESSAGE)));
            return;
        }

        log.info("[공지사항 변경 요청] userId: {}, roomId: {}" ,user.getUserId(), roomId);

        // roomId에 해당하는 방이 존재하는지 확인
        RoomEntity roomEntity = roomRepository.findById(roomId).orElse(null);
        if (roomEntity == null) {
            log.warn("[방 조회 실패] roomId: {}", roomId);
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(404, new WebsocketErrorMessage(user.getUserId(), ROOM_NOT_FOUND_MESSAGE)));
            return;
        }

        // 해당하는 방이 활동중인지 확인
        if (!roomEntity.getStatus()) {
            log.warn("[비활성화된 방 접근] roomId: {}", roomId);
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(403, new WebsocketErrorMessage(user.getUserId(), ROOM_NOT_ACTIVE_MESSAGE)));
            return;
        }

        // 해당하는 방에서 방장인지 확인
        if(!joinedRoomRepository.existsByRoom_RoomIdAndUser_UserIdAndIsAdminTrue(roomId, user.getUserId())) {
            log.warn("[방장 아닌 사람이 접근 차단] userId: {}, roomId: {}", user.getUserId(), roomId);
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(403, new WebsocketErrorMessage(user.getUserId(), ROOM_NOT_ADMIN_MESSAGE)));
            return;
        }

        // 방의 공지사항 변경 및 저장
        roomEntity.updateNotice(noticeUpdateRequest.getNotice());
        roomRepository.save(roomEntity);

        log.info("[공지사항 변경 완료] roomId: {}", roomId);
        // WebSocket 응답 전송
        messagingTemplate.convertAndSend("/sub/data/" + roomId , new NoticeUpdateResponse(WebsocketMessageType.NOTICE_UPDATED, noticeUpdateRequest.getNotice()));

    }

    /**
     * 모캠프 방 다짐 수정하는 메서드
     * @param resolutionUpdateRequest 수정하고자 하는 다짐
     * @param roomId room ID
     */
    @Transactional
    public void updateResolution(ResolutionUpdateRequest resolutionUpdateRequest, Long roomId, Principal principal) {
        String email = principal.getName();
        UserEntity user = userRepository.findUserByEmail(email).orElse(null);
        if (user == null) {
            log.warn("[유저 조회 실패] userId: {}", user.getUserId());
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(404, new WebsocketErrorMessage(user.getUserId(), USER_NOT_FOUND_MESSAGE)));
            return;
        }

        log.info("[다짐 변경 요청] userId: {}, roomId: {}" ,user.getUserId(), roomId);

        // roomId에 해당하는 방이 존재하는지 확인
        RoomEntity roomEntity = roomRepository.findById(roomId).orElse(null);
        if (roomEntity == null) {
            log.warn("[방 조회 실패] roomId: {}", roomId);
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(404, new WebsocketErrorMessage(user.getUserId(), ROOM_NOT_FOUND_MESSAGE)));
            return;
        }

        // 해당하는 방이 활동중인지 확인
        if (!roomEntity.getStatus()) {
            log.warn("[비활성화된 방 접근] roomId: {}", roomId);
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(403, new WebsocketErrorMessage(user.getUserId(), ROOM_NOT_ACTIVE_MESSAGE)));
            return;
        }

        // 해당 방에 참여중인 유저인지 확인
        JoinedRoomEntity joinedRoomEntity = joinedRoomRepository.findByRoom_RoomIdAndUser_UserIdAndIsParticipatingTrue(roomId, user.getUserId()).orElse(null);
        if (joinedRoomEntity == null) {
            log.warn("[방에 참여중인 유저인지 확인] 방에 소속되지 않은 사용자 - userId: {}, roomId: {}", user.getUserId(), roomId);
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(403, new WebsocketErrorMessage(user.getUserId(), USER_NOT_IN_ROOM_MESSAGE)));
            return;
        }

        // 유저 다짐 변경 및 저장
        joinedRoomEntity.updateResolution(resolutionUpdateRequest.getResolution());
        joinedRoomRepository.save(joinedRoomEntity);

        log.info("[다짐 변경 완료] userId: {}, roomId: {}, resolution: {}" ,user.getUserId(), roomId, resolutionUpdateRequest.getResolution());
        // WebSocket 응답 전송
        messagingTemplate.convertAndSend("/sub/data/" + roomId , new ResolutionUpdateResponse(WebsocketMessageType.RESOLUTION_UPDATED, user.getUserId(), resolutionUpdateRequest.getResolution()));

    }

    /**
     * 모캠프 방 방장 위임하는 메서드
     * @param delegationUpdateRequest 방장을 위임할 유저 ID
     * @param roomId 현재 접속하고 있는 방 ID
     */
    @Transactional
    public void updateDelegation(DelegationUpdateRequest delegationUpdateRequest, Long roomId, Principal principal) {
        String email = principal.getName();
        UserEntity user = userRepository.findUserByEmail(email).orElse(null);
        if (user == null) {
            log.warn("[유저 조회 실패] userId: {}", user.getUserId());
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(404, new WebsocketErrorMessage(user.getUserId(), USER_NOT_FOUND_MESSAGE)));
            return;
        }

        log.info("[방장 위임 요청] userId: {} --> userId: {}으로 방장 위임" ,user.getUserId(), delegationUpdateRequest.getNewAdminId());

        // roomId에 해당하는 방이 존재하는지 확인
        RoomEntity roomEntity = roomRepository.findById(roomId).orElse(null);
        if (roomEntity == null) {
            log.warn("[방 조회 실패] roomId: {}", roomId);
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(404, new WebsocketErrorMessage(user.getUserId(), ROOM_NOT_FOUND_MESSAGE)));
            return;
        }

        // 해당하는 방이 활동중인지 확인
        if (!roomEntity.getStatus()) {
            log.warn("[비활성화된 방 접근] roomId: {}", roomId);
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(403, new WebsocketErrorMessage(user.getUserId(), ROOM_NOT_ACTIVE_MESSAGE)));
            return;
        }

        // 해당 방에 참여중인 유저인지 확인
        JoinedRoomEntity joinedRoomEntity = joinedRoomRepository.findByRoom_RoomIdAndUser_UserIdAndIsParticipatingTrue(roomId, user.getUserId()).orElse(null);
        if (joinedRoomEntity == null) {
            log.warn("[방에 참여중인 유저인지 확인] 방에 소속되지 않은 사용자 - userId: {}, roomId: {}", user.getUserId(), roomId);
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(403, new WebsocketErrorMessage(user.getUserId(), USER_NOT_IN_ROOM_MESSAGE)));
            return;
        }

        // 해당 방의 위임 요청한 유저가 방장인지 확인
        if(!joinedRoomEntity.getIsAdmin()) {
            log.warn("[방의 방장인 유저인지 확인] 방의 방장이 아닌 유저 - userId: {}, roomId: {}", user.getUserId(), roomId);
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(403, new WebsocketErrorMessage(user.getUserId(), USER_NOT_HOST_MESSAGE)));
            return;
        }

        // 위임을 받을 유저 ID에 방장 권한 부여
        JoinedRoomEntity delegatedJoinedRoomEntity = joinedRoomRepository.findByRoom_RoomIdAndUser_UserIdAndIsParticipatingTrue(roomId, delegationUpdateRequest.getNewAdminId()).orElse(null);
        delegatedJoinedRoomEntity.updateIsAdmin(true);
        log.info("[새로운 방장으로 변경] 새로운 방장 - userId: {}", delegationUpdateRequest.getNewAdminId());

        // 위임을 전달받은 유저 이름 추출
        String delegatedUsername = delegatedJoinedRoomEntity.getUser().getUsername();

        // 위임을 전달할 유저 ID는 방장 권한 해제
        joinedRoomEntity.updateIsAdmin(false);
        log.info("[기존 방장은 참여자로 변경] 방장 -> 참여자 - userId: {}", user.getUserId());

        // WebSocket 응답 전송
        messagingTemplate.convertAndSend("/sub/data/" + roomId , new DelegationUpdateResponse(WebsocketMessageType.ADMIN_UPDATED, user.getUsername(), delegatedUsername));
    }

    /**
     * 모캠프 작업 공간에서 작업 상태 변경하는 메서드
     * @param statusDTO 작업 상태
     * @param roomId 모캠프 방 ID
     */
    public void updateWorkStatus(StatusDTO statusDTO, Long roomId, Principal principal) {
        String email = principal.getName();
        UserEntity user = userRepository.findUserByEmail(email).orElse(null);
        if (user == null) {
            log.warn("[유저 조회 실패] userId: {}", user.getUserId());
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(404, new WebsocketErrorMessage(user.getUserId(), USER_NOT_FOUND_MESSAGE)));
            return;
        }

        log.info("[작업 상태 변경 요청] userId: {}, roomId: {}" ,user.getUserId(), roomId);

        // roomId에 해당하는 방이 존재하는지 확인
        RoomEntity roomEntity = roomRepository.findById(roomId).orElse(null);
        if (roomEntity == null) {
            log.warn("[방 조회 실패] roomId: {}", roomId);
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(404, new WebsocketErrorMessage(user.getUserId(), ROOM_NOT_FOUND_MESSAGE)));
            return;
        }

        // 해당하는 방이 활동중인지 확인
        if (!roomEntity.getStatus()) {
            log.warn("[비활성화된 방 접근] roomId: {}", roomId);
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(403, new WebsocketErrorMessage(user.getUserId(), ROOM_NOT_ACTIVE_MESSAGE)));
            return;
        }

        // 해당 방에 참여중인 유저인지 확인
        JoinedRoomEntity joinedRoomEntity = joinedRoomRepository.findByRoom_RoomIdAndUser_UserIdAndIsParticipatingTrue(roomId, user.getUserId()).orElse(null);
        if (joinedRoomEntity == null) {
            log.warn("[방에 참여중인 유저인지 확인] 방에 소속되지 않은 사용자 - userId: {}, roomId: {}", user.getUserId(), roomId);
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(403, new WebsocketErrorMessage(user.getUserId(), USER_NOT_IN_ROOM_MESSAGE)));
            return;
        }

        // 다른 사용자 작업 상태 토글인지 확인
        if(!user.getUserId().equals(statusDTO.getUserId())) {
            log.warn("[작업 상태 변경 실패] 본인의 상태만 변경 가능 - 요청자: {}, 대상: {}", user.getUserId(), statusDTO.getUserId());
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(403, new WebsocketErrorMessage(user.getUserId(), ANOTHER_USER_STATUS_TOGGLE_MESSAGE)));
            return;
        }

        // 작업 상태 변경 및 저장
        joinedRoomEntity.updateWorkStatus(statusDTO.getWorkStatus());
        JoinedRoomEntity updatedJoinedRoomEntity = joinedRoomRepository.save(joinedRoomEntity);

        log.info("[작업 상태 변경 완료] userId: {}, roomId: {}, workStatus: {}" ,user.getUserId(), roomId, updatedJoinedRoomEntity.getWorkStatus());
        // WebSocket 응답 전송
        StatusDTO workStatusDTO = StatusDTO.builder()
                .type(WebsocketMessageType.WORK_STATUS_UPDATED)
                .userId(user.getUserId())
                .workStatus(updatedJoinedRoomEntity.getWorkStatus())
                .build();
        messagingTemplate.convertAndSend("/sub/data/" + roomId ,workStatusDTO);
    }

    /**
     * 모캠프 작업 공간에서 캠 상태 변경하는 메서드
     * @param statusDTO 작업 상태
     * @param roomId 모캠프 방 ID
     */
    public void updateCamStatus(StatusDTO statusDTO, Long roomId, Principal principal) {
        String email = principal.getName();
        UserEntity user = userRepository.findUserByEmail(email).orElse(null);
        if (user == null) {
            log.warn("[유저 조회 실패] userId: {}", user.getUserId());
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(404, new WebsocketErrorMessage(user.getUserId(), USER_NOT_FOUND_MESSAGE)));
            return;
        }

        log.info("[작업 상태 변경 요청] userId: {}, roomId: {}" ,user.getUserId(), roomId);

        // roomId에 해당하는 방이 존재하는지 확인
        RoomEntity roomEntity = roomRepository.findById(roomId).orElse(null);
        if (roomEntity == null) {
            log.warn("[방 조회 실패] roomId: {}", roomId);
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(404, new WebsocketErrorMessage(user.getUserId(), ROOM_NOT_FOUND_MESSAGE)));
            return;
        }

        // 해당하는 방이 활동중인지 확인
        if (!roomEntity.getStatus()) {
            log.warn("[비활성화된 방 접근] roomId: {}", roomId);
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(403, new WebsocketErrorMessage(user.getUserId(), ROOM_NOT_ACTIVE_MESSAGE)));
            return;
        }

        // 해당 방에 참여중인 유저인지 확인
        JoinedRoomEntity joinedRoomEntity = joinedRoomRepository.findByRoom_RoomIdAndUser_UserIdAndIsParticipatingTrue(roomId, user.getUserId()).orElse(null);
        if (joinedRoomEntity == null) {
            log.warn("[방에 참여중인 유저인지 확인] 방에 소속되지 않은 사용자 - userId: {}, roomId: {}", user.getUserId(), roomId);
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(403, new WebsocketErrorMessage(user.getUserId(), USER_NOT_IN_ROOM_MESSAGE)));
            return;
        }

        // 다른 사용자 캠 상태 토글인지 확인
        if(!user.getUserId().equals(statusDTO.getUserId())) {
            log.warn("[캠 상태 변경 실패] 본인의 상태만 변경 가능 - 요청자: {}, 대상: {}", user.getUserId(), statusDTO.getUserId());
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(403, new WebsocketErrorMessage(user.getUserId(), ANOTHER_USER_STATUS_TOGGLE_MESSAGE)));
            return;
        }

        // 캠 상태 변경 및 저장
        joinedRoomEntity.updateCamStatus(statusDTO.getCamStatus());
        JoinedRoomEntity updatedJoinedRoomEntity = joinedRoomRepository.save(joinedRoomEntity);

        log.info("[캠 상태 변경 완료] userId: {}, roomId: {}, camStatus: {}" ,user.getUserId(), roomId, updatedJoinedRoomEntity.getCamStatus());
        // WebSocket 응답 전송
        StatusDTO camStatusDTO = StatusDTO.builder()
                .type(WebsocketMessageType.CAM_STATUS_UPDATED)
                .userId(user.getUserId())
                .camStatus(updatedJoinedRoomEntity.getCamStatus())
                .build();
        messagingTemplate.convertAndSend("/sub/data/" + roomId ,camStatusDTO);
    }

    /**
     * 모캠프 작업 공간에서 마이크 상태 변경하는 메서드
     * @param statusDTO 작업 상태
     * @param roomId 모캠프 방 ID
     */
    public void updateMicStatus(StatusDTO statusDTO, Long roomId, Principal principal) {
        String email = principal.getName();
        UserEntity user = userRepository.findUserByEmail(email).orElse(null);
        if (user == null) {
            log.warn("[유저 조회 실패] userId: {}", user.getUserId());
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(404, new WebsocketErrorMessage(user.getUserId(), USER_NOT_FOUND_MESSAGE)));
            return;
        }

        log.info("[작업 상태 변경 요청] userId: {}, roomId: {}" ,user.getUserId(), roomId);

        // roomId에 해당하는 방이 존재하는지 확인
        RoomEntity roomEntity = roomRepository.findById(roomId).orElse(null);
        if (roomEntity == null) {
            log.warn("[방 조회 실패] roomId: {}", roomId);
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(404, new WebsocketErrorMessage(user.getUserId(), ROOM_NOT_FOUND_MESSAGE)));
            return;
        }

        // 해당하는 방이 활동중인지 확인
        if (!roomEntity.getStatus()) {
            log.warn("[비활성화된 방 접근] roomId: {}", roomId);
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(403, new WebsocketErrorMessage(user.getUserId(), ROOM_NOT_ACTIVE_MESSAGE)));
            return;
        }

        // 해당 방에 참여중인 유저인지 확인
        JoinedRoomEntity joinedRoomEntity = joinedRoomRepository.findByRoom_RoomIdAndUser_UserIdAndIsParticipatingTrue(roomId, user.getUserId()).orElse(null);
        if (joinedRoomEntity == null) {
            log.warn("[방에 참여중인 유저인지 확인] 방에 소속되지 않은 사용자 - userId: {}, roomId: {}", user.getUserId(), roomId);
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(403, new WebsocketErrorMessage(user.getUserId(), USER_NOT_IN_ROOM_MESSAGE)));
            return;
        }

        // 다른 사용자 마이크 상태 토글인지 확인
        if(!user.getUserId().equals(statusDTO.getUserId())) {
            log.warn("[마이크 상태 변경 실패] 본인의 상태만 변경 가능 - 요청자: {}, 대상: {}", user.getUserId(), statusDTO.getUserId());
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(403, new WebsocketErrorMessage(user.getUserId(), ANOTHER_USER_STATUS_TOGGLE_MESSAGE)));
            return;
        }

        // 마이크 상태 변경 및 저장
        joinedRoomEntity.updateMicStatus(statusDTO.getMicStatus());
        JoinedRoomEntity updatedJoinedRoomEntity = joinedRoomRepository.save(joinedRoomEntity);

        log.info("[캠 상태 변경 완료] userId: {}, roomId: {}, micStatus: {}" ,user.getUserId(), roomId, updatedJoinedRoomEntity.getMicStatus());
        // WebSocket 응답 전송
        StatusDTO micStatusDTO = StatusDTO.builder()
                .type(WebsocketMessageType.MIC_STATUS_UPDATED)
                .userId(user.getUserId())
                .micStatus(updatedJoinedRoomEntity.getMicStatus())
                .build();
        messagingTemplate.convertAndSend("/sub/data/" + roomId ,micStatusDTO);
    }
}
