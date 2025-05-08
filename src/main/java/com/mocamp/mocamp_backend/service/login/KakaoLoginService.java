package com.mocamp.mocamp_backend.service.login;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mocamp.mocamp_backend.authentication.JwtProvider;
import com.mocamp.mocamp_backend.dto.commonResponse.CommonResponse;
import com.mocamp.mocamp_backend.dto.commonResponse.SuccessResponse;
import com.mocamp.mocamp_backend.dto.kakao.KakaoLoginResponse;
import com.mocamp.mocamp_backend.entity.UserEntity;
import com.mocamp.mocamp_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class KakaoLoginService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    @Value("${kakao.key.client-id}")
    private String clientId;
    @Value("${kakao.redirect-uri}")
    private String redirectUri;
    @Value("${kakao.page.uri}")
    private String pageUri;

    /**
     * "인가 코드"로 카카오 "액세스 토큰" 요청하는 메서드
     * @param code -> 인가 코드
     * @return AccessToken 반환
     */
    private String getAccessToken(String code) {
        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP Body 생성
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(body, headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                "https://kauth.kakao.com/oauth/token",
                HttpMethod.POST,
                kakaoTokenRequest,
                String.class
        );

        // HTTP 응답 (JSON) -> 액세스 토큰 파싱
        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(responseBody);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return jsonNode.get("access_token").asText(); // 토큰 전송
    }

    /**
     * 카카오 "액세스 토큰"으로 카카오 API 호출하여 유저 정보 받아오는 메서드
     * @param kakaoAccessToken 액세스 토큰
     * @return 유저 정보 HashMap으로 응답
     */
    private HashMap<String, Object> getKakaoUserInfo(String kakaoAccessToken) {
        HashMap<String, Object> userInfo= new HashMap<String,Object>();

        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + kakaoAccessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> kakaoUserInfoRequest = new HttpEntity<>(headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.POST,
                kakaoUserInfoRequest,
                String.class
        );

        // responseBody에 있는 정보를 꺼냄
        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(responseBody);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        Long id = jsonNode.get("id").asLong();
        String email = jsonNode.get("kakao_account").get("email").asText();
        String nickname = jsonNode.get("properties").get("nickname").asText();

        userInfo.put("id",id);
        userInfo.put("email",email);
        userInfo.put("nickname",nickname);

        return userInfo;
    }

    private UserEntity createUserEntity(String userSeq, String kakaoEmail, String nickname) {
        return UserEntity.builder()
                .userSeq(userSeq)
                .email(kakaoEmail)
                .username(nickname)
                .emailVerifiedYN("N")
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 카카오에서 받은 유저 정보를 기반으로 회원가입 or 로그인을 처리하여 jwt 토큰을 반환하는 메서드
     * @param kakaoUserInfo 카카오에서 받은 유저 정보(Map)
     * @return 카카오 로그인 응답 객체 반환
     */
    private KakaoLoginResponse kakaoUserLogin(HashMap<String, Object> kakaoUserInfo) {
        String userSeq = kakaoUserInfo.get("id").toString();
        String kakaoEmail = kakaoUserInfo.get("email").toString();
        String nickname = kakaoUserInfo.get("nickname").toString();

        UserEntity optionalUserEntity = userRepository.findUserByUserSeq(kakaoUserInfo.get("id").toString()).orElse(null);

        if(optionalUserEntity == null) { // 회원가입의 경우
            UserEntity newUserEntity = createUserEntity(userSeq, kakaoEmail, nickname);
            userRepository.save(newUserEntity);

            Authentication authentication = createAuthenticationFromEmail(newUserEntity.getEmail());
            String accessToken = jwtProvider.generateAccessToken(authentication);
            String refreshToken = jwtProvider.generateRefreshToken(authentication);

            return createKakaoLoginResponse(newUserEntity, accessToken, refreshToken);
        } else { // 기존 로그인의 경우
            Authentication authentication = createAuthenticationFromEmail(optionalUserEntity.getEmail());
            String accessToken = jwtProvider.generateAccessToken(authentication);
            String refreshToken = jwtProvider.generateRefreshToken(authentication);

            return createKakaoLoginResponse(optionalUserEntity, accessToken, refreshToken);
        }
    }

    /**
     * KakaoLoginResponse 응답 객체를 생성하는 메서드
     * @param userEntity 회원가입 or 로그인 한 유저 객체
     * @param accessToken 생성한 액세스 토큰
     * @param refreshToken 생성한 리프레쉬 토큰
     * @return 응답 객체 반환
     */
    private KakaoLoginResponse createKakaoLoginResponse(UserEntity userEntity, String accessToken, String refreshToken) {
        return KakaoLoginResponse.builder()
                .id(userEntity.getUserId())
                .email(userEntity.getEmail())
                .username(userEntity.getUsername())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
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
     * 카카오 로그인을 처리하는 메서드
     * @param code 인가 코드
     * @return 카카오 로그인 완료 응답 메시지 반환
     */
    public KakaoLoginResponse kakaoLogin(String code) {
        // 1. "인가 코드"로 "액세스 토큰" 요청
        String KakaoAccessToken = getAccessToken(code);
        System.out.println(KakaoAccessToken);

        // 2. "액세스 토큰"으로 카카오 API 호출 후, 유저 정보 받아오기
        HashMap<String, Object> kakaoUserInfo = getKakaoUserInfo(KakaoAccessToken);
        System.out.println(kakaoUserInfo);

        //3. 카카오ID로 회원가입 & 로그인 처리
        return kakaoUserLogin(kakaoUserInfo);
    }

    /**
     * 카카오 로그인 페이지 로드를 위한 uri 제공 메서드
     * @return 로그인 페이지 uri
     */
    public ResponseEntity<CommonResponse> loadKakaoLoginPage() {
        String uri = pageUri + "?"
                + "client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code";

        return ResponseEntity.ok(new SuccessResponse(200, uri));
    }
}
