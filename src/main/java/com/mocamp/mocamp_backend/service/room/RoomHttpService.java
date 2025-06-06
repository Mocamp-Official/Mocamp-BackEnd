package com.mocamp.mocamp_backend.service.room;

import com.mocamp.mocamp_backend.authentication.UserDetailsServiceImpl;
import com.mocamp.mocamp_backend.dto.commonResponse.CommonResponse;
import com.mocamp.mocamp_backend.dto.commonResponse.ErrorResponse;
import com.mocamp.mocamp_backend.dto.commonResponse.SuccessResponse;
import com.mocamp.mocamp_backend.dto.room.*;
import com.mocamp.mocamp_backend.entity.*;
import com.mocamp.mocamp_backend.repository.ImageRepository;
import com.mocamp.mocamp_backend.repository.JoinedRoomRepository;
import com.mocamp.mocamp_backend.repository.RoomRepository;
import com.mocamp.mocamp_backend.repository.UserRepository;
import com.mocamp.mocamp_backend.service.image.ImageType;
import com.mocamp.mocamp_backend.service.s3.S3Uploader;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;


@Service
@RequiredArgsConstructor
@Slf4j
public class RoomHttpService {
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ImageRepository imageRepository;
    private final JoinedRoomRepository joinedRoomRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final S3Uploader s3Uploader;

    @Value("${cloud.aws.s3.bucket}")
    private String DirName;
    private static final String USER_NOT_FOUND_MESSAGE = "유저정보 조회에 실패했습니다";
    private static final String ROOM_CREATION_MESSAGE = "방 생성에 실패했습니다";
    private static final String IMAGE_SAVING_MESSAGE = "이미지 저장에 실패했습니다";
    private static final String JOINED_ROOM_CREATION_MESSAGE = "방 입장정보 저장에 실패했습니다";
    private static final String ROOM_NOT_FOUND_MESSAGE = "방 정보 조회에 실패했습니다";
    private static final String ROOM_NOT_EXISTING_MESSAGE = "현재 활동 중인 방이 아닙니다";
    private static final String ROOM_ALREADY_FULL_MESSAGE = "입장 가능한 인원이 초과되었습니다";
    private static final String ROOM_NOT_ACTIVE_MESSAGE = "활동 중인 방이 아닙니다";
    private static final String USER_NOT_IN_ROOM_MESSAGE = "해당 방에 참여 중인 유저가 아닙니다";


    private String createRandomRoomSeq() {
        Random random = new Random();
        String seqCharPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        int seqLength = 8;
        StringBuilder sb = new StringBuilder(seqLength);

        for (int i = 0; i < seqLength; i++) {
            int index = random.nextInt(seqCharPool.length());
            sb.append(seqCharPool.charAt(index));
        }
        return sb.toString();
    }

