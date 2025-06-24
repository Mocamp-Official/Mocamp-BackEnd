package com.mocamp.mocamp_backend.configuration;

import com.mocamp.mocamp_backend.authentication.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@RequiredArgsConstructor
@Component
@Slf4j
public class KurentoInterceptor implements HandshakeInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
            String token = httpServletRequest.getParameter("token");
            log.info(token);


            if (token != null) {
                try {
                    if (!jwtProvider.validateToken(token)) {
                        throw new RuntimeException("Invalid token");
                    }
                    Authentication authentication = jwtProvider.getAuthentication(token);
                    String userId = authentication.getName();
                    attributes.put("userId", userId); // WebSocketSession에서 접근 가능
                    return true;
                } catch (Exception e) {
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return false;
                }
            }
        }

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 생략 가능
    }
}
