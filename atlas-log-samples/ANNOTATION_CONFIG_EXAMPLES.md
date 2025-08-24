# Atlas Log 注解配置示例

本示例演示了如何使用 Atlas Log 的注解配置功能，提供了一种声明式的配置方式来替代传统的 YAML 配置。

## 示例应用

### 1. 完整注解配置示例 (`AnnotationConfigSampleApplication`)

展示了如何使用 `@EnableAtlasLog` 注解进行完整的配置：

```java
@SpringBootApplication
@EnableAtlasLog(
    enabled = true,
    defaultLevel = "DEBUG",
    enabledTags = {"business", "security", "api", "vip", "admin", "audit", "urgent"},
    enabledGroups = {"default", "business", "security", "urgent"},
    trace = @AtlasLogTrace(headerName = "X-Request-Trace-Id"),
    performance = @AtlasLogPerformance(slowThreshold = 500L),
    sensitive = @AtlasLogSensitive(customFields = {"password", "bankCard", "phone"})
)
public class AnnotationConfigSampleApplication {
    // ...
}
```

### 2. 简化注解配置示例 (`SimpleAnnotationConfigSampleApplication`)

展示了如何使用最少配置启用 Atlas Log：

```java
@SpringBootApplication
@EnableAtlasLog  // 使用默认配置
public class SimpleAnnotationConfigSampleApplication {
    // ...
}
```

## 配置对比

### 传统 YAML 配置方式

```yaml
atlas:
  log:
    enabled: true
    default-level: INFO
    date-format: "yyyy-MM-dd HH:mm:ss.SSS"
    pretty-print: false
    max-message-length: 2000
    spel-enabled: true
    condition-enabled: true
    
    enabled-tags:
      - "business"
      - "security" 
      - "api"
      - "vip"
      - "admin"
      - "audit"
      - "urgent"
    
    enabled-groups:
      - "default"
      - "business"
      - "security"
      - "urgent"
    
    exclusions:
      - "*.toString"
      - "*.hashCode"
      - "*.equals"
      - "*.getClass"
    
    trace-id:
      enabled: true
      header-name: "X-Trace-Id"
      generator: "uuid"
    
    performance:
      enabled: true
      slow-threshold: 1000
      log-slow-methods: true
    
    condition:
      cache-enabled: true
      timeout-ms: 1000
      fail-safe: true
    
    sensitive:
      enabled: true
      custom-fields:
        - "bankCard"
        - "idCard"
        - "socialSecurityNumber"
      mask-value: "***"
```

### 注解配置方式

```java
@EnableAtlasLog(
    enabled = true,
    defaultLevel = "INFO",
    dateFormat = "yyyy-MM-dd HH:mm:ss.SSS",
    prettyPrint = false,
    maxMessageLength = 2000,
    spelEnabled = true,
    conditionEnabled = true,
    enabledTags = {"business", "security", "api", "vip", "admin", "audit", "urgent"},
    enabledGroups = {"default", "business", "security", "urgent"},
    exclusions = {"*.toString", "*.hashCode", "*.equals", "*.getClass"},
    trace = @AtlasLogTrace(
        enabled = true,
        headerName = "X-Trace-Id",
        generator = "uuid"
    ),
    performance = @AtlasLogPerformance(
        enabled = true,
        slowThreshold = 1000L,
        logSlowMethods = true
    ),
    condition = @AtlasLogCondition(
        cacheEnabled = true,
        timeoutMs = 1000L,
        failSafe = true
    ),
    sensitive = @AtlasLogSensitive(
        enabled = true,
        customFields = {"bankCard", "idCard", "socialSecurityNumber"},
        maskValue = "***"
    )
)
```

## 配置优先级

当同时存在注解配置和 YAML 配置时，配置优先级如下：

**注解配置 > application.yml > 环境变量 > 默认值**

这意味着注解配置会覆盖 YAML 配置中的相同配置项。

## 运行示例

### 1. 运行完整配置示例

```bash
# 进入 samples 目录
cd atlas-log-samples

# 运行完整配置示例
mvn spring-boot:run -Dspring-boot.run.mainClass=com.atlas.log.samples.AnnotationConfigSampleApplication
```

### 2. 运行简化配置示例

```bash
# 运行简化配置示例
mvn spring-boot:run -Dspring-boot.run.mainClass=com.atlas.log.samples.SimpleAnnotationConfigSampleApplication
```

## 注解配置优势

1. **类型安全**：编译时检查配置的有效性
2. **IDE 支持**：自动补全和语法高亮
3. **集中配置**：配置和代码在同一位置，便于维护
4. **默认值**：合理的默认配置，减少配置工作量
5. **验证机制**：内置配置验证，及早发现配置错误
6. **冲突检测**：自动检测并报告配置冲突

## 注意事项

1. 注解配置和 YAML 配置可以共存，注解配置优先级更高
2. 使用注解配置时，建议删除或注释掉相应的 YAML 配置以避免混淆
3. 复杂的配置建议使用 YAML 方式，简单配置推荐使用注解方式
4. 注解配置在编译时确定，无法在运行时动态修改

## 相关文档

- [Atlas Log 快速开始](../README.md)
- [配置参考](../docs/configuration.md)
- [API 文档](../docs/api.md)