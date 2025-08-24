# 🚀 快速开始

在这个5分钟的快速指南中，您将学会如何在 Spring Boot 项目中集成 Atlas Log 并开始使用。

## 📋 前置要求

- Java 8 或更高版本
- Spring Boot 2.0 或更高版本
- Maven 3.6 或更高版本

## 🎯 第一步：添加依赖

在您的 `pom.xml` 文件中添加 Atlas Log 依赖：

```xml
<dependencies>
    <!-- Atlas Log Spring Boot Starter -->
    <dependency>
        <groupId>io.github.nemoob</groupId>
        <artifactId>atlas-log-spring-boot-starter</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- Spring Boot AOP Starter (如果项目中没有的话) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>
</dependencies>
```

## ⚙️ 第二步：启用 Atlas Log

在您的 Spring Boot 主类上添加 `@EnableAtlasLog` 注解：

```java
@SpringBootApplication
@EnableAtlasLog  // 添加这行
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

## 📝 第三步：使用 @Log 注解

现在您可以在任何 Spring Bean 的方法上使用 `@Log` 注解了：

### 最简单的用法

```java
@Service
public class UserService {
    
    @Log("查询用户信息")
    public User getUserById(Long id) {
        // 您的业务逻辑
        return userRepository.findById(id);
    }
}
```

### 稍微复杂一点的用法

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Log(
        value = "API调用: 获取用户 #{args[0]}",
        tags = {"api", "user"},
        logArgs = true,
        logResult = true
    )
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }
    
    @Log(
        value = "创建新用户: #{args[0].name}",
        tags = {"api", "user", "create"},
        excludeArgs = {0} // 不记录用户对象（可能包含敏感信息）
    )
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }
}
```

## 🔧 第四步：基础配置（可选）

在 `application.yml` 中添加基础配置：

```yaml
atlas:
  log:
    enabled: true  # 启用日志记录（默认为 true）
    default-level: INFO  # 默认日志级别
    
    # 敏感数据脱敏配置
    sensitive:
      enabled: true  # 启用敏感数据脱敏
      custom-fields:
        - "password"
        - "token"
        - "secret"
    
    # 链路追踪配置
    trace-id:
      enabled: true  # 启用 TraceId
      header-name: "X-Trace-Id"  # HTTP 头名称
```

## 🧪 第五步：测试验证

启动您的应用并调用带有 `@Log` 注解的方法，您应该能在日志中看到类似以下的输出：

```
2024-08-24 10:30:15.123 INFO  [main] [12345678-1234-1234-1234-123456789abc] --- 
[UserService.getUserById] 查询用户信息 开始执行
参数: [1]

2024-08-24 10:30:15.128 INFO  [main] [12345678-1234-1234-1234-123456789abc] --- 
[UserService.getUserById] 查询用户信息 执行完成
返回值: {"id":1,"name":"张三","email":"zhang***@example.com"}
执行时间: 5ms
```

## 🎉 恭喜！

您已经成功集成了 Atlas Log！只用了几分钟，您的应用就具备了：

- ✅ 自动日志记录
- ✅ 参数和返回值序列化
- ✅ 执行时间统计
- ✅ 敏感数据自动脱敏
- ✅ 链路追踪支持

## 🔍 日志输出说明

让我们来解读一下日志的各个部分：

```
2024-08-24 10:30:15.123 INFO  [main] [12345678-1234-1234-1234-123456789abc] --- 
[UserService.getUserById] 查询用户信息 开始执行
参数: [1]
```

- `2024-08-24 10:30:15.123` - 时间戳
- `INFO` - 日志级别
- `[main]` - 线程名
- `[12345678-1234-1234-1234-123456789abc]` - TraceId（链路追踪ID）
- `[UserService.getUserById]` - 方法签名
- `查询用户信息 开始执行` - 您在注解中定义的日志消息
- `参数: [1]` - 方法参数（已序列化）

## 🚀 下一步

现在您已经成功集成了 Atlas Log，可以探索更多高级功能：

### 🔍 学习 SpEL 表达式
```java
@Log("处理用户 #{args[0]} 的订单，订单数量: #{args[1].size()}")
public void processUserOrders(Long userId, List<Order> orders) {
    // 业务逻辑
}
```

### 🛡️ 配置敏感数据保护
```java
@Log(
    value = "用户登录",
    logArgs = true,
    excludeArgs = {1}  // 排除密码参数
)
public boolean login(String username, String password) {
    // 登录逻辑
}
```

### 🎯 使用条件日志
```java
@Log(
    value = "VIP用户操作",
    condition = "#{@userService.isVipUser(args[0])}"  // 只为VIP用户记录日志
)
public void vipOperation(Long userId) {
    // VIP专属操作
}
```

### ⚙️ 企业级配置
```java
@SpringBootApplication
@EnableAtlasLog({
    @AtlasLogTrace(enabled = true, headerName = "X-Trace-Id"),
    @AtlasLogSensitive(enabled = true, customFields = {"bankCard", "idCard"}),
    @AtlasLogPerformance(enabled = true, slowThreshold = 1000)
})
public class Application {
    // ...
}
```

## 📚 继续学习

- 📖 [基础使用指南](BASIC_USAGE.md) - 详细了解 @Log 注解的各种用法
- ⚙️ [配置参考](CONFIGURATION.md) - 完整的配置选项说明
- 🔍 [SpEL 表达式指南](SPEL_GUIDE.md) - 学习强大的动态表达式
- 🛡️ [敏感数据保护](SENSITIVE_DATA.md) - 深入了解数据安全特性

## ❓ 遇到问题？

如果您在集成过程中遇到问题，请查看：

- 🔧 [故障排除指南](TROUBLESHOOTING.md)
- 💡 [最佳实践](BEST_PRACTICES.md)
- 🐛 [GitHub Issues](https://github.com/nemoob/atlas-log/issues)

---

**恭喜您完成了 Atlas Log 的快速集成！🎉 现在开始享受自动化日志记录带来的便利吧！**