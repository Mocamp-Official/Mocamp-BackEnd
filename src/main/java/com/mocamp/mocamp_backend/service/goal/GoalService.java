package com.mocamp.mocamp_backend.service.goal;

import com.mocamp.mocamp_backend.authentication.UserDetailsServiceImpl;
import com.mocamp.mocamp_backend.dto.commonResponse.CommonResponse;
import com.mocamp.mocamp_backend.dto.commonResponse.ErrorResponse;
import com.mocamp.mocamp_backend.dto.commonResponse.SuccessResponse;
import com.mocamp.mocamp_backend.dto.goal.*;
import com.mocamp.mocamp_backend.entity.GoalEntity;
import com.mocamp.mocamp_backend.entity.JoinedRoomEntity;
import com.mocamp.mocamp_backend.entity.RoomEntity;
import com.mocamp.mocamp_backend.entity.UserEntity;
import com.mocamp.mocamp_backend.repository.GoalRepository;
import com.mocamp.mocamp_backend.repository.JoinedRoomRepository;
import com.mocamp.mocamp_backend.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalService {

    private static final String ROOM_NOT_FOUND_MESSAGE = "방을 찾을 수 없습니다";
    private static final String GOAL_NOT_FOUND_MESSAGE = "목표를 찾을 수 없습니다";
    private static final String ROOM_NOT_ACTIVE_MESSAGE = "활동 중인 방이 아닙니다";
    private static final String USER_NOT_IN_ROOM_MESSAGE = "해당 방에 참여 중인 유저가 아닙니다";

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
    @Transactional
    public void manageGoal(GoalListRequest goalListRequest, Long roomId) {
        UserEntity user = userDetailsService.getUserByContextHolder();

        // roomId에 해당하는 방이 존재하는지 확인
        RoomEntity roomEntity = roomRepository.findById(roomId).orElse(null);
        if (roomEntity == null) {
            messagingTemplate.convertAndSend("/sub/data/goal/" + roomId, new ErrorResponse(404, ROOM_NOT_FOUND_MESSAGE));
        }

        // 해당하는 방이 활동중인지 확인
        if (!roomEntity.getStatus()) {
            messagingTemplate.convertAndSend("/sub/data/goal/" + roomId, new ErrorResponse(403, ROOM_NOT_ACTIVE_MESSAGE));
        }

        // 해당하는 방에 소속하는 유저인지 확인
        JoinedRoomEntity joinedRoomEntity = joinedRoomRepository.findByRoom_RoomIdAndUser_UserIdAndIsParticipatingTrue(roomId, user.getUserId()).orElse(null);
        if (joinedRoomEntity == null) {
            messagingTemplate.convertAndSend("/sub/data/goal/" + roomId, new ErrorResponse(403, USER_NOT_IN_ROOM_MESSAGE));
        }

        // 생성한 목표 엔티티 생성 및 저장
        List<GoalEntity> goalEntities = goalListRequest.getCreateGoals().stream()
                .map(goal -> GoalEntity.builder()
                        .content(goal.getContent())
                        .isCompleted(false)
                        .joinedRoom(joinedRoomEntity)
                        .build())
                .toList();
        goalRepository.saveAll(goalEntities);

        // 삭제할 목표 엔티티 삭제
        for (Long deleteGoal : goalListRequest.getDeleteGoals()) {
            goalRepository.deleteById(deleteGoal);
        }

        // 웹소캣 응답으로 보낼 GoalResponse 생성
        List<GoalEntity> updateGoalList = goalRepository.findAll();
        List<GoalResponse> goalResponseList = updateGoalList.stream()
                .map(goal -> new GoalResponse(goal.getGoalId(), goal.getContent(), goal.getIsCompleted()))
                .toList();

        // WebSocket 응답 전송
        messagingTemplate.convertAndSend("/sub/data/goal/" + roomId, new GoalListResponse(goalResponseList));
    }

    /**
     * 개별 목표에 완성 여부 버튼 누르는 메서드
     * @param goalCompleteUpdateRequest 목표 ID, 목표 완료 여부
     * @param roomId room ID
     * @return 목표 완료 여부 Response
     */
    public void pressGoal(GoalCompleteUpdateRequest goalCompleteUpdateRequest, Long roomId) {
        UserEntity user = userDetailsService.getUserByContextHolder();

        // roomId에 해당하는 방이 존재하는지 확인
        RoomEntity roomEntity = roomRepository.findById(roomId).orElse(null);
        if (roomEntity == null) {
            messagingTemplate.convertAndSend("/sub/data/goal/" + roomId, new ErrorResponse(404, ROOM_NOT_FOUND_MESSAGE));
        }

        // 해당하는 방이 활동중인지 확인
        if (!roomEntity.getStatus()) {
            messagingTemplate.convertAndSend("/sub/data/goal/" + roomId, new ErrorResponse(403, ROOM_NOT_ACTIVE_MESSAGE));
        }

        // 해당하는 방에 소속하는 유저인지 확인
        JoinedRoomEntity joinedRoomEntity = joinedRoomRepository.findByRoom_RoomIdAndUser_UserIdAndIsParticipatingTrue(roomId, user.getUserId()).orElse(null);
        if (joinedRoomEntity == null) {
            messagingTemplate.convertAndSend("/sub/data/goal/" + roomId, new ErrorResponse(403, USER_NOT_IN_ROOM_MESSAGE));
        }

        // 해당 목표가 존재하는지 확인
        GoalEntity goalEntity = goalRepository.findById(goalCompleteUpdateRequest.getGoalId()).orElse(null);
        if (goalEntity == null) {
            messagingTemplate.convertAndSend("/sub/data/goal/" + roomId, new ErrorResponse(404, GOAL_NOT_FOUND_MESSAGE));
        }

        // 해당 목표의 완료 여부 변경 및 저장
        goalEntity.updateIsCompleted(goalCompleteUpdateRequest.isCompleted());
        // 리뷰 2
        goalRepository.save(goalEntity);

        // WebSocket 응답 전송
        messagingTemplate.convertAndSend("/sub/data/goal/" + roomId, new GoalResponse(goalEntity.getGoalId(), goalEntity.getContent(), goalEntity.getIsCompleted()));
    }

    /**
     * 모든 유저의 목표 조회하는 메서드
     * @param roomId room ID
     * @return 유저 목표 Response
     */
    public ResponseEntity<CommonResponse> getGoals(Long roomId) {
        UserEntity user = userDetailsService.getUserByContextHolder();

        // roomId에 해당하는 방이 존재하는지 확인
        RoomEntity roomEntity = roomRepository.findById(roomId).orElse(null);
        if (roomEntity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(404, ROOM_NOT_FOUND_MESSAGE));

        }

        // 해당하는 방이 활동중인지 확인
        if (!roomEntity.getStatus()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(403, ROOM_NOT_ACTIVE_MESSAGE));
        }

        // 해당하는 방에 소속하는 유저인지 확인
        JoinedRoomEntity joinedRoomEntity = joinedRoomRepository.findByRoom_RoomIdAndUser_UserIdAndIsParticipatingTrue(roomId, user.getUserId()).orElse(null);
        if (joinedRoomEntity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(403, USER_NOT_IN_ROOM_MESSAGE));
        }

        // 해당하는 방에 존재하는 모든 유저의 목표 반환
        List<JoinedRoomEntity> joinedRoomEntityList = joinedRoomRepository.findByRoom_RoomIdAndIsParticipatingTrue(roomId);
        List<UserGoalResponse> userGoalResponseList = joinedRoomEntityList.stream()
                .map(entity -> {
                    List<GoalResponse> goalList = entity.getGoals().stream()
                            .map(goal -> new GoalResponse(goal.getGoalId(), goal.getContent(), goal.getIsCompleted()))
                            .toList();

                    return new UserGoalResponse(entity.getUser().getUserId(), goalList);
                })
                .toList();

        return ResponseEntity.ok(new SuccessResponse(200, userGoalResponseList));
    }
}
