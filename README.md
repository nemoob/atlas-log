# Atlas Log - Spring Boot 注解驱动的日志框架

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.nemoob/atlas-log-spring-boot-starter.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.nemoob/atlas-log-spring-boot-starter)

Atlas Log 是一个为 Spring Boot 应用设计的轻量级、高性能的日志框架。通过简单的 `@Log` 注解，即可实现方法调用的自动日志记录，支持参数序列化、返回值记录、执行时间统计、异常处理等功能。

## 核心特性

- 🎯 **注解驱动** - 使用 `@Log` 注解即可开启日志记录
- 🔍 **SpEL 表达式** - 支持动态日志消息和条件判断  
- 🛡️ **敏感数据脱敏** - 自动识别并脱敏敏感信息
- 🚀 **高性能** - 基于 Spring AOP，性能开销极小
- 🔧 **灵活配置** - 支持 YAML 配置和注解配置两种方式
- 📊 **链路追踪** - 自动生成和传递 TraceId
- 🎛️ **条件日志** - 基于 SpEL 表达式的条件日志记录

## 快速开始

### 第一步：添加依赖

```xml
<dependency>
    <groupId>io.github.nemoob</groupId>
    <artifactId>atlas-log-spring-boot-starter</artifactId>
    <version>0.0.1</version>
</dependency>
```

### 第二步：启用 Atlas Log

```java
@SpringBootApplication
@EnableAtlasLog
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 第三步：使用 @Log 注解

```java
@Service
public class UserService {
    
    @Log("查询用户信息: 用户ID=#{args[0]}")
    public User getUserById(Long userId) {
        return userRepository.findById(userId);
    }
    
    @Log(
        value = "用户登录: 用户名=#{args[0]}",
        tags = {"security", "login"},
        logArgs = true,
        excludeArgs = {1} // 排除密码参数
    )
    public boolean login(String username, String password) {
        return authService.authenticate(username, password);
    }
}
```

## 使用方式对比

### 1. 基础注解方式（推荐）

```java
// 最简单的使用方式
@Log("获取用户信息")
public User getUser(Long id) { }

// 带参数的动态消息
@Log("查询用户: #{args[0]}")
public User getUserById(Long id) { }
```

### 2. 完整配置方式

```java
@Log(
    value = "VIP用户查询: #{args[0]}",
    level = LogLevel.INFO,
    tags = {"vip", "query"},
    condition = "#{@userService.isVipUser(args[0])}",
    logArgs = true,
    logResult = true,
    logExecutionTime = true
)
public User getVipUser(Long id) { }
```

### 3. 注解配置方式（企业级推荐）

```java
@SpringBootApplication
@EnableAtlasLog({
    @AtlasLogTrace(enabled = true, headerName = "X-Trace-Id"),
    @AtlasLogSensitive(enabled = true, customFields = {"bankCard", "idCard"}),
    @AtlasLogPerformance(enabled = true, slowThreshold = 1000)
})
public class Application { }
```

### 4. YAML 配置方式

```yaml
atlas:
  log:
    enabled: true
    default-level: INFO
    trace-id:
      enabled: true
      header-name: "X-Trace-Id"
    sensitive:
      enabled: true
      custom-fields: ["bankCard", "idCard"]
    performance:
      enabled: true
      slow-threshold: 1000
```

## 💡 最推荐的使用方式：基础注解 + YAML配置

这种方式结合了注解的简洁性和配置的灵活性：

```java
// 在代码中使用简洁的注解
@Log("#{methodName}: #{args[0]}")
public User getUserById(Long id) { }

@Log(
    value = "重要操作: #{args[0]}",
    tags = {"important"},
    condition = "#{args[0] != null}"
)
public void importantOperation(String data) { }
```

```yaml
# 在 application.yml 中进行全局配置
atlas:
  log:
    enabled: true
    default-level: INFO
    enabled-tags: ["important", "security", "api"]
    sensitive:
      enabled: true
      custom-fields: ["password", "token", "secret"]
    trace-id:
      enabled: true
```

**为什么推荐这种方式？**
- ✅ 代码简洁，注解不会过于冗长
- ✅ 配置集中，便于统一管理
- ✅ 环境隔离，可针对不同环境配置不同参数
- ✅ 团队协作友好，减少代码冲突

## 核心功能展示

### SpEL 表达式支持

```java
@Log(
    value = "方法 #{className}.#{methodName} 执行",
    enterMessage = "开始执行，参数: #{args}",
    exitMessage = "执行完成，耗时: #{executionTime}ms，结果: #{result}",
    exceptionMessage = "执行失败: #{exception.message}"
)
public String complexMethod(String input) {
    return "result";
}
```

### 敏感数据脱敏

```java
// 原始数据
{"password": "123456", "bankCard": "6222600260001234567"}

// 自动脱敏后
{"password": "****", "bankCard": "622260**********567"}
```

### 条件日志记录

```java
@Log(
    value = "管理员操作: #{args[0]}",
    condition = "#{@securityService.isAdmin()}",
    tags = {"admin", "audit"}
)
public void adminOperation(String operation) { }
```

## 文档导航

- 📚 [开发者指南](docs/DEVELOPER_GUIDE.md) - 详细的使用教程和最佳实践
- 🏗️ [系统架构](docs/ARCHITECTURE.md) - 架构设计和核心组件说明
- ⚙️ [配置参考](docs/CONFIGURATION.md) - 完整的配置选项说明
- 🔧 [扩展指南](docs/EXTENSION_GUIDE.md) - 自定义序列化器等扩展功能
- ❓ [故障排除](docs/TROUBLESHOOTING.md) - 常见问题和解决方案