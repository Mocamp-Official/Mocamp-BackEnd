package com.mocamp.mocamp_backend.controller;

import com.mocamp.mocamp_backend.dto.commonResponse.CommonResponse;
import com.mocamp.mocamp_backend.dto.loginResponse.LoginResponse;
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
            parameters = { @Parameter(name = "redirect_url", description = "구글 인가 코드 요청 시 사용한 redirect_uri와 동일한 값") },
            responses = { @ApiResponse(responseCode = "200", description = "URL 반환 성공") }
    )
    @GetMapping("/google/page")
    public ResponseEntity<CommonResponse> loadGoogleLoginPage(@RequestParam(name = "redirect_url") String redirectUrl) {
        return googleLoginService.loadGoogleLoginPage(redirectUrl);
    }

    @Operation(
            summary = "구글 로그인 리다이렉션 URI",
            parameters = {
                    @Parameter(name = "code", description = "로그인 후 구글 서버에서 반환하는 코드"),
                    @Parameter(name = "redirect_url", description = "구글 인가 코드 요청 시 사용한 redirect_uri와 동일한 값")
            },
            responses = { @ApiResponse(responseCode = "200", description = "로그인 성공") }
    )
    @GetMapping("/google/process")
    public ResponseEntity<CommonResponse> loginViaGoogle(@RequestParam(name = "code") String code, @RequestParam(name = "redirect_url") String redirectUrl) {
        return googleLoginService.logInViaGoogle(code, redirectUrl);
    }

    @Operation(
            summary = "카카오 로그인 페이지 로딩",
            parameters = {@Parameter(name = "redirect_url", description = "리디렉션 될 주소")},
            responses = {@ApiResponse(responseCode = "200", description = "URL 반환 성공") }
    )
    @GetMapping("/kakao/page")
    public ResponseEntity<CommonResponse> loadKakaoLoginPage(@RequestParam(name = "redirect_url") String redirect_url) {
        return kakaoLoginService.loadKakaoLoginPage(redirect_url);
    }

    @Operation(
            summary = "카카오 로그인 리다이렉션 URI",
            parameters = {
                    @Parameter(name = "code", description = "로그인 후 카카오 서버에서 반환하는 코드"),
                    @Parameter(name = "redirect_url", description = "카카오 인가 코드 요청 시 사용한 redirect_uri와 동일한 값")
            },
            responses = {@ApiResponse(responseCode = "200", description = "로그인 성공 - JWT 토큰 반환")}
    )
    @GetMapping("/kakao/process")
    public ResponseEntity<LoginResponse> kakaoLogin(@RequestParam(name = "code") String code, @RequestParam(name = "redirect_url") String redirect_url) {
        LoginResponse loginResponse = kakaoLoginService.kakaoLogin(code, redirect_url);
        return ResponseEntity.ok(loginResponse);
    }

    @Operation(
            summary = "네이버 로그인 페이지 로딩",
            parameters = {@Parameter(name = "redirect_url", description = "리디렉션 될 주소")},
            responses = { @ApiResponse(responseCode = "200", description = "URL 반환 성공") }
    )
    @GetMapping("/naver/page")
    public ResponseEntity<CommonResponse> loadNaverLoginPage(@RequestParam(name = "redirect_url") String redirect_url) {
        return naverLoginService.loadNaverLoginPage(redirect_url);
    }

    @Operation(
            summary = "네이버 로그인 리다이렉션 URI",
            parameters = {
                    @Parameter(name = "code", description = "로그인 후 네이버 서버에서 반환하는 코드"),
                    @Parameter(name = "redirect_url", description = "네이버 인가 코드 요청 시 사용한 redirect_uri와 동일한 값")
            },
            responses = {@ApiResponse(responseCode = "200", description = "로그인 성공 - JWT 토큰 반환")}
    )
    @GetMapping("/naver/process")
    public ResponseEntity<LoginResponse> naverLogin(@RequestParam(name = "code") String code, @RequestParam(name = "redirect_url") String redirect_url) {
        LoginResponse loginResponse = naverLoginService.naverLogin(code, redirect_url);
        return ResponseEntity.ok(loginResponse);
    }
}
