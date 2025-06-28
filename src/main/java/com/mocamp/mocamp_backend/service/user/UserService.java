package com.mocamp.mocamp_backend.service.user;

import com.mocamp.mocamp_backend.authentication.UserDetailsServiceImpl;
import com.mocamp.mocamp_backend.dto.commonResponse.CommonResponse;
import com.mocamp.mocamp_backend.dto.commonResponse.ErrorResponse;
import com.mocamp.mocamp_backend.dto.commonResponse.SuccessResponse;
import com.mocamp.mocamp_backend.dto.user.UserProfileResponse;
import com.mocamp.mocamp_backend.entity.JoinedRoomEntity;
import com.mocamp.mocamp_backend.entity.RoomEntity;
import com.mocamp.mocamp_backend.entity.UserEntity;
import com.mocamp.mocamp_backend.repository.JoinedRoomRepository;
import com.mocamp.mocamp_backend.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final RoomRepository roomRepository;
    private final JoinedRoomRepository joinedRoomRepository;
    private final UserDetailsServiceImpl userDetailsService;

    private static final String USER_NOT_FOUND_MESSAGE = "유저정보 조회에 실패했습니다";
    private static final String ROOM_NOT_FOUND_MESSAGE = "방 데이터 조회에 실패했습니다";
    private static final String ROOM_LIST_NOT_FOUND_MESSAGE = "방 목록 조회에 실패했습니다";

    // 모캠프 방 별 데이터 생성
    private UserRoomData makeRoomData(final RoomEntity roomEntity) {;
        return UserRoomData.builder()
                .roomId(roomEntity.getRoomId())
                .roomName(roomEntity.getRoomName())
                .startedAt(roomEntity.getStartedAt())
                .duration(roomEntity.getDuration())
                .status(roomEntity.getStatus())
                .build();
    }

    /**
     * 마이홈에 들어갈 사용자 데이터를 생성하여 반환하는 메서드
     * 모캠프 사용 추이 부분에 들어갈 시간 데이터와 목표 데이터를 모두 포함한다
     * @return 참여한 방 목록, 참여 시간, 생성한 목표 리스트를 포함한 사용자의 모든 데이터
     */
    public ResponseEntity<CommonResponse> getUserProfile() {
        UserEntity userEntity;
        List<JoinedRoomEntity> joinedRoomEntityList;
        List<UserRoomData> roomList = new ArrayList<>();
        List<UserTimeData> timeList = new ArrayList<>();
        List<UserGoalData> goalList = new ArrayList<>();
        Long totalDurationMinute = 0L;
        Long totalNumberOfGoals = 0L;

        // 유저 확인
        try {
            userEntity = userDetailsService.getUserByContextHolder();
            log.info("[마이홈 유저 조회 성공] 유저 ID: {}, 닉네임: {}", userEntity.getUserId(), userEntity.getUsername());
        } catch (Exception e) {
            log.error("[마이홈 유저 조회 실패] {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + USER_NOT_FOUND_MESSAGE));
        }

        // 모캠프 사용 이력 확인
        try {
            joinedRoomEntityList = joinedRoomRepository.findAllByUser(userEntity);
            log.info("[모캠프 목록 조회 성공] 유저 ID: {}, 닉네임: {}, 모캠프 방 개수: {}", userEntity.getUserId(), userEntity.getUsername(), joinedRoomEntityList.size());
        } catch (Exception e) {
            log.error("[모캠프 목록 조회 실패] {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + ROOM_LIST_NOT_FOUND_MESSAGE));
        }

        // 모캠프 방 별 데이터 조회
        try {
            for(JoinedRoomEntity joinedRoomEntity : joinedRoomEntityList) {
                RoomEntity roomEntity;
                try {
                    roomEntity = roomRepository.findById(joinedRoomEntity.getJoinedRoomId()).orElseThrow();
                } catch (Exception e) {
                    log.error("[방 조회 실패] joinedRoomId : {} \n message : {}", joinedRoomEntity.getJoinedRoomId(), e.getMessage(), e);
                    throw new RuntimeException();
                }

                UserRoomData userRoomData = makeRoomData(roomEntity);
                roomList.add(userRoomData);

                UserTimeData userTimeData = UserTimeData.builder()
                        .date(roomEntity.getEndedAt().getMonth().toString() + "." + roomEntity.getEndedAt().getDayOfMonth())
                        .duration((long) (roomEntity.getDuration().getHour() * 60 + roomEntity.getDuration().getMinute()))
                        .build();
                timeList.add(userTimeData);
                totalDurationMinute += userTimeData.getDuration();

                UserGoalData userGoalData = UserGoalData.builder()
                        .date(roomEntity.getEndedAt().getMonth().toString() + "." + roomEntity.getEndedAt().getDayOfMonth())
                        .amount((long) joinedRoomEntity.getGoals().size())
                        .build();
                goalList.add(userGoalData);
                totalNumberOfGoals += userGoalData.getAmount();

                log.info("[방 데이터 조회 성공] 유저 닉네임: {}, 방 이름: {}", joinedRoomEntity.getUser().getUsername(), roomEntity.getRoomName());
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + ROOM_NOT_FOUND_MESSAGE));
        }

        // 날짜별 duration 합산
        List<UserTimeData> timeListResult = timeList.stream()
                .collect(Collectors.groupingBy(
                        UserTimeData::getDate,
                        Collectors.summingLong(UserTimeData::getDuration)
                ))
                .entrySet()
                .stream()
                .map(entry -> new UserTimeData(entry.getKey(), entry.getValue()))
                .toList();

        // 날짜별 goal amount 합산
        List<UserGoalData> goalListResult = goalList.stream()
                .collect(Collectors.groupingBy(
                        UserGoalData::getDate,
                        Collectors.summingLong(UserGoalData::getAmount)
                ))
                .entrySet()
                .stream()
                .map(entry -> new UserGoalData(entry.getKey(), entry.getValue()))
                .toList();

        // 응답 객체 생성
        UserProfileResponse userProfileResponse = UserProfileResponse.builder()
                .userId(userEntity.getUserId())
                .username(userEntity.getUsername())
                .imagePath(userEntity.getImage().getPath())
                .totalDurationMinute(totalDurationMinute)
                .totalNumberOfGoals(totalNumberOfGoals)
                .roomList(roomList)
                .timeList(timeListResult)
                .goalList(goalListResult)
                .build();

        return ResponseEntity.ok(new SuccessResponse(200, userProfileResponse));
    }

    /**
     * 마이홈에서 세부 목표 확인 기능을 선택하면 해당 방에서 생성한 목표 기록을 반환하는 메서드
     * @return 목표 데이터의 ID, 내용, 달성 여부를 포함한 목표 리스트 반환
     */
    public ResponseEntity<CommonResponse> getUserGoals(final Long roomId) {
        UserEntity userEntity;
        JoinedRoomEntity joinedRoomEntity;

        // 유저 확인
        try {
            userEntity = userDetailsService.getUserByContextHolder();
            log.info("[마이홈 유저 조회 성공] 유저 ID: {}, 닉네임: {}", userEntity.getUserId(), userEntity.getUsername());
        } catch (Exception e) {
            log.error("[마이홈 유저 조회 실패] {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + USER_NOT_FOUND_MESSAGE));
        }

        // ID에 맞는 방 데이터 조회
        try {
            joinedRoomEntity = joinedRoomRepository.findByUserAndRoom_RoomId(userEntity, roomId);
            log.info("[모캠프 목록 조회 성공] 유저 ID: {}, 닉네임: {}, 목표 개수: {}", userEntity.getUserId(), userEntity.getUsername(), joinedRoomEntity.getGoals().size());
        } catch (Exception e) {
            log.error("[모캠프 목록 조회 실패] {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + ROOM_LIST_NOT_FOUND_MESSAGE));
        }

        List<GoalListData> goalListData = joinedRoomEntity.getGoals().stream()
                .map(entry -> new GoalListData(entry.getGoalId(), entry.getContent(), entry.getIsCompleted()))
                .toList();
        Map<String, Object> goalListMap = new HashMap<>();
        goalListMap.put("userId", userEntity.getUserId());
        goalListMap.put("username", userEntity.getUsername());
        goalListMap.put("roomId", roomId);
        goalListMap.put("dataList", goalListData);

        return ResponseEntity.ok(new SuccessResponse(200, goalListMap));
    }

    /**
     * 로그아웃 메서드
     * 추후 고도화 예정
     */
    public ResponseEntity<CommonResponse> logout() {
        UserEntity userEntity;
        List<JoinedRoomEntity> joinedRoomEntityList;

        // 유저 확인
        try {
            userEntity = userDetailsService.getUserByContextHolder();
            log.info("[마이홈 유저 조회 성공] 유저 ID: {}, 닉네임: {}", userEntity.getUserId(), userEntity.getUsername());
        } catch (Exception e) {
            log.error("[마이홈 유저 조회 실패] {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + USER_NOT_FOUND_MESSAGE));
        }

        return ResponseEntity.ok(new SuccessResponse(200, true));
    }
}
