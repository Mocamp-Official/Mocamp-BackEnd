package com.mocamp.mocamp_backend.service.goal;

import com.mocamp.mocamp_backend.authentication.UserDetailsServiceImpl;
import com.mocamp.mocamp_backend.dto.commonResponse.CommonResponse;
import com.mocamp.mocamp_backend.dto.commonResponse.ErrorResponse;
import com.mocamp.mocamp_backend.dto.commonResponse.SuccessResponse;
import com.mocamp.mocamp_backend.dto.goal.GoalListRequest;
import com.mocamp.mocamp_backend.dto.goal.GoalListResponse;
import com.mocamp.mocamp_backend.dto.goal.GoalResponse;
import com.mocamp.mocamp_backend.entity.GoalEntity;
import com.mocamp.mocamp_backend.entity.JoinedRoomEntity;
import com.mocamp.mocamp_backend.entity.RoomEntity;
import com.mocamp.mocamp_backend.entity.UserEntity;
import com.mocamp.mocamp_backend.repository.GoalRepository;
import com.mocamp.mocamp_backend.repository.JoinedRoomRepository;
import com.mocamp.mocamp_backend.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalSocketService {

    private static final String ROOM_NOT_FOUND_MESSAGE = "방을 찾을 수 없습니다";
    private static final String ROOM_NOT_ACTIVE_MESSAGE = "활동 중인 방이 아닙니다";
    private static final String USER_NOT_IN_ROOM_MESSAGE = "해당 방에 참여 중인 유저가 아닙니다";
    private static final String GOAL_CREATE_SUCCESS_MESSAGE = "목표 생성이 완료되었습니다";

    private final SimpMessagingTemplate messagingTemplate;
    private final UserDetailsServiceImpl userDetailsService;
    private final RoomRepository roomRepository;
    private final JoinedRoomRepository joinedRoomRepository;
    private final GoalRepository goalRepository;

    /**
     * 목표 생성하는 메서드
     * @param roomId room ID
     * @return 목표 Response
     */
    public ResponseEntity<CommonResponse> createGoal(GoalListRequest goalListRequest, Long roomId) {
        UserEntity user = userDetailsService.getUserByContextHolder();

        // roomId에 해당하는 방이 존재하는지 확인
        RoomEntity roomEntity = roomRepository.findById(roomId).orElse(null);
        if (roomEntity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(404, ROOM_NOT_FOUND_MESSAGE));
        }

        // 해당하는 방이 활동중인지 확인
        if (!roomEntity.isStatus()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(403, ROOM_NOT_ACTIVE_MESSAGE));
        }

        // 해당하는 방에 소속하는 유저인지 확인
        JoinedRoomEntity joinedRoomEntity = joinedRoomRepository.findByRoom_RoomIdAndUser_UserIdAndIsParticipatingTrue(roomId, user.getUserId()).orElse(null);
        if (joinedRoomEntity == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(403, USER_NOT_IN_ROOM_MESSAGE));
        }

        // 목표 리스트 엔티티 생성 및 저장
        List<GoalEntity> goalEntities = goalListRequest.getGoals().stream()
                .map(goal -> GoalEntity.builder()
                        .content(goal.getContent())
                        .isCompleted(false)
                        .isDeleted(false)
                        .joinedRoom(joinedRoomEntity)
                        .build())
                .toList();
        goalRepository.saveAll(goalEntities);

        // 웹소캣 응답으로 보낼 GoalResponse 생성
        List<GoalResponse> goalResponseList = goalEntities.stream()
                .map(goal -> new GoalResponse(goal.getGoalId(), goal.getContent(), goal.getIsCompleted()))
                .toList();

        // WebSocket 응답 전송
        messagingTemplate.convertAndSend("/sub/data/goal/" + roomId, new GoalListResponse(goalResponseList));

        return ResponseEntity.ok(new SuccessResponse(200, GOAL_CREATE_SUCCESS_MESSAGE));
    }
}
