package com.mocamp.mocamp_backend.repository;

import com.mocamp.mocamp_backend.authentication.JwtProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
public class TokenRepository {

    private final long REFRESH_TOKEN_EXPIRE = JwtProvider.REFRESH_TOKEN_EXPIRE;
    private final String REFRESH_TOKEN_PREFIX = "refreshToken:";

    private RedisTemplate<String, String> redisTemplate;
    private ValueOperations<String, String> valueOperations;

    public TokenRepository(final RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.valueOperations = redisTemplate.opsForValue();
    }

    /**
     * Redis에 유저 ID를 key로 refreshToken을 value로 저장하는 메서드
     * @param userId 유저 ID
     * @param refreshToken 리프레쉬 토큰
     */
    public void save(Long userId, String refreshToken) {
        valueOperations.set(REFRESH_TOKEN_PREFIX + userId, refreshToken);
        redisTemplate.expire(REFRESH_TOKEN_PREFIX + userId, REFRESH_TOKEN_EXPIRE, TimeUnit.SECONDS);
    }

    /**
     * 유저 ID로 리프레쉬 토큰 조회하는 메서드
     * @param userId 유저 ID
     * @return 리프레쉬 토큰
     */
    public String findById(Long userId) {
        return valueOperations.get(REFRESH_TOKEN_PREFIX + userId);
    }

    /**
     * 유저 ID로 리프레쉬 토큰 삭제하는 메서드
     * @param userId 유저 ID
     */
    public void deleteById(Long userId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
    }

}