    /**
     * 방 생성 메서드
     * 방 생성에 필요한 정보를 수신하여 방을 생성하는 메서드
     * @param roomCreateRequest 방 이름, 최대 인원수, 설명, 소요 시간, 대표 이미지 path, 방 전체 마이크 사용 여부, 방장의 캠/마이크 사용 여부
     * @return 생성된 방의 메타 정보 (Id 포함)
     */
    @Transactional
    public ResponseEntity<CommonResponse> createRoom(RoomCreateRequest roomCreateRequest, MultipartFile imageFile) {
        log.info("[방 생성 시작] 요청 roomCreateRequest: {}, 이미지파일: {}", roomCreateRequest, imageFile != null ? imageFile.getOriginalFilename() : "null");
        UserEntity userEntity;
        RoomEntity roomEntity;
        JoinedRoomEntity joinedRoomEntity;
        ImageEntity imageEntity;
        String imagePath = null;

        try {
            userEntity = userDetailsService.getUserByContextHolder();
            log.info("[유저 조회 성공] 유저 ID: {}, 닉네임: {}", userEntity.getUserId(), userEntity.getUsername());
        } catch (Exception e) {
            log.error("[유저 조회 실패] {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + USER_NOT_FOUND_MESSAGE));
        }

        try {
            imagePath = s3Uploader.uploadImage(imageFile, DirName);
            log.info("[이미지 업로드 성공] 경로: {}", imagePath);

            imageEntity = ImageEntity.builder()
                    .type(ImageType.room)
                    .path(imagePath)
                    .build();
            imageEntity = imageRepository.save(imageEntity);
            log.info("[이미지 저장 성공] 이미지 ID: {}", imageEntity.getImageId());
        } catch (Exception e) {
            log.error("[이미지 저장 실패] {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + IMAGE_SAVING_MESSAGE));
        }

        try {
            LocalDateTime startedAt = LocalDateTime.now();
            LocalTime duration = LocalTime.parse(roomCreateRequest.getDuration(), DateTimeFormatter.ofPattern("HH:mm"));
            LocalDateTime endedAt = startedAt.plusHours(duration.getHour()).plusMinutes(duration.getMinute());

            roomEntity = RoomEntity.builder()
                    .roomName(roomCreateRequest.getRoomName())
                    .roomSeq(createRandomRoomSeq())
                    .capacity(roomCreateRequest.getCapacity())
                    .roomNum(1)
                    .status(true)
                    .startedAt(startedAt)
                    .endedAt(endedAt)
                    .duration(duration)
                    .image(imageEntity)
                    .build();
            roomEntity = roomRepository.save(roomEntity);   // Id 포함한 객체로 재할당
            log.info("[방 저장 성공] 방 ID: {}, 방 이름: {}", roomEntity.getRoomId(), roomEntity.getRoomName());
        } catch (Exception e) {
            log.error("[방 저장 실패] {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + ROOM_CREATION_MESSAGE));
        }

        try {
            joinedRoomEntity = JoinedRoomEntity.builder()
                    .user(userEntity)
                    .room(roomEntity)
                    .isAdmin(true)
                    .isParticipating(true)
                    .build();
            joinedRoomEntity = joinedRoomRepository.save(joinedRoomEntity);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + JOINED_ROOM_CREATION_MESSAGE));

        }

        return ResponseEntity.ok(new SuccessResponse(200, "방 생성이 완료되었습니다."));
    }

    /**
     * 방 입장 메서드
     * SNS 안내 링크를 통해 들어온 유저가 방에 참여하도록 하는 메서드
     * @param roomId 참여하고자 하는 방의 Id
     * @param roomEnterRequest 해당 사용자의 캠/마이크의 사용 여부
     * @return 생성된 방의 메타 정보 (Id 포함)
     */
    @Transactional
    public ResponseEntity<CommonResponse> enterRoom(Long roomId, RoomEnterRequest roomEnterRequest) {
        UserEntity userEntity;
        RoomEntity roomEntity;
        JoinedRoomEntity joinedRoomEntity;

        log.info("[입장 요청] roomId: {}, 요청 캠: {}, 마이크: {}", roomId, roomEnterRequest.getCamTurnedOn(), roomEnterRequest.getMicTurnedOn());

        // 유저 검증
        try {
            userEntity = userDetailsService.getUserByContextHolder();
            log.info("[유저 확인 완료] userId: {}", userEntity.getUserId());
        } catch (Exception e) {
            log.error("[유저 확인 실패] {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + USER_NOT_FOUND_MESSAGE));
        }

        // roomId 유효성 검증
        Optional<RoomEntity> optionalRoomEntity = roomRepository.findById(roomId);
        if (optionalRoomEntity.isEmpty()) {
            log.error("[방 조회 실패] roomId: {} 존재하지 않음", roomId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + ROOM_NOT_FOUND_MESSAGE));
        }
        roomEntity = optionalRoomEntity.get();
        log.info("[방 조회 성공] roomId: {}, roomName: {}", roomEntity.getRoomId(), roomEntity.getRoomName());

        // room 입장 가능 여부 확인
        if (roomEntity.getIsDeleted() || !roomEntity.getStatus()) {
            log.warn("[입장 불가] 삭제되었거나 비활성화된 방입니다. roomId: {}", roomId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + ROOM_NOT_EXISTING_MESSAGE));
        }

        // 재입장 여부 확인
        if (joinedRoomRepository.existsByRoomAndUser(roomEntity, userEntity)) {
            // 재입장 처리
            log.info("[재입장 요청] userId: {}, roomId: {}", userEntity.getUserId(), roomId);
            joinedRoomEntity = joinedRoomRepository.findByRoomAndUser(roomEntity, userEntity).orElse(null);
            JoinedRoomEntity newJoinedRoomEntity = JoinedRoomEntity.builder()
                    .joinedRoomId(joinedRoomEntity.getJoinedRoomId())
                    .user(userEntity)
                    .room(roomEntity)
                    .isAdmin(joinedRoomEntity.getIsAdmin())
                    .isParticipating(true)
                    .isDeleted(false)
                    .build();
            joinedRoomRepository.save(newJoinedRoomEntity);
            roomEntity.updateRoomNum(roomEntity.getRoomNum() + 1);
            roomRepository.save(roomEntity);

            log.info("[재입장 완료] userId: {}, roomId: {}, 현재 인원 수: {}", userEntity.getUserId(), roomId, roomEntity.getRoomNum());
            return ResponseEntity.ok(new SuccessResponse(200, "재입장이 완료되었습니다"));
        } else {
            // 신규 입장
            if (roomEntity.getRoomNum() >= roomEntity.getCapacity()) {
                log.warn("[입장 실패] 방 정원 초과. roomId: {}, 현재 인원: {}, 최대 인원: {}",
                        roomId, roomEntity.getRoomNum(), roomEntity.getCapacity());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ErrorResponse(403, "에러 메시지: " + ROOM_ALREADY_FULL_MESSAGE));
            }

            log.info("[신규 입장 요청] userId: {}, roomId: {}", userEntity.getUserId(), roomId);
            joinedRoomEntity = JoinedRoomEntity.builder()
                    .user(userEntity)
                    .room(roomEntity)
                    .isAdmin(false)
                    .isParticipating(true)
                    .build();
            joinedRoomRepository.save(joinedRoomEntity);
            roomEntity.updateRoomNum(roomEntity.getRoomNum() + 1);
            roomRepository.save(roomEntity);

            log.info("[신규 입장 완료] userId: {}, roomId: {}, 현재 인원 수: {}", userEntity.getUserId(), roomId, roomEntity.getRoomNum());
            return ResponseEntity.ok(new SuccessResponse(200, "입장이 완료되었습니다"));
        }
    }

    /**
     * 방장 변경 메서드
     * @param roomId 참여하고자 하는 방의 Id
     * @param nextAdminId 방장 변경 시 다음으로 지정될 방장의 Id
     * @return 성공 메시지
     */
    @Transactional
    public ResponseEntity<CommonResponse> exitRoom(Long roomId, Long nextAdminId) {
        UserEntity userEntity, newAdminEntity;
        RoomEntity roomEntity;
        JoinedRoomEntity currentAdminRoomEntity, newAdminRoomEntity;

        // 유저 검증
        try {
            userEntity = userDetailsService.getUserByContextHolder();
            newAdminEntity = userRepository.findUserByUserId(nextAdminId).orElseThrow();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + USER_NOT_FOUND_MESSAGE));
        }

        // roomId 검증
        Optional<RoomEntity> optionalRoomEntity = roomRepository.findById(roomId);
        if(optionalRoomEntity.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + ROOM_NOT_FOUND_MESSAGE));
        roomEntity = optionalRoomEntity.get();

        // 마지막 참가자 퇴장 (모캠프 종료)
        // status 변경, 사용시간 계산 후 폐쇄 조치 (isDeleted = true)
        if(roomEntity.getRoomNum() <= 1) {
            roomEntity.setStatus(false);
            roomEntity.setEndedAt(LocalDateTime.now());
            roomEntity.setIsDeleted(true);

            Duration duration = Duration.between(roomEntity.getEndedAt(), roomEntity.getStartedAt());
            roomEntity.setDuration(LocalTime.ofSecondOfDay(duration.getSeconds()));    // 종료시각 - 시작시각으로 설정

            currentAdminRoomEntity = joinedRoomRepository.findByRoomAndUser(roomEntity, userEntity).orElse(null);
            currentAdminRoomEntity.setIsDeleted(true);
            roomEntity.setRoomNum(roomEntity.getRoomNum() - 1);

            joinedRoomRepository.save(currentAdminRoomEntity);
            roomRepository.save(roomEntity);

            return ResponseEntity.ok(new SuccessResponse(200, "퇴장 성공(모캠프 종료)"));
        }

        // 단순 퇴장 (다른 사용자는 남아있음)
        currentAdminRoomEntity = joinedRoomRepository.findByRoomAndUser(roomEntity, userEntity).orElse(null);
        if(currentAdminRoomEntity.getIsAdmin()) {
            currentAdminRoomEntity.setIsAdmin(false);
        }

        newAdminRoomEntity = joinedRoomRepository.findByRoomAndUser(roomEntity, newAdminEntity).orElse(null);
        newAdminRoomEntity.setIsAdmin(true);

        roomEntity.setRoomNum(roomEntity.getRoomNum() - 1);

        joinedRoomRepository.save(currentAdminRoomEntity);
        joinedRoomRepository.save(newAdminRoomEntity);
        roomRepository.save(roomEntity);

        return ResponseEntity.ok(new SuccessResponse(200, "퇴장 성공"));
    }

    /**
     * 방 정보 수정 메서드
     * @param roomId 참여하고자 하는 방의 Id
     * @param data 적용할 데이터
     * @return 성공 메시지
     */
    @Transactional
    public ResponseEntity<CommonResponse> modifyRoomData(Long roomId, Object data) {
        // roomId 확인
        // 유효한 방이면 인풋 바탕으로 정보 수정
        // 추후 필요 시 개발 예정
        return null;
    }

    /**
     * 방 데이터 조회 메서드
     * @param roomId 참여하고자 하는 방의 Id
     * @return 자신을 포함하여 현재 방에 속해 있는 멤버들의 데이터 전체
     */
    public ResponseEntity<CommonResponse> getRoomData(Long roomId) {
        UserEntity userEntity;
        RoomEntity roomEntity;

        log.info("[방 데이터 조회 요청] roomId: {}", roomId);

        // 유저 검증
        try {
            userEntity = userDetailsService.getUserByContextHolder();
            log.info("[유저 인증 완료] userId: {}", userEntity.getUserId());
        } catch (Exception e) {
            log.error("[유저 인증 실패] {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + USER_NOT_FOUND_MESSAGE));
        }

        // roomId 검증
        Optional<RoomEntity> optionalRoomEntity = roomRepository.findById(roomId);
        if (optionalRoomEntity.isEmpty()) {
            log.warn("[방 조회 실패] 존재하지 않는 roomId: {}", roomId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + ROOM_NOT_FOUND_MESSAGE));
        }
        roomEntity = optionalRoomEntity.get();
        log.info("[방 조회 성공] roomId: {}, roomName: {}", roomEntity.getRoomId(), roomEntity.getRoomName());

        return ResponseEntity.ok(new SuccessResponse(200, RoomDataResponse.convertEntityToDTO(roomEntity)));
    }

    /**
     * 방 참가자 정보 조회 메서드
     * @param roomId 참여하고자 하는 방의 Id
     * @return 자신을 포함하여 현재 방에 속해 있는 참가자들의 데이터
     */
    public ResponseEntity<CommonResponse> getRoomParticipantData(Long roomId) {
        log.info("[방 참가자 데이터 조회 요청] roomId: {}", roomId);

        UserEntity user;
        try {
            user = userDetailsService.getUserByContextHolder();
            log.info("[유저 인증 완료] userId: {}", user.getUserId());
        } catch (Exception e) {
            log.error("[유저 인증 실패] {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + USER_NOT_FOUND_MESSAGE));
        }

        // roomId에 해당하는 방이 존재하는지 확인
        RoomEntity roomEntity = roomRepository.findById(roomId).orElse(null);
        if (roomEntity == null) {
            log.warn("[방 조회 실패] 존재하지 않는 roomId: {}", roomId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(404, ROOM_NOT_FOUND_MESSAGE));
        }

        // 해당하는 방이 활동중인지 확인
        if (!roomEntity.getStatus()) {
            log.warn("[비활성 방] roomId: {}", roomId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(403, ROOM_NOT_ACTIVE_MESSAGE));
        }

        // 해당 방에 현재 유저가 참여 중인지 확인
        JoinedRoomEntity joinedRoomEntity = joinedRoomRepository
                .findByRoom_RoomIdAndUser_UserIdAndIsParticipatingTrue(roomId, user.getUserId())
                .orElse(null);
        if (joinedRoomEntity == null) {
            log.warn("[참여 중 아님] userId: {} 는 roomId: {} 에 참여하지 않음", user.getUserId(), roomId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(403, USER_NOT_IN_ROOM_MESSAGE));
        }

        // 현재 방에 참여 중인 유저 목록 조회
        List<JoinedRoomEntity> joinedRoomEntityList =
                joinedRoomRepository.findByRoom_RoomIdAndIsParticipatingTrue(roomId);
        log.info("[방 참가자 수] roomId: {}, 참여 인원 수: {}", roomId, joinedRoomEntityList.size());

        List<RoomParticipantResponse> roomParticipantResponseList = new ArrayList<>();

        for (JoinedRoomEntity joinedRoom : joinedRoomEntityList) {
            RoomParticipantResponse roomParticipantResponse = RoomParticipantResponse.builder()
                    .userId(joinedRoom.getUser().getUserId())
                    .userSeq(joinedRoom.getUser().getUserSeq())
                    .username(joinedRoom.getUser().getUsername())
                    .build();

            List<String> goalContentList = new ArrayList<>();
            List<GoalEntity> goalEntityList = joinedRoom.getGoals();
            for (GoalEntity goal : goalEntityList) {
                goalContentList.add(goal.getContent());
            }

            roomParticipantResponse.setGoalList(goalContentList);
            roomParticipantResponseList.add(roomParticipantResponse);
        }

        log.info("[참가자 응답 완료] 응답 인원 수: {}", roomParticipantResponseList.size());
        return ResponseEntity.ok(new SuccessResponse(200, roomParticipantResponseList));
    }
}
