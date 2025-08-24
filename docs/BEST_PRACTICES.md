# 📋 最佳实践指南

本指南总结了使用 Atlas Log 的最佳实践，帮助您在项目中高效、安全地使用日志框架。

## 📖 目录

- [设计原则](#设计原则)
- [注解使用最佳实践](#注解使用最佳实践)
- [性能优化实践](#性能优化实践)
- [安全实践](#安全实践)
- [配置管理实践](#配置管理实践)
- [团队协作实践](#团队协作实践)
- [生产环境实践](#生产环境实践)
- [监控和运维实践](#监控和运维实践)

## 🎯 设计原则

### 1. 最小侵入原则

✅ **推荐：简洁的注解使用**
```java
@Log("#{methodName}: #{args[0]}")
public User getUserById(Long id) {
    return userRepository.findById(id);
}
```

❌ **不推荐：复杂的注解配置**
```java
@Log(
    value = "用户查询",
    level = LogLevel.INFO,
    tags = {"api", "user", "query", "database", "service"},
    logArgs = true,
    logResult = true,
    logExecutionTime = true,
    maxArgLength = 1000,
    maxResultLength = 1000,
    enterMessage = "开始查询用户",
    exitMessage = "查询用户完成"
)
public User getUserById(Long id) {
    return userRepository.findById(id);
}
```

### 2. 关注点分离原则

✅ **推荐：将配置与业务逻辑分离**
```java
// 业务代码保持简洁
@Log("#{methodName}: #{args[0]}")
public User createUser(User user) {
    return userService.create(user);
}
```

```yaml
# 配置文件中管理全局设置
atlas:
  log:
    default-level: INFO
    enabled-tags: ["api", "business"]
    sensitive:
      enabled: true
```

### 3. 一致性原则

✅ **推荐：统一的日志消息格式**
```java
@Service
public class UserService {
    
    @Log("#{methodName}: #{args[0]}")
    public User getUser(Long id) { }
    
    @Log("#{methodName}: #{args[0]}")  
    public User createUser(User user) { }
    
    @Log("#{methodName}: #{args[0]}")
    public void deleteUser(Long id) { }
}
```

## 📝 注解使用最佳实践

### 1. 合理使用日志级别

```java
@Service
public class OrderService {
    
    // 普通业务操作 - INFO
    @Log(value = "创建订单", level = LogLevel.INFO)
    public Order createOrder(OrderRequest request) { }
    
    // 重要业务操作 - WARN  
    @Log(value = "取消订单", level = LogLevel.WARN)
    public void cancelOrder(Long orderId) { }
    
    // 错误恢复操作 - ERROR
    @Log(value = "订单回滚", level = LogLevel.ERROR)
    public void rollbackOrder(Long orderId) { }
    
    // 调试信息 - DEBUG
    @Log(value = "订单状态检查", level = LogLevel.DEBUG)
    public OrderStatus checkOrderStatus(Long orderId) { }
}
```

### 2. 智能使用标签分类

```java
@RestController
public class ApiController {
    
    // API层：使用 api 标签
    @Log(value = "用户API", tags = {"api", "user"})
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) { }
    
    // 支付相关：使用 payment 标签
    @Log(value = "支付API", tags = {"api", "payment", "business"})
    @PostMapping("/payments")
    public PaymentResult payment(@RequestBody PaymentRequest request) { }
    
    // 安全相关：使用 security 标签
    @Log(value = "登录API", tags = {"api", "security", "auth"})
    @PostMapping("/login")
    public LoginResult login(@RequestBody LoginRequest request) { }
}
```

### 3. 动态消息模板设计

```java
public class MessageTemplates {
    
    // ✅ 推荐：信息量丰富且简洁
    @Log("#{methodName}: 用户=#{args[0]}, 操作=#{args[1]}")
    public void userOperation(Long userId, String operation) { }
    
    // ✅ 推荐：包含业务关键信息
    @Log("订单处理: 订单=#{args[0].orderId}, 金额=#{args[0].amount}")
    public PaymentResult processPayment(PaymentRequest request) { }
    
    // ✅ 推荐：条件信息展示
    @Log("批量操作: #{args[0].size()}条记录")
    public void batchProcess(List<Record> records) { }
    
    // ❌ 不推荐：信息过少
    @Log("处理数据")
    public void processData(ComplexData data) { }
    
    // ❌ 不推荐：信息过多  
    @Log("处理数据: #{args[0].field1}, #{args[0].field2}, #{args[0].field3}...")
    public void processData(ComplexData data) { }
}
```

### 4. 条件日志的合理使用

```java
@Service  
public class BusinessService {
    
    // ✅ 推荐：基于业务条件
    @Log(
        value = "VIP用户操作", 
        condition = "#{@userService.isVip(args[0])}"
    )
    public void vipOperation(Long userId) { }
    
    // ✅ 推荐：基于配置开关
    @Log(
        value = "调试操作",
        condition = "#{@configService.isDebugEnabled()}"  
    )
    public void debugOperation() { }
    
    // ✅ 推荐：基于环境条件
    @Log(
        value = "开发环境操作",
        condition = "#{environment.getProperty('spring.profiles.active') == 'dev'}"
    )
    public void devOnlyOperation() { }
    
    // ❌ 不推荐：复杂的条件逻辑
    @Log(
        value = "复杂条件",
        condition = "#{args[0] > 100 and args[1].contains('test') and @service.check()}"
    )
    public void complexCondition(int value, String text) { }
}
```

## ⚡ 性能优化实践

### 1. 控制序列化开销

```java
@Service
public class DataService {
    
    // ✅ 推荐：限制大对象的序列化
    @Log(
        value = "处理大数据集",
        logArgs = false,  // 不序列化大参数
        maxResultLength = 200
    )
    public Result processLargeDataSet(List<LargeObject> data) { }
    
    // ✅ 推荐：选择性记录参数
    @Log(
        value = "文件上传: 文件大小=#{args[0].length}",
        excludeArgs = {0}  // 排除文件内容
    )
    public void uploadFile(byte[] fileContent, String fileName) { }
    
    // ✅ 推荐：记录摘要信息而非完整对象
    @Log("批量处理: #{args[0].size()}条记录")
    public void batchProcess(List<Record> records) { }
}
```

### 2. 优化SpEL表达式

```java
// ✅ 推荐：简单直接的表达式
@Log("用户操作: #{args[0]}")
public void simpleOperation(Long userId) { }

// ✅ 推荐：缓存复杂计算
@Component
public class LogHelpers {
    
    @Cacheable("userTypes")
    public String getUserType(Long userId) {
        return userService.getUserType(userId);
    }
}

@Log("#{@logHelpers.getUserType(args[0])}用户操作")
public void operationWithUserType(Long userId) { }

// ❌ 不推荐：复杂的内联计算
@Log("用户操作: #{@userService.getUser(args[0]).getType().getName().toUpperCase()}")
public void complexExpression(Long userId) { }
```

### 3. 使用异步日志

```yaml
# logback-spring.xml 配置异步appender
<configuration>
    <appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="CONSOLE"/>
        <queueSize>1024</queueSize>
        <discardingThreshold>0</discardingThreshold>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="ASYNC"/>
    </root>
</configuration>
```

### 4. 合理的过滤策略

```yaml
atlas:
  log:
    # 只记录重要的标签
    enabled-tags: ["business", "security", "error"]
    
    # 排除高频但不重要的操作
    exclusions:
      - "*.HealthController.*"
      - "*.MetricsController.*"
      - "*.internal.*"
    
    # 控制序列化深度
    serialization:
      max-depth: 3
      ignore-null-fields: true
```

## 🛡️ 安全实践

### 1. 敏感数据保护

```java
@Service
public class AuthService {
    
    // ✅ 推荐：排除敏感参数
    @Log(
        value = "用户登录: #{args[0]}",
        excludeArgs = {1}  // 排除密码
    )
    public boolean login(String username, String password) { }
    
    // ✅ 推荐：不记录敏感返回值
    @Log(
        value = "生成令牌",
        logResult = false  // 不记录token
    )
    public String generateToken(Long userId) { }
    
    // ✅ 推荐：使用 @LogIgnore 注解
    @Log("重置密码")
    public void resetPassword(
        Long userId, 
        @LogIgnore String oldPassword,
        @LogIgnore String newPassword
    ) { }
}
```

### 2. 完善的脱敏配置

```yaml
atlas:
  log:
    sensitive:
      enabled: true
      custom-fields:
        # 密码相关
        - "password"
        - "passwd"  
        - "pwd"
        - "secret"
        - "token"
        - "accessToken"
        - "refreshToken"
        - "apiKey"
        - "privateKey"
        
        # 个人信息
        - "idCard"
        - "socialSecurity"
        - "bankCard"
        - "creditCard"
        - "phone"
        - "mobile"
        - "email"
        
        # 业务敏感字段
        - "salary"
        - "income"
        - "balance"
```

### 3. 权限相关的日志记录

```java
@Service
public class SecurityService {
    
    // ✅ 记录权限检查结果但不泄露详情
    @Log(
        value = "权限检查: 用户=#{args[0]}, 资源=#{args[1]}, 结果=#{result}",
        level = LogLevel.WARN,
        tags = {"security", "permission"}
    )
    public boolean checkPermission(Long userId, String resource) { }
    
    // ✅ 记录安全操作但保护敏感信息
    @Log(
        value = "密码策略检查",
        logArgs = false,
        logResult = false,
        tags = {"security", "password"}
    )
    public boolean validatePasswordPolicy(String password) { }
}
```

## ⚙️ 配置管理实践

### 1. 环境差异化配置

```yaml
# application.yml - 基础配置
atlas:
  log:
    enabled: true
    trace-id:
      enabled: true

# application-dev.yml - 开发环境
atlas:
  log:
    default-level: DEBUG
    enabled-tags: ["debug", "dev", "api", "business"]
    sensitive:
      enabled: false  # 开发环境不脱敏便于调试
    
# application-test.yml - 测试环境  
atlas:
  log:
    default-level: INFO
    enabled-tags: ["api", "business", "test"]
    sensitive:
      enabled: true
      
# application-prod.yml - 生产环境
atlas:
  log:
    default-level: INFO  
    enabled-tags: ["business", "security", "error"]
    sensitive:
      enabled: true
    performance:
      slow-threshold: 2000
```

### 2. 模块化配置

```java
// 基础配置
@Configuration
public class LogBaseConfig {
    
    @Bean
    @ConditionalOnProperty(value = "atlas.log.custom.enabled", havingValue = "true")
    public CustomLogProcessor customLogProcessor() {
        return new CustomLogProcessor();
    }
}

// 开发环境特殊配置
@Profile("dev")
@Configuration  
public class DevLogConfig {
    
    @Bean
    @Primary
    public LogConfigProperties devLogProperties() {
        LogConfigProperties config = new LogConfigProperties();
        config.setDefaultLevel(LogLevel.DEBUG);
        return config;
    }
}

// 生产环境特殊配置
@Profile("prod")
@Configuration
public class ProdLogConfig {
    
    @Bean
    @Primary
    public LogConfigProperties prodLogProperties() {
        LogConfigProperties config = new LogConfigProperties();
        config.setDefaultLevel(LogLevel.INFO);
        config.getEnabledTags().addAll(Arrays.asList("business", "error"));
        return config;
    }
}
```

## 👥 团队协作实践

### 1. 统一的代码规范

```java
// 团队约定的注解使用规范
@Service
public class TeamServiceExample {
    
    // 规范1：使用统一的消息模板
    @Log("#{methodName}: #{args[0]}")
    public User getUser(Long id) { }
    
    // 规范2：重要操作使用WARN级别
    @Log(value = "#{methodName}: #{args[0]}", level = LogLevel.WARN)
    public void deleteUser(Long id) { }
    
    // 规范3：敏感操作统一标签
    @Log(
        value = "#{methodName}",
        tags = {"security", "sensitive"},
        logArgs = false
    )
    public String generateSecret() { }
}
```

### 2. 代码审查检查点

**Code Review 检查清单：**
- [ ] 敏感参数是否被排除？
- [ ] 日志级别是否合适？
- [ ] 标签分类是否正确？
- [ ] SpEL表达式是否简洁？
- [ ] 是否有性能影响？

### 3. 文档化实践

```java
/**
 * 用户服务
 * 
 * 日志策略：
 * - 查询操作：INFO级别，记录参数和结果
 * - 修改操作：WARN级别，记录参数但不记录完整用户对象
 * - 删除操作：ERROR级别，完整记录操作信息
 * 
 * 标签约定：
 * - user: 用户相关操作
 * - crud: 增删改查操作
 * - security: 安全相关操作
 */
@Service
@Log(tags = {"user"})
public class UserService {
    // 实现
}
```

## 🏭 生产环境实践

### 1. 监控和告警

```yaml
# 生产环境监控配置
atlas:
  log:
    performance:
      enabled: true
      slow-threshold: 2000
      alerts:
        enabled: true
        threshold-multiplier: 2.0
    
    # 错误统计
    error-tracking:
      enabled: true
      sample-rate: 0.1  # 10%采样避免影响性能
```

```java
// 自定义监控组件
@Component
public class LogMonitor {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void handleSlowMethod(SlowMethodEvent event) {
        meterRegistry.counter("atlas.log.slow.methods",
            "method", event.getMethodName(),
            "threshold", String.valueOf(event.getThreshold()))
            .increment();
    }
    
    @EventListener  
    public void handleLogError(LogErrorEvent event) {
        meterRegistry.counter("atlas.log.errors", 
            "type", event.getErrorType())
            .increment();
    }
}
```

### 2. 日志聚合和分析

```yaml
# ELK Stack 集成配置
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId}] %logger{36} - %msg%n"
  
atlas:
  log:
    trace-id:
      enabled: true
      header-name: "X-Trace-Id"
    
    # 结构化日志输出
    output:
      format: "json"  # JSON格式便于日志分析
      include-stack-trace: false  # 生产环境不包含堆栈
```

### 3. 容量规划

```yaml
# 生产环境容量配置
atlas:
  log:
    # 控制日志量
    enabled-tags: ["business", "error", "security"]  # 只记录关键日志
    
    # 限制序列化大小
    serialization:
      max-depth: 3
      max-string-length: 500
    
    # 性能保护
    performance:
      max-methods-per-second: 1000  # 限制处理频率
```

## 📊 监控和运维实践

### 1. 健康检查

```java
@Component
public class LogHealthIndicator implements HealthIndicator {
    
    private final LogAspect logAspect;
    
    @Override
    public Health health() {
        try {
            // 检查日志系统状态
            boolean isHealthy = checkLogSystemHealth();
            
            if (isHealthy) {
                return Health.up()
                    .withDetail("processedMethods", logAspect.getProcessedCount())
                    .withDetail("errorCount", logAspect.getErrorCount())
                    .build();
            } else {
                return Health.down()
                    .withDetail("reason", "Log system not responding")
                    .build();
            }
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
```

### 2. 指标收集

```java
@Component
public class LogMetrics {
    
    private final MeterRegistry meterRegistry;
    
    @PostConstruct
    public void initMetrics() {
        // 注册自定义指标
        Gauge.builder("atlas.log.active.methods")
            .description("当前活跃的日志方法数")
            .register(meterRegistry, this, LogMetrics::getActiveMethodCount);
    }
    
    public double getActiveMethodCount() {
        return LogContext.getActiveMethodCount();
    }
}
```

### 3. 故障恢复

```java
@Component
public class LogFailureHandler {
    
    @EventListener
    public void handleLogFailure(LogFailureEvent event) {
        // 记录失败信息
        logger.error("日志记录失败: {}", event.getReason(), event.getException());
        
        // 降级处理
        if (event.isCritical()) {
            // 切换到简单日志模式
            switchToSimpleLoggingMode();
        }
    }
    
    private void switchToSimpleLoggingMode() {
        // 实现降级逻辑
    }
}
```

## 🎯 总结

### 核心原则总结

1. **简洁优先** - 保持注解简洁，配置外部化
2. **性能考虑** - 避免大对象序列化，使用条件过滤
3. **安全第一** - 严格保护敏感数据，完善脱敏配置
4. **环境隔离** - 不同环境使用不同配置
5. **团队一致** - 统一规范，文档化实践
6. **生产就绪** - 监控告警，故障恢复

### 快速检查清单

在添加 `@Log` 注解时，请检查：

- [ ] 是否包含敏感信息？
- [ ] 日志级别是否合适？
- [ ] 消息模板是否有意义？
- [ ] 是否会影响性能？
- [ ] 标签分类是否正确？
- [ ] 是否符合团队规范？

遵循这些最佳实践，您将能够充分发挥 Atlas Log 的威力，同时保持代码的整洁和系统的高性能。

---

**恭喜！您已经掌握了 Atlas Log 的最佳实践！🏆**