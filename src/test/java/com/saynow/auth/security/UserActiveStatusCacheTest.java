// 사용자 활성 상태 TTL 캐시 동작을 검증한다.
package com.saynow.auth.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongPredicate;

import static org.assertj.core.api.Assertions.assertThat;

class UserActiveStatusCacheTest {

    @Test
    void cachesActiveStatusUntilTtlExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-31T00:00:00Z"));
        UserActiveStatusCache cache = new UserActiveStatusCache(clock, Duration.ofSeconds(60));
        AtomicInteger lookupCount = new AtomicInteger();
        LongPredicate lookup = userId -> {
            lookupCount.incrementAndGet();
            return true;
        };

        assertThat(cache.isActive(1L, lookup)).isTrue();
        assertThat(cache.isActive(1L, lookup)).isTrue();

        assertThat(lookupCount).hasValue(1);

        clock.advance(Duration.ofSeconds(61));

        assertThat(cache.isActive(1L, lookup)).isTrue();
        assertThat(lookupCount).hasValue(2);
    }

    @Test
    void cachesInactiveStatusUntilTtlExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-31T00:00:00Z"));
        UserActiveStatusCache cache = new UserActiveStatusCache(clock, Duration.ofSeconds(60));
        AtomicInteger lookupCount = new AtomicInteger();
        LongPredicate lookup = userId -> {
            lookupCount.incrementAndGet();
            return false;
        };

        assertThat(cache.isActive(2L, lookup)).isFalse();
        assertThat(cache.isActive(2L, lookup)).isFalse();

        assertThat(lookupCount).hasValue(1);
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
