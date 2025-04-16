package com.mocamp.mocamp_backend.controller;

import com.mocamp.mocamp_backend.dto.commonResponse.CommonResponse;
import com.mocamp.mocamp_backend.service.login.GoogleLoginService;
import com.mocamp.mocamp_backend.service.login.KakaoLoginService;
import lombok.Getter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/login")
public class LoginController {
    private final GoogleLoginService googleLoginService;
    private final KakaoLoginService kakaoLoginService;

    public LoginController(final GoogleLoginService googleLoginService, final KakaoLoginService kakaoLoginService) {
        this.googleLoginService = googleLoginService;
        this.kakaoLoginService = kakaoLoginService;
    }

    // 구글 로그인 페이지 로딩
    @GetMapping("/google/page")
    public ResponseEntity<CommonResponse> loadGoogleLoginPage() {
        return googleLoginService.loadGoogleLoginPage();
    }

    // 구글 로그인 진행 (리다이렉션 URI)
    @GetMapping("/google/process")
    public ResponseEntity<CommonResponse> loginViaGoogle(@RequestParam(name = "code") String code) {
        return googleLoginService.logInViaGoogle(code);
    }
}
