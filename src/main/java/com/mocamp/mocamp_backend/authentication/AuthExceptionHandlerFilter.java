package com.mocamp.mocamp_backend.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import com.mocamp.mocamp_backend.dto.commonResponse.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// JwtAuthenticationFilter에서 발생한 오류를 처리하는 필터
@Component
public class AuthExceptionHandlerFilter extends OncePerRequestFilter {

    private static final String LOGIN_AGAIN_MESSAGE = "다시 로그인 해주세요";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private void setErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException{
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        ErrorResponse errorResponse = new ErrorResponse(errorCode.getHttpStatus().value(), errorCode.getMessage());
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException | MalformedJwtException e) {
            // 토큰의 유효기간 만료 또는 잘못된 토큰
            setErrorResponse(response, ErrorCode.EXPIRED_TOKEN);
        } catch (SecurityException e) {
            setErrorResponse(response, ErrorCode.INVALID_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            // 유효하지 않은 토큰
            setErrorResponse(response, ErrorCode.INVALID_TOKEN);
        }
    }

    @Getter
    @RequiredArgsConstructor
    enum ErrorCode{
        INVALID_TOKEN(HttpStatus.UNAUTHORIZED, LOGIN_AGAIN_MESSAGE),
        EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, LOGIN_AGAIN_MESSAGE);

        private final HttpStatus httpStatus;
        private final String message;
    }
}
