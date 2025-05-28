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
            @RequestBody RoomCreateRequest roomCreateRequest) {
        return roomHttpService.createRoom(roomCreateRequest);
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
            @PathVariable Long roomId,
            @RequestBody RoomEnterRequest roomEnterRequest) {
        return roomHttpService.enterRoom(roomId, roomEnterRequest);
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
            @PathVariable Long roomId) {
        return roomHttpService.exitRoom(roomId, null);
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
            @PathVariable Long roomId) {
        return roomHttpService.modifyRoomData(roomId, null);
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
            @PathVariable Long roomId) {
        return roomHttpService.getRoomData(roomId);
    }
}
