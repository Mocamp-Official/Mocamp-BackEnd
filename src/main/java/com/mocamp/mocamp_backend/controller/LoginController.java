package com.mocamp.mocamp_backend.controller;

import com.mocamp.mocamp_backend.dto.commonResponse.CommonResponse;
import com.mocamp.mocamp_backend.dto.kakao.KakaoLoginResponse;
import com.mocamp.mocamp_backend.dto.naver.NaverLoginResponse;
import com.mocamp.mocamp_backend.service.login.GoogleLoginService;
import com.mocamp.mocamp_backend.service.login.KakaoLoginService;
import com.mocamp.mocamp_backend.service.login.NaverLoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Login Controller", description = "구글/카카오/네이버 소셜 로그인 엔드포인트")
@RestController
@RequestMapping("/api/login")
@RequiredArgsConstructor
public class LoginController {
    private final GoogleLoginService googleLoginService;
    private final KakaoLoginService kakaoLoginService;
    private final NaverLoginService naverLoginService;

    @Operation(
            summary = "구글 로그인 페이지 로딩",
            responses = { @ApiResponse(responseCode = "200", description = "URL 반환 성공") }
    )
    @GetMapping("/google/page")
    public ResponseEntity<CommonResponse> loadGoogleLoginPage() {
        return googleLoginService.loadGoogleLoginPage();
    }

    @Operation(
            summary = "구글 로그인 리다이렉션 URI",
            parameters = { @Parameter(name = "code", description = "로그인 후 구글 서버에서 반환하는 코드") },
            responses = { @ApiResponse(responseCode = "200", description = "로그인 성공") }
    )
    @GetMapping("/google/process")
    public ResponseEntity<CommonResponse> loginViaGoogle(@RequestParam(name = "code") String code) {
        return googleLoginService.logInViaGoogle(code);
    }

    @Operation(
            summary = "카카오 로그인 페이지 로딩",
            responses = { @ApiResponse(responseCode = "200", description = "URL 반환 성공") }
    )
    @GetMapping("/kakao/page")
    public ResponseEntity<CommonResponse> loadKakaoLoginPage() {
        return kakaoLoginService.loadKakaoLoginPage();
    }

    @Operation(
            summary = "카카오 로그인 리다이렉션 URI",
            parameters = { @Parameter(name = "code", description = "로그인 후 카카오 서버에서 반환하는 코드") }
    )
    @GetMapping("/kakao/process")
    public ResponseEntity<KakaoLoginResponse> kakaoLogin(@RequestParam(name = "code") String code) {
        KakaoLoginResponse kakaoLoginResponse = kakaoLoginService.kakaoLogin(code);
        return ResponseEntity.ok(kakaoLoginResponse);
    }

    @Operation(
            summary = "네이버 로그인 페이지 로딩",
            responses = { @ApiResponse(responseCode = "200", description = "URL 반환 성공") }
    )
    @GetMapping("/naver/page")
    public ResponseEntity<CommonResponse> loadNaverLoginPage() {
        return naverLoginService.loadNaverLoginPage();
    }

    @Operation(
            summary = "네이버 로그인 리다이렉션 URI",
            parameters = { @Parameter(name = "code", description = "로그인 후 네이버 서버에서 반환하는 코드") }
    )
    @GetMapping("/naver/process")
    public ResponseEntity<NaverLoginResponse> naverLogin(@RequestParam(name = "code") String code) {
        NaverLoginResponse naverLoginResponse = naverLoginService.naverLogin(code);
        return ResponseEntity.ok(naverLoginResponse);
    }
}
