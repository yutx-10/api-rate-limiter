# API Rate Limiter

> 基于 Spring Boot 的令牌桶限流器，用于保护 API 接口免受过量请求冲击。

## 项目概述

本系统使用**令牌桶算法（Token Bucket）**实现 API 限流。每个请求在到达 Controller 之前被拦截器检查，拿到令牌则放行，拿不到则返回 `429 Too Many Requests`。

**亮点特性**：
- 令牌桶算法，支持突发流量
- 按客户端 IP 独立限流，互不影响
- 配置外部化，修改限流策略无需重新编译
- 标准 HTTP 响应头（`X-RateLimit-*`），调用方可感知配额状态
- 完整的 JUnit 5 单元测试

## 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Java 21 |
| 框架 | Spring Boot 3.4.5 |
| 构建 | Maven |
| 测试 | JUnit 5 |
| 运行环境 | 内嵌 Tomcat，端口 8080 |

## 项目结构

```
api-rate-limiter/
├── pom.xml                                  # Maven 依赖管理
├── README.md
└── src/
    ├── main/
    │   ├── java/com/example/
    │   │   ├── RateLimiterApplication.java   # Spring Boot 启动入口
    │   │   ├── controller/
    │   │   │   └── HelloController.java       # REST 接口层
    │   │   └── limiter/
    │   │       ├── TokenBucket.java            # 令牌桶算法核心
    │   │       ├── RateLimitInterceptor.java   # 限流拦截器
    │   │       └── WebConfig.java              # 拦截器注册 + 配置注入
    │   └── resources/
    │       └── application.properties          # 应用配置
    └── test/
        └── java/com/example/limiter/
            └── TokenBucketTest.java            # 令牌桶单元测试
```

## 架构流程

```
HTTP 请求
    │
    ▼
┌─────────────────────────┐
│  Tomcat (接收请求)        │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│  RateLimitInterceptor    │  ← 提取客户端 IP
│  (preHandle 方法)        │  ← 查找/创建该 IP 的令牌桶
│                         │  ← 写入 X-RateLimit-* 响应头
│    tryAcquire()?         │
│    ├── true → 放行 ────→ HelloController (返回业务数据)
│    └── false → 拦截 ──→ HTTP 429 + 错误 JSON
└─────────────────────────┘
```

## 核心设计

### 令牌桶算法

```
令牌以固定速率（rate/秒）放入桶中
桶有最大容量（capacity），满了多余的令牌丢弃
请求到来时从桶中取出 1 个令牌
  → 取到：放行
  → 取不到：限流
```

**和固定窗口的对比**：令牌桶允许短时间突发流量。比如容量 10、速率 2/秒，桶满时可以瞬间处理 10 个请求，之后以每秒 2 个的速率恢复。固定窗口做不到这个。

### 按 IP 限流

```
ConcurrentHashMap<String, TokenBucket>
    │
    ├── "192.168.1.10"  →  TokenBucket(capacity=10, rate=2)
    ├── "10.0.0.5"      →  TokenBucket(capacity=10, rate=2)
    └── "172.16.0.1"    →  TokenBucket(capacity=10, rate=2)
```

每个 IP 独立拥有一个令牌桶，某 IP 超额不会影响其他 IP。

## 快速开始

### 1. 启动应用

```bash
mvn spring-boot:run
```

### 2. 测试不限流接口

```bash
curl http://localhost:8080/api/hello
# → "Hello, API Rate Limiter is running!"
```

### 3. 测试限流接口

```bash
# 连续快速发送请求
for i in {1..15}; do
  curl -i http://localhost:8080/api/limited 2>/dev/null | head -1
done

# 前10次返回 200 OK（令牌充足，容量10）
# 后面返回 429 Too Many Requests（令牌耗尽）
```

### 4. 查看限流响应头

```bash
curl -i http://localhost:8080/api/limited

# HTTP/1.1 200
# X-RateLimit-Limit: 10
# X-RateLimit-Remaining: 9
```

### 5. 运行单元测试

```bash
mvn test
```

## API 接口

| 接口 | 方法 | 限流 | 说明 |
|------|------|------|------|
| `/api/hello` | GET | ❌ | 测试接口，不受限流 |
| `/api/limited` | GET | ✅ | 受限流保护的接口 |

## 限流配置

在 `application.properties` 中修改：

```properties
# 令牌桶最大容量（允许的最大突发请求数）
rate-limiter.capacity=10

# 每秒产生令牌数（稳定时的 QPS）
rate-limiter.rate=2
```

## 限流响应头

| 响应头 | 含义 |
|--------|------|
| `X-RateLimit-Limit` | 桶的最大容量 |
| `X-RateLimit-Remaining` | 当前剩余令牌数 |
| `X-RateLimit-Reset` | 被限流后需等待的秒数（仅 429 时返回） |

## 错误响应格式

```json
{
  "code": 429,
  "message": "请求过多，请稍后重试"
}
```

## 面试要点

**这个项目的技术决策**：

| 决策 | 选型 | 原因 |
|------|------|------|
| 算法 | 令牌桶 | 支持突发流量，比固定窗口更贴近业务场景 |
| 集成方式 | Spring Interceptor | 无侵入，不修改业务 Controller |
| IP 粒度 | ConcurrentHashMap | 线程安全，每个 IP 独立配额 |
| 配置 | application.properties | 改配置不用改代码，运维友好 |
| 响应头 | X-RateLimit-* | 业界标准，调用方可感知限流状态 |

**可以从哪些方向扩展**：
- Redis + Lua 脚本实现分布式限流
- 引入配置中心（Nacos）实现动态调参
- 接入 Prometheus + Grafana 监控限流指标
- 支持更多策略：固定窗口、滑动窗口、漏桶
