# 🔧 故障排除指南

本指南帮助您快速诊断和解决使用 Atlas Log 时遇到的常见问题。

## 📖 目录

- [快速诊断](#快速诊断)
- [注解不生效问题](#注解不生效问题)
- [SpEL表达式问题](#spel表达式问题)
- [性能问题](#性能问题)
- [配置问题](#配置问题)
- [序列化问题](#序列化问题)
- [集成问题](#集成问题)
- [调试技巧](#调试技巧)

## 🩺 快速诊断

### 检查清单

在深入排查问题前，请先检查以下基础项：

- [ ] 已添加 `atlas-log-spring-boot-starter` 依赖
- [ ] 已添加 `@EnableAtlasLog` 注解
- [ ] 目标方法在 Spring Bean 中
- [ ] 目标方法是 public 的
- [ ] 不是同类内部方法调用
- [ ] `atlas.log.enabled=true`

### 快速诊断命令

```bash
# 检查依赖是否正确
mvn dependency:tree | grep atlas-log

# 检查Spring Boot自动配置
java -jar your-app.jar --debug | grep LogAutoConfiguration

# 检查Bean是否创建
curl http://localhost:8080/actuator/beans | grep -i log
```

## ❌ 注解不生效问题

### 问题1：@Log 注解完全无效

**症状：** 添加了 `@Log` 注解但完全没有日志输出

**可能原因和解决方案：**

#### 1.1 未启用 Atlas Log
```java
// ❌ 错误：忘记添加启用注解
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

// ✅ 正确：添加启用注解
@SpringBootApplication
@EnableAtlasLog  // 必须添加此注解
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

#### 1.2 缺少 Spring AOP 依赖
```xml
<!-- 确保包含 AOP 依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

#### 1.3 配置被禁用
```yaml
# 检查配置文件
atlas:
  log:
    enabled: true  # 必须为 true
```

### 问题2：同类内部调用失效

**症状：** 外部调用有日志，内部调用无日志

```java
@Service
public class UserService {
    
    @Log("外部调用")
    public void externalCall() {
        internalCall();  // ❌ 这里不会触发日志
    }
    
    @Log("内部调用")  
    public void internalCall() {
        // 业务逻辑
    }
}
```

**解决方案：**

```java
@Service
public class UserService {
    
    @Autowired
    private UserService self;  // 注入自己
    
    @Log("外部调用")
    public void externalCall() {
        self.internalCall();  // ✅ 通过代理调用
    }
    
    @Log("内部调用")
    public void internalCall() {
        // 业务逻辑
    }
}
```

或者使用 `AopContext.currentProxy()`：

```java
@Service
@EnableAspectJAutoProxy(exposeProxy = true)
public class UserService {
    
    @Log("外部调用")
    public void externalCall() {
        ((UserService) AopContext.currentProxy()).internalCall();
    }
    
    @Log("内部调用")
    public void internalCall() {
        // 业务逻辑
    }
}
```

### 问题3：private/protected 方法无效

```java
@Service
public class UserService {
    
    @Log("私有方法")
    private void privateMethod() {  // ❌ AOP无法拦截私有方法
        // 业务逻辑
    }
    
    @Log("公有方法")
    public void publicMethod() {    // ✅ 正确
        // 业务逻辑
    }
}
```

### 问题4：final 方法无效

```java
@Service
public class UserService {
    
    @Log("final方法")
    public final void finalMethod() {  // ❌ final方法无法被代理
        // 业务逻辑
    }
    
    @Log("普通方法")
    public void normalMethod() {       // ✅ 正确
        // 业务逻辑
    }
}
```

## 🔍 SpEL表达式问题

### 问题1：表达式语法错误

**症状：** 应用启动时抛出表达式解析异常

```java
// ❌ 错误的表达式语法
@Log("用户ID: {args[0]}")  // 错误：应该用 #{}
@Log("用户ID: #{args}")    // 错误：缺少索引
@Log("用户ID: #{arg[0]}")  // 错误：应该是 args

// ✅ 正确的表达式语法
@Log("用户ID: #{args[0]}")
@Log("用户名: #{args[0].name}")
@Log("结果: #{result.success}")
```

### 问题2：变量未定义

```java
// ❌ 在进入消息中使用 result（此时还没有结果）
@Log(
    value = "处理数据",
    enterMessage = "开始处理，预期结果: #{result}"  // 错误
)

// ✅ 在合适的位置使用变量
@Log(
    value = "处理数据", 
    enterMessage = "开始处理，参数: #{args[0]}",
    exitMessage = "处理完成，结果: #{result}"       // 正确
)
```

### 问题3：Bean方法调用失败

```java
// ❌ Bean不存在或方法不存在
@Log(
    value = "VIP用户操作",
    condition = "#{@nonExistentService.isVip()}"  // Bean不存在
)

// ❌ 方法签名错误
@Log(
    value = "权限检查",
    condition = "#{@securityService.hasPermission()}"  // 缺少参数
)

// ✅ 正确的Bean方法调用
@Log(
    value = "VIP用户操作", 
    condition = "#{@userService.isVip(args[0])}"       // 正确
)
```

**调试SpEL表达式：**

```yaml
# 启用详细日志查看表达式执行过程
logging:
  level:
    io.github.nemoob.atlas.log.expression: DEBUG
```

### 问题4：性能问题 - 表达式过于复杂

```java
// ❌ 复杂的表达式影响性能
@Log(
    value = "复杂计算",
    condition = "#{@service1.method1(args[0]) and @service2.method2(args[1]) and @service3.method3()}"
)

// ✅ 简化表达式或使用专门的方法
@Component
public class LogConditions {
    public boolean shouldLogComplexOperation(Object arg1, Object arg2) {
        return service1.method1(arg1) && 
               service2.method2(arg2) && 
               service3.method3();
    }
}

@Log(
    value = "复杂计算",
    condition = "#{@logConditions.shouldLogComplexOperation(args[0], args[1])}"
)
```

## ⚡ 性能问题

### 问题1：日志记录影响应用性能

**症状：** 应用响应变慢，CPU使用率上升

**诊断步骤：**

1. **检查是否记录了大对象**
```java
// ❌ 记录大对象影响性能
@Log("处理大数据")
public void processBigData(List<LargeObject> data) {  // 可能包含大量数据
    // 处理逻辑
}

// ✅ 限制记录内容
@Log(
    value = "处理大数据：#{args[0].size()}条",
    logArgs = false,              // 不记录参数
    maxArgLength = 100           // 或限制长度
)
public void processBigData(List<LargeObject> data) {
    // 处理逻辑
}
```

2. **检查SpEL表达式性能**
```java
// ❌ 每次都执行复杂计算
@Log(
    value = "用户操作",
    condition = "#{T(java.time.LocalDateTime).now().getHour() > 9}"  // 每次都计算时间
)

// ✅ 使用缓存或简化逻辑
@Log(
    value = "用户操作",
    condition = "#{@timeService.isBusinessHour()}"  // 使用缓存的结果
)
```

3. **启用性能监控**
```yaml
atlas:
  log:
    performance:
      enabled: true
      slow-threshold: 100  # 降低阈值查找性能问题
```

### 问题2：内存占用过高

**可能原因：**
- SpEL表达式缓存过大
- 序列化对象过大
- 日志输出缓冲区过大

**解决方案：**

```yaml
atlas:
  log:
    # 限制序列化长度
    serialization:
      max-depth: 5
      ignore-null-fields: true
    
    # 限制参数和返回值长度
    default-max-arg-length: 500
    default-max-result-length: 500
    
    # 使用条件过滤
    enabled-tags: ["important", "error"]  # 只记录重要日志
```

## ⚙️ 配置问题

### 问题1：配置不生效

**症状：** 修改了配置但行为没有改变

**检查步骤：**

1. **配置优先级问题**
```java
// 注解配置优先级最高，会覆盖YAML配置
@EnableAtlasLog({
    @AtlasLogSensitive(enabled = false)  // 这会覆盖YAML中的设置
})
```

2. **配置路径错误**
```yaml
# ❌ 错误的配置路径
atlas:
  logs:  # 错误：应该是 log 不是 logs
    enabled: true

# ✅ 正确的配置路径  
atlas:
  log:
    enabled: true
```

3. **环境变量格式错误**
```bash
# ❌ 错误格式
export ATLAS.LOG.ENABLED=true

# ✅ 正确格式
export ATLAS_LOG_ENABLED=true
```

### 问题2：开发和生产环境配置冲突

```yaml
# application.yml - 基础配置
atlas:
  log:
    enabled: true

# application-dev.yml - 开发环境
atlas:
  log:
    default-level: DEBUG
    sensitive:
      enabled: false  # 开发环境不脱敏

# application-prod.yml - 生产环境  
atlas:
  log:
    default-level: INFO
    sensitive:
      enabled: true   # 生产环境必须脱敏
```

## 🔄 序列化问题

### 问题1：循环引用导致序列化失败

**症状：** StackOverflowError 或 序列化异常

```java
// 问题代码：User和Order相互引用
public class User {
    private List<Order> orders;  // User引用Order
}

public class Order {
    private User user;  // Order引用User
}

@Log("查询用户")
public User getUser(Long id) {
    return userService.getUser(id);  // 序列化时可能循环引用
}
```

**解决方案：**

1. **限制序列化深度**
```yaml
atlas:
  log:
    serialization:
      max-depth: 3  # 限制序列化深度
```

2. **使用@JsonIgnore**
```java
public class Order {
    @JsonIgnore  // 忽略此字段的序列化
    private User user;
}
```

3. **排除有问题的参数**
```java
@Log(
    value = "查询用户",
    logResult = false  // 不序列化返回值
)
public User getUser(Long id) {
    return userService.getUser(id);
}
```

### 问题2：敏感数据脱敏不生效

**可能原因：**

1. **字段名不在脱敏列表中**
```yaml
atlas:
  log:
    sensitive:
      custom-fields:
        - "userPassword"  # 确保包含您的字段名
```

2. **字段名大小写问题**
```java
// 脱敏器不区分大小写，但要确保配置正确
public class User {
    private String Password;     // 会被脱敏
    private String password;     // 会被脱敏  
    private String userPwd;      // 需要在配置中添加 "userPwd"
}
```

3. **嵌套对象的敏感字段**
```java
public class UserRequest {
    private String username;
    private LoginInfo loginInfo;  // 嵌套对象
}

public class LoginInfo {
    private String password;  // 嵌套对象中的敏感字段也会被脱敏
}
```

## 🔗 集成问题

### 问题1：与其他AOP框架冲突

**症状：** NoSuchMethodError 或代理失效

**解决方案：**

1. **检查AOP依赖版本**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
    <!-- 确保版本兼容 -->
</dependency>
```

2. **调整AOP顺序**
```java
@Aspect
@Order(100)  // 调整执行顺序
public class LogAspect {
    // ...
}
```

### 问题2：与安全框架集成问题

```java
// 确保在安全检查之后记录日志
@PreAuthorize("hasRole('ADMIN')")
@Log("管理员操作")  // 确保注解顺序正确
public void adminOperation() {
    // 管理员操作
}
```

### 问题3：与事务管理冲突

```java
// 确保日志记录不影响事务
@Transactional
@Log(
    value = "数据库操作",
    // 确保异常处理不干扰事务回滚
    exceptionHandlers = {
        @ExceptionHandler(
            exception = DataAccessException.class,
            rethrow = true  // 重新抛出异常以确保事务回滚
        )
    }
)
public void databaseOperation() {
    // 数据库操作
}
```

## 🔍 调试技巧

### 启用详细日志

```yaml
logging:
  level:
    io.github.nemoob.atlas.log: DEBUG              # Atlas Log框架日志
    io.github.nemoob.atlas.log.aspect: TRACE       # 切面执行详情
    io.github.nemoob.atlas.log.expression: DEBUG   # SpEL表达式执行
    io.github.nemoob.atlas.log.config: DEBUG       # 配置加载过程
```

### 使用Spring Boot Actuator

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    configprops:
      show-values: ALWAYS
```

检查配置：
```bash
curl http://localhost:8080/actuator/configprops | grep atlas
```

检查Bean：
```bash
curl http://localhost:8080/actuator/beans | grep -i log
```

### 自定义调试接口

```java
@RestController
@RequestMapping("/debug")
public class DebugController {
    
    @Autowired
    private LogConfigProperties logConfig;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @GetMapping("/log-config")
    public Object getLogConfig() {
        return logConfig;
    }
    
    @GetMapping("/log-beans")
    public Map<String, Object> getLogBeans() {
        return applicationContext.getBeansOfType(Object.class)
            .entrySet().stream()
            .filter(entry -> entry.getKey().toLowerCase().contains("log"))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().getClass().getName()
            ));
    }
    
    @Log("测试日志功能")
    @GetMapping("/test-log")
    public String testLog(@RequestParam String message) {
        return "测试完成: " + message;
    }
}
```

### 性能监控

```java
@Component
public class LogPerformanceMonitor {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void handleSlowMethod(SlowMethodEvent event) {
        meterRegistry.counter("atlas.log.slow.methods", 
            "method", event.getMethodName(),
            "class", event.getClassName())
            .increment();
    }
}
```

## 📞 获取帮助

如果以上解决方案都无法解决您的问题：

1. **检查版本兼容性**
   - Spring Boot 版本
   - Java 版本  
   - Atlas Log 版本

2. **提供详细信息**
   - 完整的错误堆栈
   - 相关配置文件
   - 最小复现示例

3. **联系支持**
   - [GitHub Issues](https://github.com/nemoob/atlas-log/issues)
   - [GitHub Discussions](https://github.com/nemoob/atlas-log/discussions)

## 🎯 总结

大多数问题都源于以下几个常见原因：

1. **基础配置缺失** - 忘记 `@EnableAtlasLog` 或缺少AOP依赖
2. **AOP限制** - 私有方法、同类调用、final方法
3. **SpEL语法错误** - 表达式语法或变量使用错误
4. **性能考量不足** - 大对象序列化、复杂表达式
5. **配置优先级混淆** - 不了解注解>YAML>环境变量的优先级

遵循最佳实践并仔细阅读错误信息，大部分问题都能快速解决。

---

**希望这个故障排除指南能帮助您快速解决问题！🔧**