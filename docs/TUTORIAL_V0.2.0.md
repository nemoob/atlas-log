# Atlas Log 0.2.0 版本教程

欢迎使用 Atlas Log 0.2.0！本教程将指导您使用最新版本的功能，特别是新修复的注解配置功能。

## 📖 目录

- [版本亮点](#版本亮点)
- [快速开始](#快速开始)
- [注解配置详解](#注解配置详解)
- [HTTP 日志配置](#http-日志配置)
- [格式化器配置](#格式化器配置)
- [故障排除](#故障排除)
- [最佳实践](#最佳实践)

## 🎯 版本亮点

### ✅ 修复的重要问题

1. **注解配置 urlFormat 不生效** - 现在完全支持注解级别的 HTTP 日志配置
2. **SensitiveDataMasker 序列化异常** - 优化了敏感数据处理，避免序列化问题
3. **注解级别格式化器不生效** - 修复了 `@Log` 注解中格式化器配置问题

### 🚀 新增功能

- 完整的 HTTP 日志注解配置支持
- 改进的配置优先级处理
- 增强的调试和故障排除功能

## 🚀 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.github.nemoob</groupId>
    <artifactId>atlas-log-spring-boot-starter</artifactId>
    <version>0.2.0</version>
</dependency>
```

### 2. 基础配置

```java
@SpringBootApplication
@EnableAtlasLog
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. 验证安装

```java
@RestController
public class TestController {
    
    @GetMapping("/test")
    @Log("测试接口")
    public String test() {
        return "Atlas Log 0.2.0 工作正常！";
    }
}
```

## 📝 注解配置详解

### 完整注解配置示例

```java
@SpringBootApplication
@EnableAtlasLog(
    enabled = true,
    defaultLevel = "INFO",
    spelEnabled = true,
    
    // HTTP 日志配置 - 0.2.0 新修复
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
        headerName = "X-Trace-Id",
        generator = "uuid"
    ),
    
    // 性能监控配置
    performance = @AtlasLogPerformance(
        enabled = true,
        slowThreshold = 1000L,
        logSlowMethods = true
    ),
    
    // 敏感数据配置
    sensitive = @AtlasLogSensitive(
        enabled = true,
        customFields = {"password", "bankCard", "idCard"}
    )
)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 🌐 HTTP 日志配置

### urlFormat 占位符支持

0.2.0 版本完全修复了 urlFormat 配置问题，现在支持以下占位符：

| 占位符 | 说明 | 示例值 |
|--------|------|--------|
| `{method}` | HTTP 方法 | `GET`, `POST`, `PUT` |
| `{uri}` | 请求 URI | `/api/users/123` |
| `{queryString}` | 查询字符串 | `?name=john&age=25` |
| `{remoteAddr}` | 客户端 IP | `192.168.1.100` |

### 配置示例

#### 1. 只显示查询参数
```java
@EnableAtlasLog(
    httpLog = @AtlasLogHttpLog(
        urlFormat = "{queryString}",
        includeQueryString = true
    )
)
```

**输出效果：**
```
# 请求: GET /api/users?id=123&name=john
# 日志: TraceId: xxx | URL: ?id=123&name=john | HTTP请求完成
```

#### 2. 包含客户端 IP
```java
@EnableAtlasLog(
    httpLog = @AtlasLogHttpLog(
        urlFormat = "[{remoteAddr}] {method} {uri}"
    )
)
```

**输出效果：**
```
# 日志: TraceId: xxx | URL: [192.168.1.100] GET /api/users | HTTP请求完成
```

#### 3. 完整格式
```java
@EnableAtlasLog(
    httpLog = @AtlasLogHttpLog(
        urlFormat = "{remoteAddr} -> {method} {uri}{queryString}",
        includeQueryString = true
    )
)
```

**输出效果：**
```
# 日志: TraceId: xxx | URL: 192.168.1.100 -> GET /api/users?id=123 | HTTP请求完成
```

### YAML 配置对比

您也可以使用 YAML 配置，但注解配置具有更高优先级：

```yaml
atlas:
  log:
    http-log:
      url-format: "{method} {uri}{queryString}"
      include-query-string: true
      log-full-parameters: true
```

## 🎨 格式化器配置

### 注解级别格式化器

0.2.0 版本修复了注解级别格式化器不生效的问题：

```java
@RestController
public class UserController {
    
    // 使用 key-value 格式化器
    @GetMapping("/users")
    @Log(
        value = "查询用户列表",
        logArgs = true,
        argumentFormatter = "key-value"
    )
    public List<User> getUsers(@RequestParam String name, 
                              @RequestParam Integer age) {
        // 输出: arg0=john&arg1=25
        return userService.findUsers(name, age);
    }
    
    // 使用 JSON 格式化器
    @PostMapping("/users")
    @Log(
        value = "创建用户",
        logArgs = true,
        logResult = true,
        argumentFormatter = "json",
        resultFormatter = "json"
    )
    public User createUser(@RequestBody User user) {
        // 参数输出: [{"name":"john","age":25}]
        // 返回值输出: {"id":1,"name":"john","age":25}
        return userService.createUser(user);
    }
    
    // 混合使用格式化器
    @PutMapping("/users/{id}")
    @Log(
        value = "更新用户",
        logArgs = true,
        logResult = true,
        argumentFormatter = "key-value",  // 参数用 key-value
        resultFormatter = "json"          // 返回值用 JSON
    )
    public User updateUser(@PathVariable Long id, @RequestBody User user) {
        return userService.updateUser(id, user);
    }
}
```

### 自定义格式化器

```java
@Component
public class CustomArgumentFormatter implements ArgumentFormatter {
    
    @Override
    public String getName() {
        return "custom";
    }
    
    @Override
    public String formatArguments(Object[] args, FormatterContext context) {
        return "[自定义格式] " + Arrays.toString(args);
    }
    
    @Override
    public String formatResult(Object result, FormatterContext context) {
        return "[返回值] " + result;
    }
}

// 使用自定义格式化器
@Log(
    value = "测试自定义格式化器",
    argumentFormatter = "custom",
    resultFormatter = "custom"
)
public String testCustomFormatter(String input) {
    return "processed: " + input;
}
```

## 🔍 故障排除

### 配置验证

添加配置检查器来验证配置是否生效：

```java
@Component
public class AtlasLogConfigChecker {
    
    @Autowired
    private LogConfigProperties properties;
    
    @PostConstruct
    public void checkConfiguration() {
        System.out.println("=== Atlas Log 0.2.0 Configuration ===");
        
        // 基础配置
        System.out.println("Enabled: " + properties.isEnabled());
        System.out.println("Default Level: " + properties.getDefaultLevel());
        
        // HTTP 日志配置
        LogConfigProperties.HttpLogConfig httpLog = properties.getHttpLog();
        System.out.println("URL Format: " + httpLog.getUrlFormat());
        System.out.println("Include Query String: " + httpLog.isIncludeQueryString());
        System.out.println("Log Full Parameters: " + httpLog.isLogFullParameters());
        
        // 链路追踪配置
        LogConfigProperties.TraceIdConfig trace = properties.getTraceId();
        System.out.println("Trace Enabled: " + trace.isEnabled());
        System.out.println("Trace Header: " + trace.getHeaderName());
        
        System.out.println("=====================================");
    }
}
```

### 调试日志

启用调试日志查看详细的配置解析过程：

```yaml
logging:
  level:
    io.github.nemoob.atlas.log.config: DEBUG
    io.github.nemoob.atlas.log.web.LoggingFilter: DEBUG
    io.github.nemoob.atlas.log.aspect.AtlasLogAspect: DEBUG
```

### 常见问题

#### 1. 注解配置不生效

**问题**: 配置了 `@AtlasLogHttpLog(urlFormat = "{queryString}")` 但仍显示默认格式

**解决方案**:
```java
// ✅ 正确的配置方式
@EnableAtlasLog(
    httpLog = @AtlasLogHttpLog(
        urlFormat = "{queryString}"
    )
)

// ❌ 错误的配置方式
@AtlasLogHttpLog(urlFormat = "{queryString}")  // 直接在类上使用不会生效
```

#### 2. 格式化器不生效

**问题**: `argumentFormatter = "key-value"` 配置后仍输出 JSON 格式

**解决方案**:
- 确保格式化器名称正确：`json`, `key-value`, `custom`
- 检查自定义格式化器是否正确注册为 Spring Bean
- 启用调试日志查看格式化器选择过程

#### 3. YAML 配置覆盖注解配置

**问题**: YAML 配置覆盖了注解配置

**解决方案**:
- 检查 `application.yml` 中是否有冲突的配置
- 注解配置应该具有更高优先级，如果被覆盖请检查配置合并逻辑

## 💡 最佳实践

### 1. 配置策略

```java
// 推荐：使用注解配置进行类型安全的配置
@EnableAtlasLog(
    // 基础配置
    enabled = true,
    defaultLevel = "INFO",
    
    // 生产环境建议的 HTTP 日志配置
    httpLog = @AtlasLogHttpLog(
        urlFormat = "{method} {uri}",  // 生产环境不建议记录查询参数
        includeQueryString = false,
        logFullParameters = false,
        includeHeaders = false
    ),
    
    // 开发环境可以更详细
    // httpLog = @AtlasLogHttpLog(
    //     urlFormat = "{remoteAddr} -> {method} {uri}{queryString}",
    //     includeQueryString = true,
    //     logFullParameters = true
    // )
)
```

### 2. 格式化器选择

```java
// 推荐：根据场景选择合适的格式化器
public class BestPracticeController {
    
    // API 接口：使用 JSON 格式便于解析
    @PostMapping("/api/users")
    @Log(
        value = "创建用户 API",
        argumentFormatter = "json",
        resultFormatter = "json"
    )
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return ResponseEntity.ok(userService.create(user));
    }
    
    // 内部方法：使用 key-value 格式便于阅读
    @Log(
        value = "内部用户查询",
        argumentFormatter = "key-value"
    )
    public List<User> findUsers(String name, Integer age, String department) {
        return userService.findUsers(name, age, department);
    }
    
    // 复杂对象：使用自定义格式化器
    @Log(
        value = "复杂业务处理",
        argumentFormatter = "custom",
        resultFormatter = "custom"
    )
    public BusinessResult processComplexBusiness(ComplexRequest request) {
        return businessService.process(request);
    }
}
```

### 3. 性能优化

```java
// 推荐：合理配置性能监控
@EnableAtlasLog(
    performance = @AtlasLogPerformance(
        enabled = true,
        slowThreshold = 1000L,      // 1秒阈值
        logSlowMethods = true
    ),
    
    // 敏感数据处理已优化，可以安全启用
    sensitive = @AtlasLogSensitive(
        enabled = true,
        customFields = {"password", "token", "secret"}
    )
)
```

### 4. 环境配置

```java
// 推荐：使用 Profile 进行环境区分
@Profile("dev")
@Configuration
public class DevLogConfig {
    // 开发环境详细配置
}

@Profile("prod")
@Configuration
public class ProdLogConfig {
    // 生产环境精简配置
}
```

## 🎉 总结

Atlas Log 0.2.0 版本修复了重要的配置问题，现在您可以：

- ✅ 使用注解配置 HTTP 日志格式，包括 urlFormat 占位符
- ✅ 使用注解级别的格式化器配置
- ✅ 享受更稳定的敏感数据处理
- ✅ 获得更好的调试和故障排除体验

立即升级到 0.2.0 版本，体验更强大、更稳定的日志功能！

---

## 📚 相关文档

- [变更日志](CHANGELOG.md)
- [配置指南](CONFIGURATION.md)
- [基础使用](BASIC_USAGE.md)
- [故障排除](TROUBLESHOOTING.md)
- [最佳实践](BEST_PRACTICES.md)

如有问题，请参考 [故障排除文档](TROUBLESHOOTING.md) 或提交 Issue。