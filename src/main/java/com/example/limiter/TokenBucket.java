package com.example.limiter;

/**
 * 令牌桶限流器
 *
 * 核心思想（面试时这样讲）：
 *   一个桶，最多装 capacity 个令牌。
 *   每秒钟往桶里放 rate 个令牌（桶满了就丢弃）。
 *   每个请求来的时候，尝试从桶里拿 1 个令牌：
 *     → 拿到了：请求放行
 *     → 没拿到：请求被限流，返回 429
 */
public class TokenBucket {

    private final int capacity;       // 桶最大容量（令牌个数）
    private final double rate;        // 每秒放入令牌的速率
    private double tokens;            // 当前桶里有多少令牌
    private long lastRefillTime;      // 上一次补充令牌的时间（纳秒）

    /**
     * @param capacity  桶最大容量，比如 10 个
     * @param rate      每秒产生的令牌数，比如每秒 2 个
     */
    public TokenBucket(int capacity, double rate) {
        this.capacity = capacity;
        this.rate = rate;
        this.tokens = capacity;              // 初始化时桶是满的
        this.lastRefillTime = System.nanoTime();
    }

    /**
     * 尝试获取 1 个令牌
     * @return true = 拿到了，请求放行；false = 没拿到，限流
     */
    public synchronized boolean tryAcquire() {
        refill();  // 先补充令牌，再尝试获取

        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;   // 拿到令牌
        }
        return false;      // 令牌不够，限流
    }

    /**
     * 根据时间流逝，往桶里补充令牌
     */
    private void refill() {
        long now = System.nanoTime();
        double elapsedSeconds = (now - lastRefillTime) / 1_000_000_000.0;

        // 计算这段时间产生了多少新令牌
        double newTokens = elapsedSeconds * rate;
        tokens = Math.min(capacity, tokens + newTokens);

        lastRefillTime = now;
    }

    // ---------- 获取状态信息，用于演示 ----------

    public int getCapacity() { return capacity; }
    public double getRate() { return rate; }
    public synchronized double getAvailableTokens() {
        refill();
        return tokens;
    }
}
