package com.example.limiter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 限流拦截器
 *
 * 在请求到达 Controller 之前拦截，检查令牌：
 *   → 有令牌：放行，调用 preHandle 返回 true
 *   → 没令牌：拦截，返回 429 Too Many Requests
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    // 全局唯一的令牌桶：容量 10，每秒产生 2 个令牌
    private final TokenBucket bucket = new TokenBucket(10, 2.0);

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if (bucket.tryAcquire()) {
            // 拿到令牌 → 放行到 Controller
            return true;
        } else {
            // 没拿到令牌 → 返回 429
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"code\":429,\"message\":\"请求过多，请稍后重试\"}"
            );
            return false;  // false = 拦截，不再往下走
        }
    }
}
