package com.example.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    // 普通接口：不做限流，永远返回 OK
    @GetMapping("/api/hello")
    public String hello() {
        return "Hello, API Rate Limiter is running!";
    }

    // 被限流保护的接口：后面会加上限流
    @GetMapping("/api/limited")
    public String limited() {
        return "Request allowed — you got a token!";
    }
}
