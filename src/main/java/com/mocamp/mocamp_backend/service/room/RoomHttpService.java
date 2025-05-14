package com.mocamp.mocamp_backend.service.room;

import com.mocamp.mocamp_backend.authentication.JwtProvider;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoomHttpService {
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final ImageRepository imageRepository;
    private final JoinedRoomRepository joinedRoomRepository;


    private UserEntity getUserInfo(String token) throws Exception {
        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            throw new UserPrincipalNotFoundException("Invalid token");
        }

        String tokenValue = token.substring(7);
        Authentication authentication = jwtProvider.getAuthentication(tokenValue);
        String userEmail = authentication.getName();

        Optional<UserEntity> optionalUserEntity = userRepository.findUserByEmail(userEmail);
        if(optionalUserEntity.isEmpty()) {
            throw new UsernameNotFoundException("User not found");
        }

        return optionalUserEntity.get();
    }

    @Transactional
    public ResponseEntity<CommonResponse<RoomResponse>> createRoom(String token, RoomCreateRequest roomCreateRequest) {
        UserEntity userEntity;
        try {
            userEntity = getUserInfo(token);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse<>(403, null));
        }

        RoomEntity roomEntity = RoomEntity.builder()
                .roomName(roomCreateRequest.getRoomName())
                .capacity(roomCreateRequest.getCapacity())
                .status("active")
                .startedAt(LocalDateTime.now())
                .endedAt(LocalDateTime.now())
                .duration(LocalTime.parse(roomCreateRequest.getDuration(), DateTimeFormatter.ofPattern("HH:mm")))
                .build();
        roomEntity = roomRepository.save(roomEntity);   // Id 포함한 객체로 재할당

        ImageEntity imageEntity = ImageEntity.builder()
                .type(ImageType.room)
                .path(roomCreateRequest.getImagePath())
                .build();
        imageEntity = imageRepository.save(imageEntity);

        JoinedRoomEntity joinedRoomEntity = JoinedRoomEntity.builder()
                .user(userEntity)
                .room(roomEntity)
                .isAdmin(true)
                .isParticipating(true)
                .build();
        joinedRoomEntity = joinedRoomRepository.save(joinedRoomEntity);

        RoomResponse roomResponse = RoomResponse.convertEntityToDTO(roomEntity, roomCreateRequest.getMicAvailability());
        return ResponseEntity.ok(new SuccessResponse<>(200, roomResponse));
    }

    public ResponseEntity<CommonResponse<RoomResponse>> enterRoom(String token, String roomId, RoomEnterRequest roomEnterRequest) {
        return null;
    }

    public ResponseEntity<CommonResponse<String>> exitRoom(String token, String roomId) {
        return null;
    }

    public ResponseEntity<CommonResponse<String>> modifyRoomData(String token, String roomId) {
        return null;
    }

    public ResponseEntity<CommonResponse<RoomResponse>> getRoomData(String token, String roomId) {
        return null;
    }

    public ResponseEntity<CommonResponse<LocalDateTime>> getRoomTime(String token, String roomId) {
        return null;
    }

    public ResponseEntity<CommonResponse<Integer>> getRoomParticipant(String token, String roomId) {
        return null;
    }
}
