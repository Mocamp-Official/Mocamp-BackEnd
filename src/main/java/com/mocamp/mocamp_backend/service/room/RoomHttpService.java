package com.mocamp.mocamp_backend.service.room;

import com.mocamp.mocamp_backend.authentication.UserDetailsServiceImpl;
import com.mocamp.mocamp_backend.dto.commonResponse.CommonResponse;
import com.mocamp.mocamp_backend.dto.commonResponse.ErrorResponse;
import com.mocamp.mocamp_backend.dto.commonResponse.SuccessResponse;
import com.mocamp.mocamp_backend.dto.room.RoomCreateRequest;
import com.mocamp.mocamp_backend.dto.room.RoomEnterRequest;
import com.mocamp.mocamp_backend.dto.room.RoomResponse;
import com.mocamp.mocamp_backend.entity.ImageEntity;
import com.mocamp.mocamp_backend.entity.JoinedRoomEntity;
import com.mocamp.mocamp_backend.entity.RoomEntity;
import com.mocamp.mocamp_backend.entity.UserEntity;
import com.mocamp.mocamp_backend.repository.ImageRepository;
import com.mocamp.mocamp_backend.repository.JoinedRoomRepository;
import com.mocamp.mocamp_backend.repository.RoomRepository;
import com.mocamp.mocamp_backend.repository.UserRepository;
import com.mocamp.mocamp_backend.service.image.ImageType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Random;


@Service
@RequiredArgsConstructor
public class RoomHttpService {
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ImageRepository imageRepository;
    private final JoinedRoomRepository joinedRoomRepository;
    private final UserDetailsServiceImpl userDetailsService;

    private static final String USER_NOT_FOUND_MESSAGE = "유저정보 조회에 실패했습니다";
    private static final String ROOM_CREATION_MESSAGE = "방 생성에 실패했습니다";
    private static final String IMAGE_SAVING_MESSAGE = "이미지 저장에 실패했습니다";
    private static final String JOINED_ROOM_CREATION_MESSAGE = "방 입장정보 저장에 실패했습니다";
    private static final String ROOM_NOT_FOUND_MESSAGE = "방 정보 조회에 실패했습니다";
    private static final String ROOM_NOT_EXISTING_MESSAGE = "현재 활동 중인 방이 아닙니다";
    private static final String ROOM_ALREADY_FULL_MESSAGE = "입장 가능한 인원이 초과되었습니다.";
    private static final String ROOM_ALREADY_MESSAGE = "";

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
    public ResponseEntity<CommonResponse> createRoom(RoomCreateRequest roomCreateRequest) {
        UserEntity userEntity;
        RoomEntity roomEntity;
        JoinedRoomEntity joinedRoomEntity;
        ImageEntity imageEntity;

        try {
            userEntity = userDetailsService.getUserByContextHolder();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + USER_NOT_FOUND_MESSAGE));
        }

        try {
            imageEntity = ImageEntity.builder()
                    .type(ImageType.room)
                    .path(roomCreateRequest.getImagePath())
                    .build();
            imageEntity = imageRepository.save(imageEntity);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + IMAGE_SAVING_MESSAGE));
        }

        try {
            roomEntity = RoomEntity.builder()
                    .roomName(roomCreateRequest.getRoomName())
                    .roomSeq(createRandomRoomSeq())
                    .capacity(roomCreateRequest.getCapacity())
                    .roomNum(1)
                    .status(true)
                    .startedAt(LocalDateTime.now())
                    .endedAt(LocalDateTime.now())
                    .duration(LocalTime.parse(roomCreateRequest.getDuration(), DateTimeFormatter.ofPattern("HH:mm")))
                    .image(imageEntity)
                    .build();
            roomEntity = roomRepository.save(roomEntity);   // Id 포함한 객체로 재할당
        } catch (Exception e) {
            System.out.println(e.getMessage());
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

        RoomResponse roomResponse = RoomResponse.convertEntityToDTO(roomEntity, roomCreateRequest.getCamTurnedOn(), roomCreateRequest.getMicTurnedOn());
        return ResponseEntity.ok(new SuccessResponse(200, roomResponse));
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

        // 유저 검증
        try {
            userEntity = userDetailsService.getUserByContextHolder();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + USER_NOT_FOUND_MESSAGE));
        }

        // roomSeq 유효성 검증
        Optional<RoomEntity> optionalRoomEntity = roomRepository.findById(roomId);
        if(optionalRoomEntity.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + ROOM_NOT_FOUND_MESSAGE));
        roomEntity = optionalRoomEntity.get();

        // room 입장 허용 여부 검증
        if(roomEntity.getIsDeleted() || !roomEntity.getStatus()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + ROOM_NOT_EXISTING_MESSAGE));

        }

        // 이미 입장한 방이 있는 경우 (재입장)
        // JoinedRoom에서 실제 해당 방에 접속되어 있는 사용자인지 확인
        // 맞으면 해당 방 찾아서 그대로 Room 반환
        if(joinedRoomRepository.existsByRoomAndUser(roomEntity, userEntity)) {
            // 재입장
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

            RoomResponse roomResponse = RoomResponse.convertEntityToDTO(roomEntity, roomEnterRequest.getCamTurnedOn(), roomEnterRequest.getMicTurnedOn());
            return ResponseEntity.ok(new SuccessResponse(200, roomResponse));
        } else {
            // 신규 입장
            // 최대 인원 수 확인해서 여유가 있는 경우 입장
            if(roomEntity.getRoomNum() >= roomEntity.getCapacity())  // Full
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(403, "에러 메시지: " + ROOM_ALREADY_FULL_MESSAGE));

            joinedRoomEntity = JoinedRoomEntity.builder()
                    .user(userEntity)
                    .room(roomEntity)
                    .isAdmin(false)
                    .isParticipating(true)
                    .build();
            joinedRoomRepository.save(joinedRoomEntity);

            roomEntity.setRoomNum(roomEntity.getRoomNum() + 1);
            roomRepository.save(roomEntity);

            RoomResponse roomResponse = RoomResponse.convertEntityToDTO(roomEntity, roomEnterRequest.getCamTurnedOn(), roomEnterRequest.getMicTurnedOn());
            return ResponseEntity.ok(new SuccessResponse(200, roomResponse));
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
        // roomId 확인
        // 방장 탈퇴 시 반장 변경 로직 (차기 방장 id 먼저 받자)
        // 마지막 남은 사람이 나가는 경우 status 변경 후 폐쇄 조치

        return null;
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

        return null;
    }

    /**
     * 방 입장 조회 메서드
     * @param roomId 참여하고자 하는 방의 Id
     * @return 자신을 포함하여 현재 방에 속해 있는 멤버들의 데이터 전체
     */
    @Transactional
    public ResponseEntity<CommonResponse> getRoomData(Long roomId) {
        // 반환 데이터 : 자신을 포함한 멤버의 데이터


        return null;
    }
}
