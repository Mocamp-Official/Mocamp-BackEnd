package com.mocamp.mocamp_backend.controller;

import com.mocamp.mocamp_backend.dto.commonResponse.CommonResponse;
import com.mocamp.mocamp_backend.dto.room.RoomCreateRequest;
import com.mocamp.mocamp_backend.dto.room.RoomEnterRequest;
import com.mocamp.mocamp_backend.service.room.RoomHttpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Tag(name = "Room Controller", description = "모캠프 작업 공간(방) 메타 데이터 저장을 위한 HTTP 엔드포엔트")
@RestController
@RequestMapping("/api/room")
@RequiredArgsConstructor
public class RoomHttpController {
    private final RoomHttpService roomHttpService;

    @Operation(
            summary = "모캠프 방 생성 (방장 전용)",
            parameters = { @Parameter(name = "Authorization", description = "Jwt 토큰", required = true) },
            responses = { @ApiResponse(responseCode = "200", description = "작업 공간 생성 성공") }
    )
    @PostMapping("/create")
    public ResponseEntity<CommonResponse> createRoom(
            @RequestHeader(name = "Authorization") String token,
            @RequestBody RoomCreateRequest roomCreateRequest) {
        return roomHttpService.createRoom(token, roomCreateRequest);
    }

    @Operation(
            summary = "모캠프 방 입장 (참가자 전용)",
            parameters = {
                    @Parameter(name = "Authorization", description = "Jwt 토큰", required = true),
                    @Parameter(name = "roomId", description = "입장하고자 하는 방 ID", required = true)
            },
            responses = { @ApiResponse(responseCode = "200", description = "입장 성공") }
    )
    @PostMapping("/enter/{roomId}")
    public ResponseEntity<CommonResponse> enterRoom(
            @RequestHeader(name = "Authorization") String token,
            @PathVariable String roomId,
            @RequestBody RoomEnterRequest roomEnterRequest) {
        return roomHttpService.enterRoom(token, roomId, roomEnterRequest);
    }

    @Operation(
            summary = "모캠프 방 퇴장",
            parameters = {
                    @Parameter(name = "Authorization", description = "Jwt 토큰", required = true),
                    @Parameter(name = "roomId", description = "퇴장하고자 하는 방 ID", required = true)
            },
            responses = { @ApiResponse(responseCode = "200", description = "퇴장 성공") }
    )
    @PostMapping("/exit/{roomId}")
    public ResponseEntity<CommonResponse> exitRoom(
            @RequestHeader(name = "Authorization") String token,
            @PathVariable String roomId) {
        return roomHttpService.exitRoom(token, roomId);
    }

    @Operation(
            summary = "모캠프 방 정보 수정",
            parameters = {
                    @Parameter(name = "Authorization", description = "Jwt 토큰", required = true),
                    @Parameter(name = "roomId", description = "데이터를 수정하고자 하는 방 ID", required = true)
            },
            responses = { @ApiResponse(responseCode = "200", description = "데이터 수정 성공") }
    )
    @PostMapping("/modify/{roomId}")
    public ResponseEntity<CommonResponse> modifyRoomData(
            @RequestHeader(name = "Authorization") String token,
            @PathVariable String roomId) {
        return roomHttpService.modifyRoomData(token, roomId);
    }



    @Operation(
            summary = "모캠프 방 정보 조회",
            parameters = {
                    @Parameter(name = "Authorization", description = "Jwt 토큰", required = true),
                    @Parameter(name = "roomId", description = "데이터를 조회하고자 하는 방 ID", required = true)
            },
            responses = { @ApiResponse(responseCode = "200", description = "데이터 조회 성공") }
    )
    @GetMapping("/{roomId}")
    public ResponseEntity<CommonResponse> getRoomData(
            @RequestHeader(name = "Authorization") String token,
            @PathVariable String roomId) {
        return roomHttpService.getRoomData(token, roomId);
    }

    @Operation(
            summary = "남은 시간 업데이트",
            parameters = {
                    @Parameter(name = "Authorization", description = "Jwt 토큰", required = true),
                    @Parameter(name = "roomId", description = "데이터를 조회하고자 하는 방 ID", required = true)
            },
            responses = { @ApiResponse(responseCode = "200", description = "시간 데이터 조회 성공") }
    )
    @GetMapping("/time/{roomId}")
    public ResponseEntity<CommonResponse> getRoomTime(
            @RequestHeader(name = "Authorization") String token,
            @PathVariable String roomId) {
        return roomHttpService.getRoomTime(token, roomId);
    }

    @Operation(
            summary = "참여 인원 수 업데이트",
            parameters = {
                    @Parameter(name = "Authorization", description = "Jwt 토큰", required = true),
                    @Parameter(name = "roomId", description = "데이터를 조회하고자 하는 방 ID", required = true)
            },
            responses = { @ApiResponse(responseCode = "200", description = "참여 인원 수 조회 성공") }
    )
    @GetMapping("/participant/{roomId}")
    public ResponseEntity<CommonResponse> getRoomParticipant(
            @RequestHeader(name = "Authorization") String token,
            @PathVariable String roomId) {
        return roomHttpService.getRoomParticipant(token, roomId);
    }
}
