package com.mocamp.mocamp_backend.controller;

import com.mocamp.mocamp_backend.dto.commonResponse.CommonResponse;
import com.mocamp.mocamp_backend.service.goal.GoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Goal Controller", description = "목표 설정 엔드포인트")
@RestController
@RequestMapping("/api/goal")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @Operation(
            summary = "방에 참여 중인 모든 유저의 목표 목록 조회",
            parameters = {
                    @Parameter(name = "roomId", description = "조회할 방의 ID")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "유저별 목표 목록 조회 성공"),
                    @ApiResponse(responseCode = "404", description = "존재하지 않는 방"),
                    @ApiResponse(responseCode = "403", description = "비활성화된 방 또는 접근 권한 없음")
            }
    )
    @GetMapping("/{roomId}")
    public ResponseEntity<CommonResponse> getGoals(@PathVariable Long roomId) {
        return goalService.getGoals(roomId);
    }
}
