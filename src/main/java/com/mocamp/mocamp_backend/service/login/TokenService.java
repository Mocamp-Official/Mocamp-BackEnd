package com.mocamp.mocamp_backend.service.login;

import com.mocamp.mocamp_backend.authentication.JwtProvider;
import com.mocamp.mocamp_backend.authentication.UserDetailsServiceImpl;
import com.mocamp.mocamp_backend.dto.commonResponse.CommonResponse;
import com.mocamp.mocamp_backend.dto.commonResponse.ErrorResponse;
import com.mocamp.mocamp_backend.dto.commonResponse.SuccessResponse;
import com.mocamp.mocamp_backend.dto.loginResponse.LoginResponse;
import com.mocamp.mocamp_backend.dto.loginResponse.LoginResult;
import com.mocamp.mocamp_backend.entity.UserEntity;
import com.mocamp.mocamp_backend.repository.TokenRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class TokenService {

    private static final String DIFFERENT_REFRESH_TOKEN_EXCEPTION_MESSAGE = "다시 로그인 해주세요";
    private final JwtProvider jwtProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final TokenRepository tokenRepository;

    /**
     * Redis에 리프레쉬 토큰 지우는 메서드
     * @param userId 유저 ID
     */
    private void deleteRefreshToken(Long userId) {
        tokenRepository.deleteById(userId);
    }

    /**
     * 쿠키에 담긴 리프레쉬 토큰만 추출하는 메서드
     * @param request 요청
     * @return refreshToken 자체의 문자열
     */
    private String resolveTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * JwtProvider의 토큰 생성 기능을 그대로 사용하기 위해 Authentication 객체를 만드는 메서드
     * @param email 사용자의 이메일 입력
     * @return 사용자별 설정 권한이 포함된 Authentication 객체
     */
    private Authentication createAuthenticationFromEmail(String email) {
        return new UsernamePasswordAuthenticationToken(
                email, // principal - 유저 이메일
                null, // credentials - 소셜 로그인이므로 별도 비밀번호 설정 X
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    /**
     * LoginResult 응답 객체를 생성하는 메서드
     * @param accessToken 생성한 액세스 토큰
     * @param refreshToken 생성한 리프레쉬 토큰
     * @return 응답 객체 반환
     */
    private LoginResult createLoginResult(String accessToken, String refreshToken) {
        return LoginResult.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * 리프레쉬 토큰을 쿠키에 담아주는 메서드
     * @param refreshToken 리프레쉬 토큰
     * @return 리프레쉬 토큰 담긴 쿠키
     */
    private Cookie createRefreshTokenCookie(String refreshToken) {
        String cookieName = "refreshToken";
        String cookieValue = refreshToken;
        Cookie cookie = new Cookie(cookieName, cookieValue);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(1000 * 60 * 60 * 24 * 15);
        return cookie;
    }

    /**
     * 리프레쉬 토큰을 받아 재발급 하는 메서드
     * @param request 요청
     * @param response 응답
     * @return CommonResponse 응답 객체
     */
    public ResponseEntity<CommonResponse> reIssueToken(HttpServletRequest request, HttpServletResponse response) {
        UserEntity user = userDetailsService.getUserByContextHolder();

        // 쿠키에 담긴 리프레쉬 토큰과 Redis에 저장된 리프레쉬 토큰을 꺼내 일치하는지 확인
        String refreshToken = resolveTokenFromCookies(request);
        String refreshTokenInRedis = tokenRepository.findById(user.getUserId());

        if (refreshTokenInRedis == null || !refreshTokenInRedis.equals(refreshToken)) {
            deleteRefreshToken(user.getUserId());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(403, DIFFERENT_REFRESH_TOKEN_EXCEPTION_MESSAGE));
        }

        // 일치하면 새로운 액세스 토큰과 리프레쉬 토큰 생성
        Authentication authentication = createAuthenticationFromEmail(user.getEmail());
        String newAccessToken = jwtProvider.generateAccessToken(authentication);
        String newRefreshToken = jwtProvider.generateRefreshToken(authentication);
        tokenRepository.save(user.getUserId(), newRefreshToken);

        LoginResult loginResult = createLoginResult(newAccessToken, newRefreshToken);

        Cookie refreshTokenCookie = createRefreshTokenCookie(loginResult.getRefreshToken());
        response.addCookie(refreshTokenCookie);

        return ResponseEntity.ok(new SuccessResponse(200, new LoginResponse(loginResult.getAccessToken())));
    }
}
