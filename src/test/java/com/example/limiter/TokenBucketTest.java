package com.example.limiter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenBucketTest {

    private TokenBucket bucket;

    @BeforeEach
    void setUp() {
        // 每个测试方法执行前，都会创建一个新桶：容量5，每秒2个令牌
        bucket = new TokenBucket(5, 2.0);
    }

    @Test
    @DisplayName("初始化时桶应该是满的")
    void shouldStartWithFullBucket() {
        assertEquals(5.0, bucket.getAvailableTokens(), 0.001);
        assertEquals(5, bucket.getCapacity());
        assertEquals(2.0, bucket.getRate(), 0.001);
    }

    @Test
    @DisplayName("有令牌时 tryAcquire 返回 true，并消耗一个令牌")
    void shouldAcquireTokenWhenAvailable() {
        assertTrue(bucket.tryAcquire());
        assertEquals(4.0, bucket.getAvailableTokens(), 0.001);
    }

    @Test
    @DisplayName("令牌耗完后 tryAcquire 返回 false")
    void shouldReturnFalseWhenBucketEmpty() {
        // 消耗全部5个令牌
        for (int i = 0; i < 5; i++) {
            assertTrue(bucket.tryAcquire(), "第" + (i + 1) + "次应成功");
        }
        // 第6次应该失败（令牌已耗尽）
        assertFalse(bucket.tryAcquire());
        // 注意：不检查精确 token 数，因为 tryAcquire 和 getAvailableTokens 之间有微小时间流逝
        assertTrue(bucket.getAvailableTokens() < 1.0);
    }

    @Test
    @DisplayName("等待足够时间后令牌会自动补充")
    void shouldRefillOverTime() throws InterruptedException {
        // 消耗全部令牌
        for (int i = 0; i < 5; i++) {
            bucket.tryAcquire();
        }
        assertTrue(bucket.getAvailableTokens() < 1.0);

        // 等待1秒，每秒2个令牌 → 应该恢复至少1.9个
        Thread.sleep(1000);
        assertTrue(bucket.getAvailableTokens() >= 1.9);
    }

    @Test
    @DisplayName("令牌数不会超过容量上限")
    void shouldNotExceedCapacity() throws InterruptedException {
        // 消耗1个令牌
        bucket.tryAcquire();
        assertEquals(4.0, bucket.getAvailableTokens(), 0.001);

        // 等很久让令牌恢复
        Thread.sleep(5000);

        // 不能超过容量5
        double tokens = bucket.getAvailableTokens();
        assertTrue(tokens <= 5.0, "令牌数 " + tokens + " 超过了容量 5");
    }

    @Test
    @DisplayName("令牌不够时 getWaitSeconds 应返回正确的等待秒数")
    void shouldReturnCorrectWaitTime() {
        // 消耗全部令牌
        for (int i = 0; i < 5; i++) {
            bucket.tryAcquire();
        }
        // 0个令牌，速率2/秒，等1个令牌需要 ceil((1-0)/2) = 1秒
        assertEquals(1, bucket.getWaitSeconds());
    }

    @Test
    @DisplayName("有令牌时 getWaitSeconds 返回 0")
    void shouldReturnZeroWaitWhenTokensAvailable() {
        assertEquals(0, bucket.getWaitSeconds());
    }
}
