package com.mocamp.mocamp_backend.controller;

import com.mocamp.mocamp_backend.dto.commonResponse.CommonResponse;
import com.mocamp.mocamp_backend.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "User Controller (마이홈)", description = "마이홈 메뉴 구성을 위한 HTTP 엔드포인트")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @Operation(
            summary = "유저 정보 조회(모캠프 사용 추이 데이터 포함)",
            parameters = { @Parameter(name = "Authorization", description = "Jwt 토큰", required = true) },
            responses = { @ApiResponse(responseCode = "200", description = "유저 정보 조회 성공") }
    )
    @GetMapping("/profile")
    public ResponseEntity<CommonResponse> getUserProfile() {
        return userService.getUserProfile();
    }

    @Operation(
            summary = "모캠프 방 별 목표 정보 조회",
            parameters = {
                    @Parameter(name = "Authorization", description = "Jwt 토큰", required = true),
                    @Parameter(name = "roomId", description = "방 ID", required = true)
            },
            responses = { @ApiResponse(responseCode = "200", description = "목표 조회 성공") }
    )
    @GetMapping("/goal/{roomId}")
    public ResponseEntity<CommonResponse> getUserGoals(@PathVariable("roomId") Long roomId) {
        return userService.getUserGoals(roomId);
    }

    @Operation(
            summary = "유저 로그아웃",
            parameters = { @Parameter(name = "Authorization", description = "Jwt 토큰", required = true) },
            responses = { @ApiResponse(responseCode = "200", description = "로그아웃 성공") }
    )
    @PostMapping("/logout")
    public ResponseEntity<CommonResponse> logout() {
        return userService.logout();
    }

    @Operation(
            summary = "유저 정보 수정(유저 이름 및 프로필 사진)",
            parameters = { @Parameter(name = "Authorization", description = "Jwt 토큰", required = true) },
            responses = { @ApiResponse(responseCode = "200", description = "유저 정보 수정 성공") }
    )
    @PatchMapping("/modify")
    public ResponseEntity<CommonResponse> modifyUserProfile(@RequestPart(value = "username", required = false) String username,
                                                            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        return userService.modifyUserProfile(username, imageFile);
    }
}