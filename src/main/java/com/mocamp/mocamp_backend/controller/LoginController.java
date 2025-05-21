package com.mocamp.mocamp_backend.controller;

import com.mocamp.mocamp_backend.dto.commonResponse.CommonResponse;
import com.mocamp.mocamp_backend.dto.commonResponse.SuccessResponse;
import com.mocamp.mocamp_backend.dto.loginResponse.LoginResponse;
import com.mocamp.mocamp_backend.dto.loginResponse.LoginResult;
import com.mocamp.mocamp_backend.service.login.GoogleLoginService;
import com.mocamp.mocamp_backend.service.login.KakaoLoginService;
import com.mocamp.mocamp_backend.service.login.NaverLoginService;
import com.mocamp.mocamp_backend.service.login.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private final TokenService tokenService;

    private Cookie createRefreshTokenCookie(String refreshToken) {
        String cookieName = "refreshToken";
        String cookieValue = refreshToken;
        Cookie cookie = new Cookie(cookieName, cookieValue);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24 * 15);
        return cookie;
    }

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
    public ResponseEntity<CommonResponse> loginViaGoogle(@RequestParam(name = "code") String code,
                                                         @RequestParam(name = "redirect_url") String redirectUrl,
                                                         HttpServletResponse response) {
        System.out.println("asdasd");
        ResponseEntity<CommonResponse> responseEntity = googleLoginService.logInViaGoogle(code, redirectUrl);
        CommonResponse body = responseEntity.getBody();

        if (body instanceof SuccessResponse success && success.getMessage() instanceof LoginResult loginResult) {
            response.addCookie(createRefreshTokenCookie(loginResult.getRefreshToken()));
            return ResponseEntity.ok(new SuccessResponse(200, new LoginResponse(loginResult.getAccessToken())));
        }

        return responseEntity;
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
    public ResponseEntity<CommonResponse> kakaoLogin(@RequestParam(name = "code") String code,
                                                     @RequestParam(name = "redirect_url") String redirect_url,
                                                     HttpServletResponse response) {
        LoginResult loginResult = kakaoLoginService.kakaoLogin(code, redirect_url);

        Cookie refreshTokenCookie = createRefreshTokenCookie(loginResult.getRefreshToken());
        response.addCookie(refreshTokenCookie);

        return ResponseEntity.ok(new SuccessResponse(200, new LoginResponse(loginResult.getAccessToken())));
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
    public ResponseEntity<CommonResponse> naverLogin(@RequestParam(name = "code") String code,
                                                     @RequestParam(name = "redirect_url") String redirect_url,
                                                     HttpServletResponse response) {
        LoginResult loginResult = naverLoginService.naverLogin(code, redirect_url);

        Cookie refreshTokenCookie = createRefreshTokenCookie(loginResult.getRefreshToken());
        response.addCookie(refreshTokenCookie);

        return ResponseEntity.ok(new SuccessResponse(200, new LoginResponse(loginResult.getAccessToken())));
    }

    @Operation(
            summary = "JWT 토큰 재발급",
            responses = {
                    @ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
                    @ApiResponse(responseCode = "403", description = "리프레시 토큰 불일치 또는 유효하지 않음")
            }
    )
    @PostMapping("/re-issue")
    public ResponseEntity<CommonResponse> reIssueToken(HttpServletRequest request, HttpServletResponse response) {
        return tokenService.reIssueToken(request, response);
    }

}
