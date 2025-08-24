# Atlas Log 开发者指南

欢迎使用 Atlas Log！本指南将帮助您快速上手并深入了解 Atlas Log 的各项功能。

## 📖 指南导航

### 基础篇
- [🚀 快速开始](QUICK_START.md) - 5分钟快速集成指南
- [📝 基础使用](BASIC_USAGE.md) - @Log 注解基础用法
- [⚙️ 配置指南](CONFIGURATION.md) - 完整配置参考

### 进阶篇  
- [🔍 SpEL 表达式](SPEL_GUIDE.md) - 动态表达式使用指南
- [🛡️ 敏感数据保护](SENSITIVE_DATA.md) - 数据脱敏和安全实践
- [📊 链路追踪](TRACE_GUIDE.md) - 分布式链路追踪配置

### 高级篇
- [🎯 条件日志](CONDITIONAL_LOGGING.md) - 智能条件日志记录
- [🔧 扩展开发](EXTENSION_GUIDE.md) - 自定义扩展开发
- [⚡ 性能优化](PERFORMANCE_GUIDE.md) - 性能优化最佳实践

### 实战篇
- [💼 企业实践](ENTERPRISE_GUIDE.md) - 企业级使用场景和配置
- [🔨 故障排除](TROUBLESHOOTING.md) - 常见问题和解决方案
- [📋 最佳实践](BEST_PRACTICES.md) - 开发和使用最佳实践

## 🎯 为什么选择 Atlas Log？

### 对比传统日志记录方式

| 特性 | 传统方式 | Atlas Log |
|------|----------|-----------|
| 代码侵入性 | 高，需要在每个方法中编写日志代码 | 低，只需添加注解 |
| 一致性 | 难以保证，依赖开发者自觉 | 高，统一的日志格式和内容 |
| 参数记录 | 手动编写，容易遗漏 | 自动序列化，支持过滤 |
| 敏感数据 | 容易泄露，需人工注意 | 自动脱敏，安全可靠 |
| 性能监控 | 需要额外代码统计 | 内置执行时间统计 |
| 链路追踪 | 需要手动传递TraceId | 自动生成和传递 |
| 维护成本 | 高，修改复杂 | 低，配置驱动 |

### 典型使用场景

#### 🌐 Web API 日志记录
```java
@RestController
public class UserController {
    
    @Log(
        value = "API调用: 获取用户信息",
        tags = {"api", "user"},
        logArgs = true,
        logResult = true
    )
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }
}
```

#### 💰 业务服务日志
```java
@Service
public class PaymentService {
    
    @Log(
        value = "支付处理: 订单=#{args[0].orderId}, 金额=#{args[0].amount}",
        tags = {"payment", "business"},
        condition = "#{args[0].amount > 1000}", // 只记录大额支付
        exceptionHandlers = {
            @ExceptionHandler(
                exception = PaymentException.class,
                level = LogLevel.ERROR
            )
        }
    )
    public PaymentResult processPayment(PaymentRequest request) {
        // 支付逻辑
    }
}
```

#### 🔐 安全审计日志
```java
@Service
public class SecurityService {
    
    @Log(
        value = "安全操作: 用户=#{@securityContext.currentUser}, 操作=#{args[0]}",
        tags = {"security", "audit"},
        level = LogLevel.WARN,
        logArgs = false, // 不记录敏感参数
        logResult = false
    )
    public void sensitiveOperation(String operation, String secretData) {
        // 敏感操作
    }
}
```

## 📊 功能特性总览

### 🎯 注解驱动
- 简单的 `@Log` 注解即可启用日志记录
- 支持类级别和方法级别配置
- 多注解组合使用

### 🔍 SpEL 表达式支持
- 动态日志消息生成
- 条件日志记录
- 访问方法参数、返回值和异常信息
- 调用 Spring Bean 方法

### 🛡️ 安全特性
- 自动敏感数据脱敏
- 参数排除和忽略
- 内容长度限制
- 安全的表达式执行环境

### ⚡ 高性能设计
- 基于 Spring AOP，开销极小
- SpEL 表达式缓存
- 条件过滤减少不必要处理
- 支持异步日志输出

### 🔧 灵活配置
- YAML 文件配置
- 注解配置
- 环境变量支持
- 热更新配置

### 📊 监控和追踪
- 自动 TraceId 生成和传递
- 方法执行时间统计
- 慢方法识别
- 性能监控指标

## 🚀 立即开始

选择适合您的学习路径：

### 👋 新手用户
1. 从 [快速开始](QUICK_START.md) 开始
2. 学习 [基础使用](BASIC_USAGE.md)
3. 查看 [配置指南](CONFIGURATION.md)

### 🔧 有经验的开发者
1. 快速浏览 [快速开始](QUICK_START.md)
2. 深入了解 [SpEL 表达式](SPEL_GUIDE.md)
3. 探索 [高级功能](EXTENSION_GUIDE.md)

### 🏢 企业用户
1. 了解 [企业实践](ENTERPRISE_GUIDE.md)
2. 学习 [安全配置](SENSITIVE_DATA.md)
3. 查看 [性能优化](PERFORMANCE_GUIDE.md)

## 💬 获取帮助

- 📖 查看完整文档
- 🐛 报告问题：[GitHub Issues](https://github.com/nemoob/atlas-log/issues)
- 💡 功能建议：[GitHub Discussions](https://github.com/nemoob/atlas-log/discussions)
- 📧 联系作者：[杨杨杨大侠](mailto:your-email@example.com)

让我们开始 Atlas Log 的学习之旅吧！🎉