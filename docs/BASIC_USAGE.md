# 📝 基础使用指南

本指南将详细介绍 `@Log` 注解的各种用法，帮助您掌握 Atlas Log 的核心功能。

## 📖 目录

- [@Log 注解基础语法](#log-注解基础语法)
- [日志消息定制](#日志消息定制)
- [参数控制](#参数控制)
- [返回值控制](#返回值控制)
- [日志级别和标签](#日志级别和标签)
- [异常处理](#异常处理)
- [类级别注解](#类级别注解)
- [多注解组合](#多注解组合)

## 🎯 @Log 注解基础语法

### 完整注解参数说明

```java
@Log(
    value = "日志消息模板",           // 主要日志消息
    level = LogLevel.INFO,          // 日志级别
    tags = {"tag1", "tag2"},        // 标签数组
    condition = "#{SpEL表达式}",     // 条件表达式
    logArgs = true,                 // 是否记录参数
    logResult = true,               // 是否记录返回值
    logExecutionTime = true,        // 是否记录执行时间
    excludeArgs = {0, 1},          // 排除的参数索引
    maxArgLength = 1000,           // 参数最大长度
    maxResultLength = 1000,        // 返回值最大长度
    enterMessage = "开始执行消息",    // 方法进入消息
    exitMessage = "执行完成消息",     // 方法退出消息
    exceptionMessage = "异常消息"    // 异常时的消息
)
```

### 最简单的用法

```java
@Service
public class UserService {
    
    // 最基础的用法
    @Log("获取用户信息")
    public User getUser(Long id) {
        return userRepository.findById(id);
    }
    
    // 无参数，只记录方法执行
    @Log("清理缓存")
    public void clearCache() {
        cache.clear();
    }
}
```

**输出示例：**
```
2024-08-24 10:30:15.123 INFO --- [UserService.getUser] 获取用户信息 开始执行
参数: [123]
2024-08-24 10:30:15.128 INFO --- [UserService.getUser] 获取用户信息 执行完成
返回值: {"id":123,"name":"张三"}
执行时间: 5ms
```

## 💬 日志消息定制

### 静态消息

```java
@Log("用户登录验证")
public boolean validateUser(String username, String password) {
    return authService.validate(username, password);
}
```

### 动态消息（使用SpEL表达式）

```java
@Log("查询用户: ID=#{args[0]}")
public User getUserById(Long id) {
    return userRepository.findById(id);
}

@Log("创建用户: 姓名=#{args[0].name}, 邮箱=#{args[0].email}")
public User createUser(User user) {
    return userRepository.save(user);
}

@Log("批量处理: 共#{args[0].size()}条数据")
public void batchProcess(List<String> data) {
    // 批量处理逻辑
}
```

### 自定义进入和退出消息

```java
@Log(
    value = "用户注册流程",
    enterMessage = "开始注册用户: #{args[0]}",
    exitMessage = "注册完成，用户ID: #{result.id}，耗时: #{executionTime}ms",
    exceptionMessage = "注册失败: #{exception.message}"
)
public User registerUser(String username) {
    return userService.register(username);
}
```

**输出示例：**
```
2024-08-24 10:30:15.123 INFO --- [UserService.registerUser] 开始注册用户: johnsmith
2024-08-24 10:30:15.156 INFO --- [UserService.registerUser] 注册完成，用户ID: 456，耗时: 33ms
```

## 📥 参数控制

### 控制参数记录

```java
public class OrderService {
    
    // 记录所有参数（默认行为）
    @Log(value = "创建订单", logArgs = true)
    public Order createOrder(Long userId, BigDecimal amount, String description) {
        return orderRepository.save(new Order(userId, amount, description));
    }
    
    // 不记录任何参数
    @Log(value = "敏感操作", logArgs = false)
    public void sensitiveOperation(String secret, String token) {
        // 敏感操作
    }
    
    // 排除特定参数（排除密码）
    @Log(
        value = "用户登录",
        excludeArgs = {1}  // 排除第2个参数（密码）
    )
    public boolean login(String username, String password) {
        return authService.authenticate(username, password);
    }
    
    // 排除多个参数
    @Log(
        value = "更新用户密码", 
        excludeArgs = {1, 2}  // 排除旧密码和新密码
    )
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        userService.updatePassword(userId, oldPassword, newPassword);
    }
}
```

### 使用 @LogIgnore 注解

```java
@Log("用户注册")
public User register(
    String username, 
    @LogIgnore String password,  // 使用注解忽略此参数
    String email
) {
    return userService.register(username, password, email);
}
```

### 参数长度限制

```java
@Log(
    value = "处理大量数据",
    maxArgLength = 200  // 限制参数序列化长度为200字符
)
public void processLargeData(List<ComplexObject> data) {
    // 处理大量数据
}
```

**输出示例：**
```
参数: [{"items":[{"id":1,"name":"item1"},{"id":2,"name":"item2"}...]...]（已截断，总长度: 1543）
```

## 📤 返回值控制

### 控制返回值记录

```java
public class DataService {
    
    // 记录返回值（默认行为）
    @Log(value = "查询数据", logResult = true)
    public List<Data> queryData(String condition) {
        return dataRepository.findByCondition(condition);
    }
    
    // 不记录返回值（如敏感数据）
    @Log(value = "获取用户令牌", logResult = false)
    public String getUserToken(Long userId) {
        return tokenService.generateToken(userId);
    }
    
    // 限制返回值长度
    @Log(
        value = "导出大量数据",
        maxResultLength = 500
    )
    public String exportData() {
        return dataExporter.exportToJson();
    }
}
```

### 动态返回值在消息中的使用

```java
@Log(
    value = "订单处理完成",
    exitMessage = "订单#{result.id}处理完成，状态: #{result.status}"
)
public Order processOrder(OrderRequest request) {
    return orderService.process(request);
}
```

## 🏷️ 日志级别和标签

### 日志级别设置

```java
public class SecurityService {
    
    @Log(value = "普通操作", level = LogLevel.INFO)
    public void normalOperation() { }
    
    @Log(value = "警告操作", level = LogLevel.WARN)
    public void warningOperation() { }
    
    @Log(value = "错误处理", level = LogLevel.ERROR)
    public void errorHandling() { }
    
    @Log(value = "调试信息", level = LogLevel.DEBUG)
    public void debugOperation() { }
}
```

### 标签分类

```java
public class ApiController {
    
    @Log(
        value = "用户API调用",
        tags = {"api", "user", "query"}  // 多个标签
    )
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }
    
    @Log(
        value = "支付API调用",
        tags = {"api", "payment", "business"}
    )
    @PostMapping("/payments")
    public PaymentResult payment(@RequestBody PaymentRequest request) {
        return paymentService.process(request);
    }
    
    @Log(
        value = "管理员操作",
        tags = {"api", "admin", "security"}
    )
    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }
}
```

## ⚠️ 异常处理

### 基础异常记录

```java
@Log("数据处理")
public void processData(String data) {
    if (data == null) {
        throw new IllegalArgumentException("数据不能为空");
    }
    // 处理逻辑
}
```

**异常时的输出：**
```
2024-08-24 10:30:15.123 INFO --- [DataService.processData] 数据处理 开始执行
2024-08-24 10:30:15.125 ERROR --- [DataService.processData] 数据处理 执行异常
异常: java.lang.IllegalArgumentException: 数据不能为空
执行时间: 2ms
```

### 自定义异常消息

```java
@Log(
    value = "用户认证",
    exceptionMessage = "认证失败，用户: #{args[0]}，原因: #{exception.message}"
)
public void authenticate(String username, String password) {
    if (!authService.validate(username, password)) {
        throw new AuthenticationException("用户名或密码错误");
    }
}
```

### 特定异常处理

```java
@Log(
    value = "支付处理",
    exceptionHandlers = {
        @ExceptionHandler(
            exception = PaymentException.class,
            level = LogLevel.ERROR,
            message = "支付失败: 订单=#{args[0]}, 错误=#{exception.code}"
        ),
        @ExceptionHandler(
            exception = NetworkException.class,
            level = LogLevel.WARN,
            message = "网络异常，稍后重试: #{exception.message}"
        )
    }
)
public PaymentResult processPayment(String orderId) {
    return paymentService.process(orderId);
}
```

## 🏛️ 类级别注解

### 类级别默认配置

```java
@Log(
    level = LogLevel.INFO,
    tags = {"user-service"},
    logArgs = true,
    logResult = true
)
@Service
public class UserService {
    
    // 继承类级别配置
    public User getUser(Long id) {
        return userRepository.findById(id);
    }
    
    // 方法级别覆盖类级别配置
    @Log(
        value = "删除用户", 
        level = LogLevel.WARN,
        tags = {"user-service", "delete", "admin"}
    )
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
```

## 🔄 多注解组合

### 使用 @Logs 注解

```java
@Logs({
    @Log(
        value = "业务操作",
        tags = {"business"},
        condition = "#{@configService.isBusinessLogEnabled()}"
    ),
    @Log(
        value = "审计日志",
        level = LogLevel.WARN,
        tags = {"audit"},
        logArgs = false,
        condition = "#{@securityService.needAudit()}"
    )
})
public void importantBusinessOperation(String data) {
    // 重要业务操作
}
```

### 不同场景的多重日志

```java
@Logs({
    @Log(
        value = "API调用统计",
        tags = {"api", "stats"},
        logResult = false
    ),
    @Log(
        value = "安全审计",
        level = LogLevel.WARN,
        tags = {"security", "audit"},
        logArgs = false,
        condition = "#{@securityService.isHighRiskOperation()}"
    )
})
@PostMapping("/sensitive-operation")
public Result sensitiveOperation(@RequestBody SensitiveData data) {
    return businessService.process(data);
}
```

## 📋 常用模式总结

### Web API 模式

```java
@RestController
@RequestMapping("/api/users")
@Log(tags = {"api", "user"})  // 类级别标签
public class UserController {
    
    @Log("查询用户列表")
    @GetMapping
    public List<User> getUsers(@RequestParam String keyword) {
        return userService.search(keyword);
    }
    
    @Log(
        value = "获取用户详情: #{args[0]}",
        tags = {"query"}
    )
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }
    
    @Log(
        value = "创建用户: #{args[0].username}",
        tags = {"create"},
        excludeArgs = {0}  // 不记录完整用户对象
    )
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.create(user);
    }
}
```

### 服务层模式

```java
@Service
@Log(tags = {"service", "user"})
public class UserService {
    
    @Log("#{methodName}: #{args[0]}")  // 使用方法名
    public User getUser(Long id) {
        return userRepository.findById(id);
    }
    
    @Log(
        value = "用户注册",
        condition = "#{@configService.isRegistrationLogEnabled()}"
    )
    public User register(UserRegistrationDto dto) {
        return userRepository.save(dto.toEntity());
    }
}
```

### 安全敏感操作模式

```java
@Service
public class SecurityService {
    
    @Log(
        value = "敏感操作执行",
        level = LogLevel.WARN,
        tags = {"security", "sensitive"},
        logArgs = false,    // 不记录敏感参数
        logResult = false   // 不记录敏感返回值
    )
    public String generateSecretToken(String userId, String secretKey) {
        return tokenGenerator.generate(userId, secretKey);
    }
}
```

## 🎯 下一步

现在您已经掌握了 `@Log` 注解的基础用法，可以继续学习：

- 🔍 [SpEL 表达式指南](SPEL_GUIDE.md) - 学习强大的动态表达式
- ⚙️ [配置参考](CONFIGURATION.md) - 了解完整配置选项
- 🛡️ [敏感数据保护](SENSITIVE_DATA.md) - 深入了解数据安全特性
- 🎯 [条件日志](CONDITIONAL_LOGGING.md) - 智能条件日志记录

---

**恭喜！您已经掌握了 Atlas Log 的基础用法！🎉**