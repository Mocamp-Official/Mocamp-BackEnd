package com.mocamp.mocamp_backend.service.room;

import com.mocamp.mocamp_backend.authentication.UserDetailsServiceImpl;
import com.mocamp.mocamp_backend.dto.commonResponse.ErrorResponse;
import com.mocamp.mocamp_backend.dto.commonResponse.SuccessResponse;
import com.mocamp.mocamp_backend.dto.goal.GoalResponse;
import com.mocamp.mocamp_backend.dto.notice.NoticeUpdateRequest;
import com.mocamp.mocamp_backend.entity.RoomEntity;
import com.mocamp.mocamp_backend.entity.UserEntity;
import com.mocamp.mocamp_backend.repository.JoinedRoomRepository;
import com.mocamp.mocamp_backend.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomSocketService {

    private static final String ROOM_NOT_FOUND_MESSAGE = "방을 찾을 수 없습니다";
    private static final String ROOM_NOT_ACTIVE_MESSAGE = "활동 중인 방이 아닙니다";
    private static final String ROOM_NOT_ADMIN_MESSAGE = "방장이 아닙니다";

    private final RoomRepository roomRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final SimpMessagingTemplate messagingTemplate;
    private final JoinedRoomRepository joinedRoomRepository;

    /**
     * 모캠프 방 공지사항 수정하는 메서드
     * @param noticeUpdateRequest 수정하고자 하는 공지사항
     * @param roomId room ID
     */
    public void UpdateNotice(NoticeUpdateRequest noticeUpdateRequest, Long roomId) {
        UserEntity user = userDetailsService.getUserByContextHolder();

        // roomId에 해당하는 방이 존재하는지 확인
        RoomEntity roomEntity = roomRepository.findById(roomId).orElse(null);
        if (roomEntity == null) {
            messagingTemplate.convertAndSend("/sub/data/notice/" + roomId, new ErrorResponse(404, ROOM_NOT_FOUND_MESSAGE));
        }

        // 해당하는 방이 활동중인지 확인
        if (!roomEntity.getStatus()) {
            messagingTemplate.convertAndSend("/sub/data/notice/" + roomId, new ErrorResponse(403, ROOM_NOT_ACTIVE_MESSAGE));
        }

        // 해당하는 방에서 방장인지 확인
        if(!joinedRoomRepository.existsByRoom_RoomIdAndUser_UserIdAndIsAdminTrue(roomId, user.getUserId())) {
            messagingTemplate.convertAndSend("/sub/data/notice/" + roomId, new ErrorResponse(403, ROOM_NOT_ADMIN_MESSAGE));
        }

        // 방의 공지사항 변경 및 저장
        roomEntity.updateNotice(noticeUpdateRequest.getNotice());
        roomRepository.save(roomEntity);

        // WebSocket 응답 전송
        messagingTemplate.convertAndSend("/sub/data/notice/" + roomId , new SuccessResponse(200, noticeUpdateRequest.getNotice()));

    }
}
