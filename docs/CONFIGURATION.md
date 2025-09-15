# ⚙️ 配置指南

本指南将详细介绍 Atlas Log 的所有配置选项，帮助您根据需要定制日志行为。

> **🆕 0.2.0 版本更新**：修复了注解配置中 HTTP 日志配置不生效的问题，现在 `@AtlasLogHttpLog` 注解配置完全可用！详见 [0.2.0 版本教程](TUTORIAL_V0.2.0.md)。

## 📖 目录

- [配置方式概览](#配置方式概览)
- [YAML 配置详解](#yaml-配置详解)
- [注解配置详解](#注解配置详解)
- [环境变量配置](#环境变量配置)
- [配置优先级](#配置优先级)
- [实际配置示例](#实际配置示例)

## 🎯 配置方式概览

Atlas Log 支持多种配置方式：

| 配置方式 | 使用场景 | 优先级 | 特点 |
|----------|----------|--------|------|
| 注解配置 | 企业级、类型安全 | 最高 | 编译时检查，IDE支持 |
| YAML配置 | 通用场景 | 高 | 灵活，环境隔离 |
| 环境变量 | 容器化部署 | 中 | 运行时覆盖 |
| 默认值 | 开箱即用 | 最低 | 无需配置 |

## 📄 YAML 配置详解

### 完整配置示例

```yaml
atlas:
  log:
    # === 基础配置 ===
    enabled: true                    # 是否启用日志记录
    default-level: INFO              # 默认日志级别
    spel-enabled: true               # 是否启用SpEL表达式
    condition-enabled: true          # 是否启用条件日志
    
    # === 标签和分组过滤 ===
    enabled-tags:                    # 启用的标签列表
      - "api"
      - "business"
      - "security"
    enabled-groups:                  # 启用的分组列表
      - "default"
      - "important"
    exclusions:                      # 排除的方法模式
      - "*.internal.*"
      - "*.test.*"
    
    # === 敏感数据脱敏 ===
    sensitive:
      enabled: true                  # 启用敏感数据脱敏
      custom-fields:                 # 自定义敏感字段
        - "password"
        - "pwd"
        - "secret"
        - "token"
        - "key"
        - "bankCard"
        - "creditCard"
        - "idCard"
        - "phone"
        - "mobile"
        - "email"
      mask-char: "*"                # 脱敏字符
      preserve-length: 4            # 保留字符长度
    
    # === 链路追踪 ===
    trace-id:
      enabled: true                  # 启用链路追踪
      header-name: "X-Trace-Id"     # HTTP头名称
      generator: "uuid"              # 生成器类型: uuid, snowflake, custom
      length: 32                     # TraceId长度（当generator为custom时）
    
    # === 性能监控 ===
    performance:
      enabled: true                  # 启用性能监控
      slow-threshold: 1000          # 慢方法阈值（毫秒）
      log-slow-methods: true        # 是否记录慢方法
      
    # === 序列化配置 ===
    serialization:
      max-depth: 10                 # 最大序列化深度
      date-format: "yyyy-MM-dd HH:mm:ss"  # 日期格式
      ignore-null-fields: true      # 忽略null字段
      pretty-print: false           # 是否格式化JSON
    
    # === 异常处理 ===
    exception:
      log-stack-trace: true         # 是否记录堆栈跟踪
      max-stack-trace-lines: 50     # 最大堆栈行数
      fail-safe: true               # 表达式异常时的安全模式
    
    # === Web集成 ===
    web:
      enabled: true                 # 启用Web集成
      log-requests: true            # 记录HTTP请求
      log-responses: false          # 记录HTTP响应
      exclude-patterns:             # 排除的URL模式
        - "/health"
        - "/metrics"
        - "/actuator/**"
```

### 基础配置详解

#### 核心开关

```yaml
atlas:
  log:
    enabled: true                    # 全局开关，false时完全禁用
    default-level: INFO              # 当注解未指定level时使用
    spel-enabled: true               # 禁用后所有SpEL表达式失效
    condition-enabled: true          # 禁用后所有condition条件失效
```

#### 过滤配置

```yaml
atlas:
  log:
    # 只记录包含这些标签的日志
    enabled-tags:
      - "api"          # API调用日志
      - "business"     # 业务逻辑日志
      - "audit"        # 审计日志
    
    # 只记录这些分组的日志
    enabled-groups:
      - "default"
      - "important"
    
    # 排除这些包/类的日志（支持通配符）
    exclusions:
      - "*.internal.*"              # 排除internal包
      - "com.example.test.*"        # 排除测试包
      - "*Controller.health*"       # 排除健康检查方法
```

### 敏感数据脱敏配置

```yaml
atlas:
  log:
    sensitive:
      enabled: true
      
      # 自定义敏感字段（不区分大小写）
      custom-fields:
        - "password"
        - "passwd"
        - "pwd"
        - "secret"
        - "token"
        - "accessToken"
        - "refreshToken"
        - "apiKey"
        - "privateKey"
        - "bankCard"
        - "creditCard"
        - "cardNumber"
        - "idCard"
        - "socialSecurity"
        - "phone"
        - "mobile"
        - "telephone"
        - "email"
        - "mail"
      
      mask-char: "*"                # 脱敏字符
      preserve-length: 4            # 前后保留字符数
      
      # 高级脱敏配置
      strategies:
        phone: "KEEP_FIRST_LAST_3"   # 手机号脱敏策略
        email: "KEEP_DOMAIN"         # 邮箱脱敏策略
        bankCard: "KEEP_FIRST_4_LAST_4"  # 银行卡脱敏策略
```

**脱敏效果示例：**
```json
{
  "password": "****",
  "phone": "138****8888", 
  "email": "user***@example.com",
  "bankCard": "6222****1234"
}
```

### 链路追踪配置

```yaml
atlas:
  log:
    trace-id:
      enabled: true
      header-name: "X-Trace-Id"     # HTTP请求头名称
      generator: "uuid"              # 生成器类型
      
      # UUID生成器配置
      uuid:
        remove-hyphens: true        # 是否移除连字符
      
      # 雪花算法生成器配置  
      snowflake:
        worker-id: 1                # 工作节点ID
        datacenter-id: 1            # 数据中心ID
      
      # 自定义生成器配置
      custom:
        length: 32                  # TraceId长度
        charset: "0123456789ABCDEF" # 字符集
```

### 性能监控配置

```yaml
atlas:
  log:
    performance:
      enabled: true
      slow-threshold: 1000          # 慢方法阈值（毫秒）
      log-slow-methods: true        # 记录慢方法详情
      
      # 性能统计
      statistics:
        enabled: true               # 启用性能统计
        window-size: 100           # 统计窗口大小
        percentiles: [50, 90, 95, 99]  # 百分位统计
      
      # 告警配置
      alerts:
        enabled: false              # 启用性能告警
        threshold-multiplier: 2.0   # 告警阈值倍数
```

### Web集成配置

```yaml
atlas:
  log:
    # === HTTP 日志配置 (0.2.0+ 完全支持) ===
    http-log:
      url-format: "{method} {uri}{queryString}"  # URL格式化模式
      include-query-string: true                   # 是否包含查询字符串
      log-full-parameters: true                    # 是否记录完整请求参数
      include-headers: false                       # 是否包含请求头
      exclude-headers:                             # 排除的请求头
        - "authorization"
        - "cookie"
        - "x-auth-token"
    
    web:
      enabled: true
      
      # 请求日志配置
      request:
        enabled: true
        log-headers: false          # 记录请求头
        log-body: true             # 记录请求体
        max-body-length: 1000      # 请求体最大长度
      
      # 响应日志配置  
      response:
        enabled: false
        log-headers: false          # 记录响应头
        log-body: false            # 记录响应体
        max-body-length: 1000      # 响应体最大长度
      
      # 排除模式
      exclude-patterns:
        - "/actuator/**"
        - "/health"
        - "/metrics"
        - "*.js"
        - "*.css"
        - "*.png"
        - "*.jpg"
        - "*.ico"
```

#### HTTP 日志 URL 格式化 (0.2.0+ 新功能)

**支持的占位符：**

| 占位符 | 说明 | 示例值 |
|--------|------|--------|
| `{method}` | HTTP 方法 | `GET`, `POST`, `PUT` |
| `{uri}` | 请求 URI | `/api/users/123` |
| `{queryString}` | 查询字符串 | `?name=john&age=25` |
| `{remoteAddr}` | 客户端 IP | `192.168.1.100` |

**配置示例：**

```yaml
# 1. 默认格式
atlas:
  log:
    http-log:
      url-format: "{method} {uri}"
# 输出: GET /api/users

# 2. 包含查询参数
atlas:
  log:
    http-log:
      url-format: "{method} {uri}{queryString}"
      include-query-string: true
# 输出: GET /api/users?id=123&name=john

# 3. 包含客户端IP
atlas:
  log:
    http-log:
      url-format: "[{remoteAddr}] {method} {uri}"
# 输出: [192.168.1.100] GET /api/users

# 4. 完整格式
atlas:
  log:
    http-log:
      url-format: "{remoteAddr} -> {method} {uri}{queryString}"
      include-query-string: true
# 输出: 192.168.1.100 -> GET /api/users?id=123

# 5. 只显示查询参数
atlas:
  log:
    http-log:
      url-format: "{queryString}"
      include-query-string: true
# 输出: ?id=123&name=john
```

## 🏗️ 注解配置详解

### @EnableAtlasLog 主配置

```java
@SpringBootApplication
@EnableAtlasLog(
    enabled = true,
    defaultLevel = "INFO",
    
    // HTTP 日志配置 (0.2.0+ 完全支持)
    httpLog = @AtlasLogHttpLog(
        urlFormat = "{remoteAddr} -> {method} {uri}{queryString}",
        includeQueryString = true,
        logFullParameters = true,
        includeHeaders = false,
        excludeHeaders = {"authorization", "cookie"}
    ),
    
    // 链路追踪配置
    trace = @AtlasLogTrace(
        enabled = true,
        headerName = "X-Request-Id",
        generator = "uuid"
    ),
    
    // 敏感数据配置
    sensitive = @AtlasLogSensitive(
        enabled = true,
        customFields = {"bankCard", "idCard", "socialSecurity"},
        maskChar = "*",
        preserveLength = 3
    ),
    
    // 性能监控配置
    performance = @AtlasLogPerformance(
        enabled = true,
        slowThreshold = 2000,
        logSlowMethods = true
    ),
    
    // 条件配置
    condition = @AtlasLogCondition(
        enabled = true,
        failSafe = true
    )
)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 子配置注解详解

#### @AtlasLogHttpLog - HTTP 日志配置 (0.2.0+ 新功能)

```java
@AtlasLogHttpLog(
    urlFormat = "{method} {uri}{queryString}",  // URL格式化模式
    includeQueryString = true,                   // 是否包含查询字符串
    logFullParameters = true,                    // 是否记录完整请求参数
    includeHeaders = false,                      // 是否包含请求头
    excludeHeaders = {"authorization", "cookie"} // 排除的请求头
)
```

**urlFormat 占位符说明：**
- `{method}`: HTTP 方法 (GET, POST, PUT, DELETE 等)
- `{uri}`: 请求 URI 路径
- `{queryString}`: 查询字符串 (需要 includeQueryString = true)
- `{remoteAddr}`: 客户端 IP 地址

**常用配置示例：**
```java
// 1. 生产环境配置（简洁）
@AtlasLogHttpLog(
    urlFormat = "{method} {uri}",
    includeQueryString = false,
    logFullParameters = false
)

// 2. 开发环境配置（详细）
@AtlasLogHttpLog(
    urlFormat = "{remoteAddr} -> {method} {uri}{queryString}",
    includeQueryString = true,
    logFullParameters = true
)

// 3. 只记录查询参数
@AtlasLogHttpLog(
    urlFormat = "{queryString}",
    includeQueryString = true
)
```

#### @AtlasLogTrace - 链路追踪配置

```java
@AtlasLogTrace(
    enabled = true,                 // 是否启用
    headerName = "X-Trace-Id",     // HTTP头名称
    generator = "uuid",            // 生成器类型
    length = 32                    // 自定义长度
)
```

#### @AtlasLogSensitive - 敏感数据配置

```java
@AtlasLogSensitive(
    enabled = true,                // 是否启用脱敏
    customFields = {               // 自定义敏感字段
        "password", "token", "secret",
        "bankCard", "creditCard", "idCard"
    },
    maskChar = "*",               // 脱敏字符
    preserveLength = 4            // 保留长度
)
```

#### @AtlasLogPerformance - 性能监控配置

```java
@AtlasLogPerformance(
    enabled = true,               // 是否启用性能监控
    slowThreshold = 1000,         // 慢方法阈值（毫秒）
    logSlowMethods = true,        // 是否记录慢方法
    statisticsEnabled = true      // 是否启用统计
)
```

#### @AtlasLogCondition - 条件配置

```java
@AtlasLogCondition(
    enabled = true,               // 是否启用条件日志
    failSafe = true              // 表达式异常时的安全模式
)
```

### 模块化配置示例

```java
// 开发环境配置
@Profile("dev")
@Configuration
public class DevLogConfig {
    
    @Bean
    @Primary
    public LogConfigProperties devLogConfig() {
        LogConfigProperties config = new LogConfigProperties();
        config.setDefaultLevel(LogLevel.DEBUG);
        config.getEnabledTags().addAll(Arrays.asList("debug", "dev"));
        return config;
    }
}

// 生产环境配置
@Profile("prod")  
@Configuration
public class ProdLogConfig {
    
    @Bean
    @Primary
    public LogConfigProperties prodLogConfig() {
        LogConfigProperties config = new LogConfigProperties();
        config.setDefaultLevel(LogLevel.INFO);
        config.getEnabledTags().addAll(Arrays.asList("business", "audit"));
        return config;
    }
}
```

## 🌍 环境变量配置

所有 YAML 配置都可以通过环境变量覆盖：

```bash
# 基础配置
export ATLAS_LOG_ENABLED=true
export ATLAS_LOG_DEFAULT_LEVEL=INFO

# 敏感数据配置
export ATLAS_LOG_SENSITIVE_ENABLED=true
export ATLAS_LOG_SENSITIVE_CUSTOM_FIELDS=password,token,secret

# 链路追踪配置
export ATLAS_LOG_TRACE_ID_ENABLED=true
export ATLAS_LOG_TRACE_ID_HEADER_NAME=X-Request-Id

# 性能监控配置
export ATLAS_LOG_PERFORMANCE_ENABLED=true
export ATLAS_LOG_PERFORMANCE_SLOW_THRESHOLD=2000
```

### Docker 容器配置示例

```dockerfile
# Dockerfile
FROM openjdk:8-jre-alpine
COPY app.jar /app.jar

# 设置环境变量
ENV ATLAS_LOG_ENABLED=true
ENV ATLAS_LOG_DEFAULT_LEVEL=INFO
ENV ATLAS_LOG_TRACE_ID_ENABLED=true

ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```yaml
# docker-compose.yml
version: '3.8'
services:
  app:
    image: myapp:latest
    environment:
      - ATLAS_LOG_ENABLED=true
      - ATLAS_LOG_DEFAULT_LEVEL=INFO
      - ATLAS_LOG_SENSITIVE_ENABLED=true
      - ATLAS_LOG_TRACE_ID_HEADER_NAME=X-Request-Id
```

## 🎯 配置优先级

配置优先级从高到低：

1. **注解配置** - 编译时确定，类型安全
2. **YAML 配置** - 应用启动时加载
3. **环境变量** - 运行时覆盖
4. **默认值** - 框架内置默认值

### 优先级示例

```java
// 1. 注解配置（优先级最高）
@EnableAtlasLog({
    @AtlasLogPerformance(slowThreshold = 500)
})
```

```yaml
# 2. YAML配置
atlas:
  log:
    performance:
      slow-threshold: 1000  # 被注解配置覆盖
```

```bash
# 3. 环境变量
export ATLAS_LOG_PERFORMANCE_SLOW_THRESHOLD=2000  # 被注解和YAML配置覆盖
```

```java
// 4. 默认值（优先级最低）
// slowThreshold = 1000（框架默认）
```

**最终生效值：500（注解配置）**

## 📋 实际配置示例

### 微服务架构配置

```yaml
# application.yml - 微服务通用配置
atlas:
  log:
    enabled: true
    default-level: INFO
    
    # 服务追踪
    trace-id:
      enabled: true
      header-name: "X-Trace-Id"
    
    # 敏感数据保护
    sensitive:
      enabled: true
      custom-fields: ["password", "token", "apiKey"]
    
    # 只记录重要日志
    enabled-tags: ["api", "business", "audit"]
    
    # 排除健康检查等
    exclusions: 
      - "*/actuator/*"
      - "*/health"
```

### 开发环境配置

```yaml
# application-dev.yml
atlas:
  log:
    default-level: DEBUG
    enabled-tags: ["debug", "dev", "api", "business"]
    
    # 开发时记录更详细信息
    performance:
      slow-threshold: 100  # 更低的阈值便于发现问题
    
    # 开发时不脱敏，便于调试
    sensitive:
      enabled: false
```

### 生产环境配置

```yaml
# application-prod.yml  
atlas:
  log:
    default-level: INFO
    enabled-tags: ["business", "audit", "error"]
    
    # 生产环境性能配置
    performance:
      slow-threshold: 2000
      log-slow-methods: true
    
    # 生产环境严格脱敏
    sensitive:
      enabled: true
      custom-fields: 
        - "password"
        - "token" 
        - "secret"
        - "bankCard"
        - "idCard"
        - "phone"
        - "email"
```

### 高并发场景配置

```yaml
atlas:
  log:
    # 减少日志量，提升性能
    enabled-tags: ["error", "business"]
    
    # 限制序列化长度
    serialization:
      max-depth: 3
      ignore-null-fields: true
    
    # 只记录慢方法和异常
    performance:
      slow-threshold: 5000
    
    # 排除高频接口
    exclusions:
      - "*/api/heartbeat"
      - "*/api/metrics"
```

## 🔧 配置验证和调试

### 启用配置验证

```yaml
atlas:
  log:
    # 启用配置验证（开发环境推荐）
    config-validation:
      enabled: true
      strict-mode: true  # 严格模式，配置错误时启动失败
```

### 配置调试输出

```yaml
# 启用调试日志查看配置加载过程
logging:
  level:
    io.github.nemoob.atlas.log.config: DEBUG
```

### 配置健康检查

```java
@RestController
public class LogConfigController {
    
    @Autowired
    private LogConfigProperties logConfig;
    
    @GetMapping("/log/config")
    public LogConfigProperties getLogConfig() {
        return logConfig;  // 查看当前生效的配置
    }
}
```

## 🎯 下一步

现在您已经掌握了完整的配置选项，可以继续学习：

- 🔍 [SpEL 表达式指南](SPEL_GUIDE.md) - 学习动态表达式
- 🛡️ [敏感数据保护](SENSITIVE_DATA.md) - 深入了解数据安全
- ⚡ [性能优化指南](PERFORMANCE_GUIDE.md) - 优化日志性能
- 💼 [企业实践指南](ENTERPRISE_GUIDE.md) - 企业级配置实践

---

**您现在已经掌握了 Atlas Log 的完整配置！🎉**