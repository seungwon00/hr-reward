package com.seungwon.hrreward.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

// Refresh Token 저장소 (Redis)
//   auth:refresh:{tokenHash} = userId          TTL = refresh 만료
//   auth:user:{userId}       = Set{tokenHash}  TTL = 가장 최근 발급 토큰의 만료에 맞춰 갱신
// 사용자별 Set은 비밀번호 변경·탈퇴 시 전체 기기 로그아웃용. 만료된 토큰 해시가 Set에 남을 수 있지만
// 전체 로그아웃 때 없는 키를 DEL하는 것은 무해하고, Set 자체도 마지막 토큰과 함께 만료된다.
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private static final String TOKEN_KEY_PREFIX = "auth:refresh:";
    private static final String USER_KEY_PREFIX = "auth:user:";

    private final StringRedisTemplate redisTemplate;

    public void save(String tokenHash, Long userId, Duration ttl) {
        String userKey = userKey(userId);
        redisTemplate.execute(new SessionCallback<>() {
            @Override
            @SuppressWarnings("unchecked")
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                operations.opsForValue().set(tokenKey(tokenHash), String.valueOf(userId), ttl);
                operations.opsForSet().add(userKey, tokenHash);
                operations.expire(userKey, ttl);
                return operations.exec();
            }
        });
    }

    // GETDEL로 꺼내면서 삭제한다. 같은 토큰으로 동시에 요청해도 한 요청만 userId를 받는다.
    public Optional<Long> consume(String tokenHash) {
        String userId = redisTemplate.opsForValue().getAndDelete(tokenKey(tokenHash));
        if (userId == null) {
            return Optional.empty();
        }
        redisTemplate.opsForSet().remove(userKey(Long.valueOf(userId)), tokenHash);
        return Optional.of(Long.valueOf(userId));
    }

    public Optional<Long> findUserId(String tokenHash) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(tokenKey(tokenHash)))
                .map(Long::valueOf);
    }

    public void delete(String tokenHash, Long userId) {
        redisTemplate.execute(new SessionCallback<>() {
            @Override
            @SuppressWarnings("unchecked")
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                operations.delete(tokenKey(tokenHash));
                operations.opsForSet().remove(userKey(userId), tokenHash);
                return operations.exec();
            }
        });
    }

    private static String tokenKey(String tokenHash) {
        return TOKEN_KEY_PREFIX + tokenHash;
    }

    private static String userKey(Long userId) {
        return USER_KEY_PREFIX + userId;
    }
}
