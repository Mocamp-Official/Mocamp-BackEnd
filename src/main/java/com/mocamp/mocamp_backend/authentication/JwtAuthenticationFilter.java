package com.mocamp.mocamp_backend.authentication;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try{
            // 토큰 값만 추출
            String token = resolveToken(request);
            // 토큰이 존재하고, 유효한 토큰이면 인증 객체를 생성한 뒤, SecurityContextHolder 스레드에 등록
            if(token != null && jwtProvider.validateToken(token)){
                Authentication authentication = jwtProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        catch (SecurityException e){
            throw new SecurityException();
        }
        catch (MalformedJwtException e){
            throw new MalformedJwtException(e.getMessage());
        }
        catch(ExpiredJwtException e){
            throw new ExpiredJwtException(e.getHeader(), e.getClaims(), e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}
