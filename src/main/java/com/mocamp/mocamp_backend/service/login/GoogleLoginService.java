package com.mocamp.mocamp_backend.service.login;

import com.mocamp.mocamp_backend.authentication.JwtProvider;
import com.mocamp.mocamp_backend.configuration.GoogleLoginConfig;
import com.mocamp.mocamp_backend.dto.commonResponse.CommonResponse;
import com.mocamp.mocamp_backend.dto.commonResponse.ErrorResponse;
import com.mocamp.mocamp_backend.dto.commonResponse.SuccessResponse;
import com.mocamp.mocamp_backend.entity.UserEntity;
import com.mocamp.mocamp_backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

@Service
public class GoogleLoginService {
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final GoogleLoginConfig googleLoginConfig;
    private final HttpServletResponse httpServletResponse;
    private final JwtProvider jwtProvider;
    private final RestTemplate restTemplate;

    public GoogleLoginService(final UserRepository userRepository, final AuthenticationManager authenticationManager,
                              final GoogleLoginConfig googleLoginConfig,
                              final HttpServletResponse httpServletResponse, final JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.googleLoginConfig = googleLoginConfig;
        this.httpServletResponse = httpServletResponse;
        this.jwtProvider = jwtProvider;
        this.restTemplate = new RestTemplate();
    }

    /**
     * 구글 서버에 Access Token을 요청하는 메서드
     * 사용자가 로그인을 진행한 뒤에 생성되는 코드를 Query String으로 갖고 와서 구글에 요청을 보낸다
     * @param code 클라이언트가 전달해주는 코드 ({API Endpoint}/login/google?code={코드})
     */
    private String requestGoogleAccessToken(final String code) {
        final String decodedCode = URLDecoder.decode(code, StandardCharsets.UTF_8);
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_FORM_URLENCODED_VALUE);

        GoogleLoginRequest googleClient = GoogleLoginRequest.builder()
                .code(decodedCode)
                .clientId(googleLoginConfig.getClientId())
                .clientSecret(googleLoginConfig.getClientSecret())
                .grantType(googleLoginConfig.getGrantType())
                .redirectUri(googleLoginConfig.getRedirectUri())
                .build();

        HttpEntity<GoogleLoginRequest> httpEntity = new HttpEntity<>(googleClient, headers);

        final GoogleLoginResponse response = restTemplate.exchange(
                googleLoginConfig.getAccessTokenUri(), HttpMethod.POST, httpEntity, GoogleLoginResponse.class
        ).getBody();

        return response.getAccessToken();
    }

    /**
     * 구글 서버에 사용자 정보를 요청하는 메서드
     * 발급 받은 Access Token을 가지고 구글에 요청을 보내 사용자 정보를 가져온다
     * @param accessToken 구글 Access Token
     * @return 구글 사용자 정보
     */
    private GoogleUserProfile requestGoogleUserProfile(final String accessToken) {
        // Access Token을 통해 사용자 정보 휙득
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        final HttpEntity<GoogleLoginRequest> httpEntity = new HttpEntity<>(headers);
        return restTemplate.exchange(googleLoginConfig.getUserProfileUri(), HttpMethod.GET, httpEntity, GoogleUserProfile.class)
                .getBody();
    }


    /**
     * DB에 사용자 정보를 저장하기 위한 엔티티 객체 생성 메서드
     */
    private UserEntity createUserEntity(GoogleUserProfile googleUserProfile) {
        return UserEntity.builder()
                .userSeq(googleUserProfile.getId())
                .email(googleUserProfile.getEmail())
                .username(googleUserProfile.getName())
                .emailVerifiedYN("N")
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
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
     * 구글 로그인 페이지 로드를 위한 uri 제공 메서드
     * @return 로그인 페이지 uri
     */
    public ResponseEntity<CommonResponse<String>> loadGoogleLoginPage() {
        String uri = googleLoginConfig.getLoginPageUri() + "?"
                + "client_id=" + googleLoginConfig.getClientId()
                + "&redirect_uri=" + googleLoginConfig.getRedirectUri()
                + "&response_type=" + googleLoginConfig.getResponseType()
                + "&scope=" + googleLoginConfig.getScope();

        return ResponseEntity.ok(new SuccessResponse<>(200, uri));
    }

    /**
     * 구글 로그인 메서드
     * 클라이언트가 전달하는 코드를 수신하여 구글 서버를 통해 사용자 정보를 추출하여 DB에 저장한다
     * @param code 클라이언트가 전달하는 코드
     */
    @Transactional
    public ResponseEntity<CommonResponse<String>> logInViaGoogle(String code) {
        UserEntity userEntity;
        GoogleUserProfile googleUserProfile;
        String jwtToken;

        try {
            String accessToken = requestGoogleAccessToken(code);
            googleUserProfile = requestGoogleUserProfile(accessToken);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse<>(403, "Error: failed to get user profile from google"));
        }

        try {
            Optional<UserEntity> optionalUserEntity = userRepository.findUserByUserSeq(googleUserProfile.getId());
            if (optionalUserEntity.isEmpty()) {   // 회원가입
                UserEntity newUserEntity = createUserEntity(googleUserProfile);
                userEntity = userRepository.save(newUserEntity);
            } else {
                userEntity = optionalUserEntity.get();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse<>(403, "Error: failed to load or save user profile"));
        }

        try {
            Authentication authentication = createAuthenticationFromEmail(userEntity.getEmail());
            jwtToken = jwtProvider.generateToken(authentication, JwtProvider.ACCESS_TOKEN_EXPIRE);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse<>(403, "Error: failed to create jwt token"));
        }

        return ResponseEntity.ok(new SuccessResponse<>(200, jwtToken));
    }
}