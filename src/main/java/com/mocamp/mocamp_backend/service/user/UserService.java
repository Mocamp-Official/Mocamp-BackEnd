package com.mocamp.mocamp_backend.service.user;

import com.mocamp.mocamp_backend.authentication.UserDetailsServiceImpl;
import com.mocamp.mocamp_backend.dto.commonResponse.CommonResponse;
import com.mocamp.mocamp_backend.dto.commonResponse.ErrorResponse;
import com.mocamp.mocamp_backend.dto.commonResponse.SuccessResponse;
import com.mocamp.mocamp_backend.dto.user.UserProfileResponse;
import com.mocamp.mocamp_backend.entity.*;
import com.mocamp.mocamp_backend.repository.ImageRepository;
import com.mocamp.mocamp_backend.repository.JoinedRoomRepository;
import com.mocamp.mocamp_backend.repository.RoomRepository;
import com.mocamp.mocamp_backend.repository.UserRepository;
import com.mocamp.mocamp_backend.service.image.ImageType;
import com.mocamp.mocamp_backend.service.s3.S3Uploader;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final String USER_NOT_FOUND_MESSAGE = "유저정보 조회에 실패했습니다";
    private static final String ROOM_NOT_FOUND_MESSAGE = "방 데이터 조회에 실패했습니다";
    private static final String ROOM_LIST_NOT_FOUND_MESSAGE = "방 목록 조회에 실패했습니다";
    private static final String IMAGE_SAVING_MESSAGE = "이미지 저장에 실패했습니다";

    @Value("${cloud.aws.s3.bucket}")
    private String DirName;
    private final RoomRepository roomRepository;
    private final ImageRepository imageRepository;
    private final JoinedRoomRepository joinedRoomRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserRepository userRepository;
    private final S3Uploader s3Uploader;

    // 모캠프 방 별 데이터 생성
    private UserRoomData makeRoomData(final RoomEntity roomEntity, final List<GoalEntity> goalList) {;
        List<GoalListData> goalDataList = goalList.stream().map(
                entry -> new GoalListData(entry.getGoalId(), entry.getContent(), entry.getIsCompleted())
        ).toList();

        return UserRoomData.builder()
                .roomId(roomEntity.getRoomId())
                .roomName(roomEntity.getRoomName())
                .startedAt(roomEntity.getStartedAt())
                .duration(roomEntity.getDuration())
                .status(roomEntity.getStatus())
                .userGoalList(goalDataList)
                .build();
    }

    // 날짜별 모캠프 사용 시간 데이터 생성
    private List<UserTimeData> makeTimeData(final List<UserTimeData> timeList) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M.d");

        return timeList.stream()
                .collect(Collectors.groupingBy(
                        UserTimeData::getDate,
                        Collectors.toList()
                ))
                .entrySet()
                .stream()
                .map(entry -> {
                    String date = entry.getKey();
                    List<UserTimeData> dataList = entry.getValue();

                    // 날짜별 duration 합산
                    Long totalDuration = dataList.stream()
                            .mapToLong(UserTimeData::getDuration)
                            .sum();

                    // 날짜별 goalList 이어붙이기
                    List<GoalListData> mergedGoalList = dataList.stream()
                            .flatMap(data -> data.getUserGoalList().stream())
                            .collect(Collectors.toList());

                    return UserTimeData.builder()
                            .date(date)
                            .duration(totalDuration)
                            .userGoalList(mergedGoalList)
                            .build();
                })
                .sorted(Comparator.comparing(
                        data -> LocalDate.parse(data.getDate(), formatter).withYear(LocalDate.now().getYear())
                )).toList();
    }

    // 날짜별 목표 수행 데이터 생성
    private List<UserGoalData> makeGoalData(final List<UserGoalData> goalList) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M.d");

        return goalList.stream()
                .collect(Collectors.groupingBy(
                        UserGoalData::getDate,
                        Collectors.toList()
                ))
                .entrySet()
                .stream()
                .map(entry -> {
                    String date = entry.getKey();
                    List<UserGoalData> dataList = entry.getValue();

                    // 날짜별 amount 합산
                    Long totalAmount = dataList.stream()
                            .mapToLong(UserGoalData::getAmount)
                            .sum();

                    // 날짜별 goalList 이어붙이기
                    List<GoalListData> mergedGoalList = dataList.stream()
                            .flatMap(data -> data.getUserGoalList().stream())
                            .collect(Collectors.toList());

                    return UserGoalData.builder()
                            .date(date)
                            .amount(totalAmount)
                            .userGoalList(mergedGoalList)
                            .build();
                })
                .sorted(Comparator.comparing(
                        data -> LocalDate.parse(data.getDate(), formatter).withYear(LocalDate.now().getYear())
                )).toList();
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
                    roomEntity = roomRepository.findById(joinedRoomEntity.getRoom().getRoomId()).orElseThrow();
                } catch (Exception e) {
                    log.error("[방 조회 실패] joinedRoomId : {} \n message : {}", joinedRoomEntity.getJoinedRoomId(), e.getMessage(), e);
                    throw new RuntimeException();
                }

                List<GoalListData> goalDataList = joinedRoomEntity.getGoals().stream()
                        .map(entry -> new GoalListData(entry.getGoalId(), entry.getContent(), entry.getIsCompleted()))
                        .toList();

                UserRoomData userRoomData = makeRoomData(roomEntity, joinedRoomEntity.getGoals());
                roomList.add(userRoomData);

                UserTimeData userTimeData = UserTimeData.builder()
                        .date(roomEntity.getEndedAt().getMonth().getValue() + "." + roomEntity.getEndedAt().getDayOfMonth())
                        .duration((long) (roomEntity.getDuration().getHour() * 60 + roomEntity.getDuration().getMinute()))
                        .userGoalList(goalDataList)
                        .build();
                timeList.add(userTimeData);
                totalDurationMinute += userTimeData.getDuration();

                UserGoalData userGoalData = UserGoalData.builder()
                        .date(roomEntity.getEndedAt().getMonth().getValue() + "." + roomEntity.getEndedAt().getDayOfMonth())
                        .amount((long) goalDataList.size())
                        .userGoalList(goalDataList)
                        .build();
                goalList.add(userGoalData);
                totalNumberOfGoals += userGoalData.getAmount();

                log.info("[방 데이터 조회 성공] 유저 닉네임: {}, 방 이름: {}", joinedRoomEntity.getUser().getUsername(), roomEntity.getRoomName());
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + ROOM_NOT_FOUND_MESSAGE));
        }

        List<UserRoomData> roomListResult = roomList.stream()
                .sorted(Comparator.comparing(UserRoomData::getStartedAt).reversed())
                .toList();;
        List<UserTimeData> timeListResult = makeTimeData(timeList);
        List<UserGoalData> goalListResult = makeGoalData(goalList);

        // 응답 객체 생성
        UserProfileResponse userProfileResponse = UserProfileResponse.builder()
                .userId(userEntity.getUserId())
                .username(userEntity.getUsername())
                .imagePath(userEntity.getImage().getPath())
                .totalDurationMinute(totalDurationMinute)
                .totalNumberOfGoals(totalNumberOfGoals)
                .roomList(roomListResult)
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
                .map(entry -> GoalListData.builder()
                        .goalId(entry.getGoalId())
                        .content(entry.getContent())
                        .isCompleted(entry.getIsCompleted()).build())
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
     * 현재 참가하고 있는 모든 모캠프 방에서 자동으로 나가지도록 처리한다.
     * @return 로그아웃 완료 메시지
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

        try {
            joinedRoomEntityList = joinedRoomRepository.findAllByUser(userEntity);
            log.info("[마이홈 방 목록 조회 성공] 유저 ID: {}, 닉네임: {}", userEntity.getUserId(), joinedRoomEntityList.get(0).getUser().getUsername());
        } catch (Exception e) {
            log.error("[마이홈 방 목록 조회 실패] {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + ROOM_LIST_NOT_FOUND_MESSAGE));
        }

        List<JoinedRoomEntity> updatedRoomEntityList = joinedRoomEntityList.stream()
            .map(entity -> {
                entity.setIsParticipating(false);
                return entity;
            }).toList();
        joinedRoomRepository.saveAll(updatedRoomEntityList);

        return ResponseEntity.ok(new SuccessResponse(200, "로그아웃 되었습니다."));
    }

    /**
     * 유저 프로필 정보 수정하는 메서드
     * @return 프로필 변경 완료 메시지
     */
    @Transactional
    public ResponseEntity<CommonResponse> modifyUserProfile(String username, MultipartFile imageFile) {
        UserEntity userEntity;
        String imagePath = null;

        // 유저 확인
        try {
            userEntity = userDetailsService.getUserByContextHolder();
            log.info("[마이홈 유저 조회 성공] 유저 ID: {}, 닉네임: {}", userEntity.getUserId(), userEntity.getUsername());
        } catch (Exception e) {
            log.error("[마이홈 유저 조회 실패] {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + USER_NOT_FOUND_MESSAGE));
        }

        // 프로필 변경 사항에 유저 이름 변경이 있을 때, 유저 변경 및 저장
        if(username != null) {
            userEntity.updateUsername(username);
            UserEntity updatedUserEntity = userRepository.save(userEntity);
            log.info("[프로필 이름 변경 완료] - 변경된 닉네임: {}", updatedUserEntity.getUsername());
        }

        // 프로필 변경 사항에 유저 프로필 사진 변경이 있을 때, 유저 프로필 사진 변경 및 저장
        if(imageFile != null && !imageFile.isEmpty()) {
            try {
                imagePath = s3Uploader.uploadImage(imageFile, DirName);
                log.info("[이미지 업로드 성공] 경로: {}", imagePath);

                ImageEntity imageEntity = null;

                // 기존 이미지 엔티티가 존재할 경우
                if (userEntity.getImage() != null) {
                    imageEntity = imageRepository.findById(userEntity.getImage().getImageId()).orElse(null);

                    if (imageEntity != null) {
                        imageEntity.updatePath(imagePath);
                        imageRepository.save(imageEntity);
                        log.info("[이미지 경로 업데이트 완료] 이미지 ID: {}", imageEntity.getImageId());
                    }
                }

                // 기존 이미지가 없거나 못 찾은 경우 새로 생성
                if(imageEntity == null) {
                    imageEntity = ImageEntity.builder()
                            .type(ImageType.profile)
                            .path(imagePath)
                            .build();
                    ImageEntity newImageEntity = imageRepository.save(imageEntity);
                    userEntity.setImage(newImageEntity);
                    log.info("[새 이미지 엔티티 저장 및 연결] 이미지 ID: {}", newImageEntity.getImageId());
                }

            } catch (Exception e) {
                log.error("[이미지 저장 실패] {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ErrorResponse(403, "에러 메시지: " + IMAGE_SAVING_MESSAGE));
            }
        }

        return ResponseEntity.ok(new SuccessResponse(200, "프로필 변경이 완료되었습니다"));
    }
}
