package com.mocamp.mocamp_backend.service.room;

import com.mocamp.mocamp_backend.dto.commonResponse.ErrorResponse;
import com.mocamp.mocamp_backend.dto.notice.NoticeUpdateRequest;
import com.mocamp.mocamp_backend.dto.notice.NoticeUpdateResponse;
import com.mocamp.mocamp_backend.dto.websocket.WebsocketErrorMessage;
import com.mocamp.mocamp_backend.dto.websocket.WebsocketMessageType;
import com.mocamp.mocamp_backend.entity.RoomEntity;
import com.mocamp.mocamp_backend.entity.UserEntity;
import com.mocamp.mocamp_backend.repository.JoinedRoomRepository;
import com.mocamp.mocamp_backend.repository.RoomRepository;
import com.mocamp.mocamp_backend.repository.UserRepository;
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

    private final RoomRepository roomRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final JoinedRoomRepository joinedRoomRepository;
    private final UserRepository userRepository;

    /**
     * 모캠프 방 공지사항 수정하는 메서드
     * @param noticeUpdateRequest 수정하고자 하는 공지사항
     * @param roomId room ID
     */
    public void UpdateNotice(NoticeUpdateRequest noticeUpdateRequest, Long roomId, Principal principal) {
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
}
