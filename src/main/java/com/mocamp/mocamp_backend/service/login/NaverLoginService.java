package com.mocamp.mocamp_backend.service.login;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mocamp.mocamp_backend.authentication.JwtProvider;
import com.mocamp.mocamp_backend.dto.commonResponse.CommonResponse;
import com.mocamp.mocamp_backend.dto.commonResponse.SuccessResponse;
import com.mocamp.mocamp_backend.dto.loginResponse.LoginResponse;
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
public class NaverLoginService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    @Value("${naver.client.id}")
    private String clientId;
    @Value("${naver.client.secret}")
    private String clientSecret;
    private final static String NAVER_AUTH_URI = "https://nid.naver.com";
    private final static String NAVER_API_URI = "https://openapi.naver.com";

    /**
     * "인가 코드"로 네이버 "액세스 토큰" 요청하는 메서드
     * @param code -> 인가 코드
     * @param redirect_url -> 리디렉션 url
     * @return AccessToken 반환
     */
    private String getAccessToken(String code, String redirect_url) {
        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded");

        // HTTP Body 생성
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("redirect_uri", redirect_url);
        body.add("code", code);
        body.add("client_secret", clientSecret);

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> naverTokenRequest = new HttpEntity<>(body, headers);
        RestTemplate rt = new RestTemplate();

        ResponseEntity<String> response = rt.exchange(
                NAVER_AUTH_URI + "/oauth2.0/token",
                HttpMethod.POST,
                naverTokenRequest,
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
     * 네이버 "액세스 토큰"으로 네이버 API 호출하여 유저 정보 받아오는 메서드
     * @param naverAccessToken 액세스 토큰
     * @return 유저 정보 HashMap으로 응답
     */
    private HashMap<String, Object> getNaverUserInfo(String naverAccessToken) {
        HashMap<String, Object> userInfo= new HashMap<String,Object>();

        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + naverAccessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> naverUserInfoRequest = new HttpEntity<>(headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                NAVER_API_URI + "/v1/nid/me",
                HttpMethod.POST,
                naverUserInfoRequest,
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

        String id = jsonNode.get("response").get("id").asText();
        String email = jsonNode.get("response").get("email").asText();
        String nickname = jsonNode.get("response").get("name").asText();

        userInfo.put("id",id);
        userInfo.put("email",email);
        userInfo.put("nickname",nickname);

        return userInfo;
    }

    /**
     * 네이버에서 받은 유저 정보를 기반으로 회원가입 or 로그인을 처리하여 jwt 토큰을 반환하는 메서드
     * @param naverUserInfo 네이버에서 받은 유저 정보(Map)
     */
    private LoginResponse naverUserLogin(HashMap<String, Object> naverUserInfo) {
        String userSeq = naverUserInfo.get("id").toString();
        String naverEmail = naverUserInfo.get("email").toString();
        String nickname = naverUserInfo.get("nickname").toString();

        UserEntity optionalUserEntity = userRepository.findUserByUserSeq(naverUserInfo.get("id").toString()).orElse(null);

        if(optionalUserEntity == null) { // 회원가입의 경우
            UserEntity newUserEntity = createUserEntity(userSeq, naverEmail, nickname);
            userRepository.save(newUserEntity);

            Authentication authentication = createAuthenticationFromEmail(newUserEntity.getEmail());
            String accessToken = jwtProvider.generateAccessToken(authentication);
            String refreshToken = jwtProvider.generateRefreshToken(authentication);

            return createLoginResponse(accessToken, refreshToken);
        } else { // 기존 로그인의 경우
            Authentication authentication = createAuthenticationFromEmail(optionalUserEntity.getEmail());
            String accessToken = jwtProvider.generateAccessToken(authentication);
            String refreshToken = jwtProvider.generateRefreshToken(authentication);

            return createLoginResponse(accessToken, refreshToken);
        }
    }

    /**
     * 회원가입의 경우, User Entity를 생성해주는 메서드
     * @param userSeq 네이버 고유 ID
     * @param naverEmail 네이버 이메일
     * @param nickname 네이버 이름
     * @return UserEntity
     */
    private UserEntity createUserEntity(String userSeq, String naverEmail, String nickname) {
        return UserEntity.builder()
                .userSeq(userSeq)
                .email(naverEmail)
                .username(nickname)
                .emailVerifiedYN("N")
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .build();
    }

    /**
     * JwtProvider 토큰 생성 기능을 사용하기 위해 Authentication 객체를 만드는 메서드
     * @param email 사용자의 이메일
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
     * NaverLoginResponse 응답 객체를 생성하는 메서드
     * @param accessToken 생성한 액세스 토큰
     * @param refreshToken 생성한 리프레쉬 토큰
     * @return 응답 객체 반환
     */
    private LoginResponse createLoginResponse(String accessToken, String refreshToken) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * 네이버 로그인 페이지 로드를 위한 uri 제공 메서드
     * @param redirect_url 리디렉션 uri
     * @return 로그인 페이지 uri
     */
    public ResponseEntity<CommonResponse> loadNaverLoginPage(String redirect_url) {
        String uri = NAVER_AUTH_URI+ "/oauth2.0/authorize"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirect_url
                + "&response_type=code";

        return ResponseEntity.ok(new SuccessResponse(200, uri));
    }

    /**
     * 네이버 로그인을 처리하는 메서드
     * @param code 인가 코드
     * @param redirect_url 리디렉션 url
     * @return 네이버 로그인 응답 메시지 반환
     */
    public LoginResponse naverLogin(String code, String redirect_url) {
        // 1. "인가 코드"로 "액세스 토큰" 요청
        String naverAccessToken = getAccessToken(code, redirect_url);
        System.out.println(naverAccessToken);

        // 2. "액세스 토큰"으로 네이버 API 호출 후, 유저 정보 받아오기
        HashMap<String, Object> naverUserInfo = getNaverUserInfo(naverAccessToken);
        System.out.println(naverUserInfo);

        //3. 네이버 ID로 회원가입 & 로그인 처리
        return naverUserLogin(naverUserInfo);
    }

}
