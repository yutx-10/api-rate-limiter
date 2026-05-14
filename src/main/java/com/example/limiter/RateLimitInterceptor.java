package com.example.limiter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;

public class RateLimitInterceptor implements HandlerInterceptor {

    private final int capacity;
    private final double rate;

    // 为每个 IP 维护独立的令牌桶
    // key = IP 地址, value = 属于这个 IP 的 TokenBucket
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitInterceptor(int capacity, double rate) {
        this.capacity = capacity;
        this.rate = rate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String ip = getClientIP(request);

        // computeIfAbsent：如果这个 IP 还没有桶，就创建一个新的给它
        TokenBucket bucket = buckets.computeIfAbsent(ip,
                k -> new TokenBucket(capacity, rate));

        response.setHeader("X-RateLimit-Limit", String.valueOf(bucket.getCapacity()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf((int) bucket.getAvailableTokens()));

        if (bucket.tryAcquire()) {
            return true;
        } else {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Reset", String.valueOf(bucket.getWaitSeconds()));
            response.getWriter().write(
                "{\"code\":429,\"message\":\"请求过多，请稍后重试\"}"
            );
            return false;
        }
    }

    private String getClientIP(HttpServletRequest request) {
        // 如果有代理/负载均衡，优先取 X-Forwarded-For
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        // 如果 X-Forwarded-For 里有多个 IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
