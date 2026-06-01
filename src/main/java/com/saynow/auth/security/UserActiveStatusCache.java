// 사용자 활성 상태 조회 결과를 짧은 TTL로 보관하는 캐시
package com.saynow.auth.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongPredicate;

@Component
public class UserActiveStatusCache {

    private static final Duration DEFAULT_TTL = Duration.ofSeconds(60);

    private final Map<Long, CacheEntry> entries = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Duration ttl;

    public UserActiveStatusCache() {
        this(Clock.systemUTC(), DEFAULT_TTL);
    }

    UserActiveStatusCache(Clock clock, Duration ttl) {
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        this.clock = clock;
        this.ttl = ttl;
    }

    public boolean isActive(Long userId, LongPredicate lookup) {
        long now = clock.millis();
        CacheEntry cached = entries.get(userId);
        if (cached != null && cached.expiresAtMillis() > now) {
            return cached.active();
        }

        boolean active = lookup.test(userId);
        entries.put(userId, new CacheEntry(active, now + ttl.toMillis()));
        return active;
    }

    public void evict(Long userId) {
        entries.remove(userId);
    }

    private record CacheEntry(boolean active, long expiresAtMillis) {
    }
}
