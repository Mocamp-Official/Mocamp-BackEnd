package com.mocamp.mocamp_backend.service.goal;

import com.mocamp.mocamp_backend.authentication.UserDetailsServiceImpl;
import com.mocamp.mocamp_backend.dto.commonResponse.ErrorResponse;
import com.mocamp.mocamp_backend.dto.goal.GoalCompleteUpdateRequest;
import com.mocamp.mocamp_backend.dto.goal.GoalListRequest;
import com.mocamp.mocamp_backend.dto.goal.GoalListResponse;
import com.mocamp.mocamp_backend.dto.goal.GoalResponse;
import com.mocamp.mocamp_backend.dto.websocket.WebsocketErrorMessage;
import com.mocamp.mocamp_backend.dto.websocket.WebsocketMessageType;
import com.mocamp.mocamp_backend.entity.GoalEntity;
import com.mocamp.mocamp_backend.entity.JoinedRoomEntity;
import com.mocamp.mocamp_backend.entity.RoomEntity;
import com.mocamp.mocamp_backend.entity.UserEntity;
import com.mocamp.mocamp_backend.repository.GoalRepository;
import com.mocamp.mocamp_backend.repository.JoinedRoomRepository;
import com.mocamp.mocamp_backend.repository.RoomRepository;
import com.mocamp.mocamp_backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.parameters.P;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoalSocketService {

    private static final String ROOM_NOT_FOUND_MESSAGE = "방을 찾을 수 없습니다";
    private static final String GOAL_NOT_FOUND_MESSAGE = "목표를 찾을 수 없습니다";
    private static final String ROOM_NOT_ACTIVE_MESSAGE = "활동 중인 방이 아닙니다";
    private static final String USER_NOT_IN_ROOM_MESSAGE = "해당 방에 참여 중인 유저가 아닙니다";
    private static final String USER_NOT_FOUND_MESSAGE = "유저정보 조회에 실패했습니다";


    private final SimpMessagingTemplate messagingTemplate;
    private final UserDetailsServiceImpl userDetailsService;
    private final RoomRepository roomRepository;
    private final JoinedRoomRepository joinedRoomRepository;
    private final GoalRepository goalRepository;
    private final UserRepository userRepository;

    /**
     * 목표 생성 및 삭제하는 메서드
     * @param roomId room ID
     * @return 목표 Response
     */
    @Transactional
    public void manageGoal(GoalListRequest goalListRequest, Long roomId, Principal principal) {
        String email = principal.getName();
        UserEntity user = userRepository.findUserByEmail(email).orElse(null);
        if (user == null) {
            log.warn("[유저 조회 실패] userId: {}", user.getUserId());
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(404, new WebsocketErrorMessage(user.getUserId(), USER_NOT_FOUND_MESSAGE)));
            return;
        }
        log.info("[목표 관리 요청] userId: {}, roomId: {}", user.getUserId(), roomId);

        RoomEntity roomEntity = roomRepository.findById(roomId).orElse(null);
        if (roomEntity == null) {
            log.warn("[목표 관리 실패] 존재하지 않는 방 - roomId: {}", roomId);
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(404, new WebsocketErrorMessage(user.getUserId(), ROOM_NOT_FOUND_MESSAGE)));
            return;
        }

        if (!roomEntity.getStatus()) {
            log.warn("[목표 관리 실패] 비활성화된 방 - roomId: {}", roomId);
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(403, new WebsocketErrorMessage(user.getUserId(), ROOM_NOT_ACTIVE_MESSAGE)));
            return;
        }

        JoinedRoomEntity joinedRoomEntity = joinedRoomRepository.findByRoom_RoomIdAndUser_UserIdAndIsParticipatingTrue(roomId, user.getUserId()).orElse(null);
        if (joinedRoomEntity == null) {
            log.warn("[목표 관리 실패] 방에 소속되지 않은 사용자 - userId: {}, roomId: {}", user.getUserId(), roomId);
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(403, new WebsocketErrorMessage(user.getUserId(), USER_NOT_IN_ROOM_MESSAGE)));
            return;
        }

        // 목표 생성
        List<GoalEntity> goalEntities = goalListRequest.getCreateGoals().stream()
                .map(goal -> GoalEntity.builder()
                        .content(goal.getContent())
                        .isCompleted(false)
                        .joinedRoom(joinedRoomEntity)
                        .build())
                .toList();
        goalRepository.saveAll(goalEntities);
        log.info("[목표 생성 완료] 생성된 목표 수: {}", goalEntities.size());

        // 목표 삭제
        for (Long deleteGoal : goalListRequest.getDeleteGoals()) {
            goalRepository.deleteById(deleteGoal);
            log.info("[목표 삭제] 삭제된 goalId: {}", deleteGoal);
        }

        // 전체 목표 조회 후 응답
        List<GoalEntity> updateGoalList = goalRepository.findAllByJoinedRoom(joinedRoomEntity);
        List<GoalResponse> goalResponseList = updateGoalList.stream()
                .map(goal -> new GoalResponse(goal.getGoalId(), goal.getContent(), goal.getIsCompleted()))
                .toList();

        messagingTemplate.convertAndSend("/sub/data/" + roomId, new GoalListResponse(WebsocketMessageType.GOAL_LIST_UPDATED, user.getUserId(), goalResponseList));
        log.info("[목표 리스트 응답 전송 완료] userId: {}, roomId: {}, 총 목표 수: {}", user.getUserId(), roomId, goalResponseList.size());
    }

    /**
     * 개별 목표에 완성 여부 버튼 누르는 메서드
     * @param goalCompleteUpdateRequest 목표 ID, 목표 완료 여부
     * @param roomId room ID
     * @return 목표 완료 여부 Response
     */
    @Transactional
    public void pressGoal(GoalCompleteUpdateRequest goalCompleteUpdateRequest, Long roomId, Principal principal) {
        String email = principal.getName();
        UserEntity user = userRepository.findUserByEmail(email).orElse(null);
        if (user == null) {
            log.warn("[유저 조회 실패] userId: {}", user.getUserId());
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(404, new WebsocketErrorMessage(user.getUserId(), USER_NOT_FOUND_MESSAGE)));
            return;
        }
        log.info("[목표 완료 토글 요청] userId: {}, roomId: {}, goalId: {}, 완료 여부: {}",
                user.getUserId(), roomId, goalCompleteUpdateRequest.getGoalId(), goalCompleteUpdateRequest.isCompleted());

        RoomEntity roomEntity = roomRepository.findById(roomId).orElse(null);
        if (roomEntity == null) {
            log.warn("[목표 토글 실패] 존재하지 않는 방 - roomId: {}", roomId);
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(404, new WebsocketErrorMessage(user.getUserId(), ROOM_NOT_FOUND_MESSAGE)));
            return;
        }

        if (!roomEntity.getStatus()) {
            log.warn("[목표 토글 실패] 비활성화된 방 - roomId: {}", roomId);
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(403, new WebsocketErrorMessage(user.getUserId(), ROOM_NOT_ACTIVE_MESSAGE)));
            return;
        }

        JoinedRoomEntity joinedRoomEntity = joinedRoomRepository.findByRoom_RoomIdAndUser_UserIdAndIsParticipatingTrue(roomId, user.getUserId()).orElse(null);
        if (joinedRoomEntity == null) {
            log.warn("[목표 토글 실패] 방에 소속되지 않은 사용자 - userId: {}, roomId: {}", user.getUserId(), roomId);
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(403, new WebsocketErrorMessage(user.getUserId(), USER_NOT_IN_ROOM_MESSAGE)));
            return;
        }

        GoalEntity goalEntity = goalRepository.findById(goalCompleteUpdateRequest.getGoalId()).orElse(null);
        if (goalEntity == null) {
            log.warn("[목표 토글 실패] 존재하지 않는 목표 - goalId: {}", goalCompleteUpdateRequest.getGoalId());
            messagingTemplate.convertAndSend("/sub/data/" + roomId, new ErrorResponse(404, new WebsocketErrorMessage(user.getUserId(), GOAL_NOT_FOUND_MESSAGE)));
            return;
        }

        goalEntity.updateIsCompleted(goalCompleteUpdateRequest.isCompleted());
        GoalEntity updatedGoalEntity = goalRepository.save(goalEntity);
        log.info("[목표 완료 상태 변경 성공] goalId: {}, isCompleted: {}", updatedGoalEntity.getGoalId(), updatedGoalEntity.getIsCompleted());

        messagingTemplate.convertAndSend("/sub/data/" + roomId, new GoalResponse(WebsocketMessageType.GOAL_COMPLETE_UPDATED, user.getUserId(), updatedGoalEntity.getGoalId(), updatedGoalEntity.getContent(), updatedGoalEntity.getIsCompleted()));
    }
}
