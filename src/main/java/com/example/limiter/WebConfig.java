package com.example.limiter;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 只对 /api/limited 接口生效，/api/hello 不受影响
        registry.addInterceptor(new RateLimitInterceptor())
                .addPathPatterns("/api/limited");
    }
}
