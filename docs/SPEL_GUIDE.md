# 🔍 SpEL 表达式指南

Spring Expression Language (SpEL) 是 Atlas Log 的核心特性之一，让您能够创建动态、智能的日志消息。本指南将详细介绍如何在日志注解中使用 SpEL 表达式。

## 📖 目录

- [SpEL 基础语法](#spel-基础语法)
- [内置变量](#内置变量)
- [常用表达式示例](#常用表达式示例)
- [条件表达式](#条件表达式)
- [调用Spring Bean](#调用spring-bean)
- [类型和方法调用](#类型和方法调用)
- [高级用法](#高级用法)
- [性能和注意事项](#性能和注意事项)

## 🎯 SpEL 基础语法

### 表达式格式

在 Atlas Log 中，SpEL 表达式需要用 `#{}` 包围：

```java
@Log("用户ID: #{args[0]}")              // ✅ 正确
@Log("用户ID: {args[0]}")               // ❌ 错误：缺少 #
@Log("用户ID: ${args[0]}")              // ❌ 错误：这是属性占位符语法
```

### 基本数据类型

```java
// 字符串
@Log("消息: #{'Hello World'}")

// 数字  
@Log("数值: #{123}")
@Log("浮点: #{3.14}")

// 布尔值
@Log("状态: #{true}")

// null值
@Log("空值: #{null}")
```

## 📋 内置变量

Atlas Log 提供了丰富的内置变量，可在不同阶段的日志消息中使用：

### 1. 方法参数 (args)

```java
@Log("单个参数: #{args[0]}")
public void singleParam(String name) { }

@Log("多个参数: 用户=#{args[0]}, 年龄=#{args[1]}")
public void multipleParams(String name, int age) { }

@Log("参数数量: #{args.length}")
public void paramCount(String a, String b, String c) { }

@Log("所有参数: #{args}")
public void allParams(String name, int age) { }
```

**访问对象属性：**
```java
@Log("用户信息: ID=#{args[0].id}, 姓名=#{args[0].name}")
public void processUser(User user) { }

@Log("订单详情: 订单号=#{args[0].orderNo}, 金额=#{args[0].amount}")
public void processOrder(Order order) { }
```

### 2. 返回值 (result)

⚠️ **注意：** `result` 只能在 `exitMessage` 中使用

```java
@Log(
    value = "数据处理",
    exitMessage = "处理完成，结果: #{result}"
)
public ProcessResult processData(String data) { }

@Log(
    value = "用户查询", 
    exitMessage = "查询完成，用户ID: #{result.id}, 姓名: #{result.name}"
)
public User getUser(Long id) { }

@Log(
    value = "计算操作",
    exitMessage = "计算结果: #{result}, 是否成功: #{result > 0}"
)
public int calculate(int a, int b) { }
```

### 3. 异常信息 (exception)

⚠️ **注意：** `exception` 只能在 `exceptionMessage` 中使用

```java
@Log(
    value = "数据库操作",
    exceptionMessage = "操作失败: #{exception.message}"
)
public void dbOperation() { }

@Log(
    value = "文件处理",
    exceptionMessage = "文件处理失败，错误类型: #{exception.class.simpleName}, 详情: #{exception.message}"
)
public void processFile(String filename) { }
```

### 4. 方法元信息

```java
@Log("执行方法: #{methodName}")
public void someMethod() { }

@Log("类名: #{className}")  
public void anotherMethod() { }

@Log("完整方法: #{className}.#{methodName}")
public void fullMethodName() { }

@Log(
    value = "方法执行",
    exitMessage = "#{methodName} 执行完成，耗时: #{executionTime}ms"
)
public void timedMethod() { }
```

## 💡 常用表达式示例

### 1. 字符串操作

```java
@Log("用户名长度: #{args[0].length()}")
public void checkUsername(String username) { }

@Log("大写用户名: #{args[0].toUpperCase()}")
public void processUsername(String username) { }

@Log("用户名是否包含admin: #{args[0].contains('admin')}")
public void validateUsername(String username) { }

@Log("截取用户名: #{args[0].substring(0, 3)}")
public void truncateUsername(String username) { }
```

### 2. 数字运算

```java
@Log("计算结果: #{args[0] + args[1]}")
public int add(int a, int b) { }

@Log("是否为正数: #{args[0] > 0}")
public void checkPositive(int number) { }

@Log("数值范围检查: #{args[0] >= 1 && args[0] <= 100}")
public void checkRange(int value) { }
```

### 3. 集合操作

```java
@Log("列表大小: #{args[0].size()}")
public void processList(List<String> items) { }

@Log("列表为空: #{args[0].isEmpty()}")
public void checkEmpty(List<String> items) { }

@Log("第一个元素: #{args[0].get(0)}")
public void getFirstElement(List<String> items) { }

@Log("列表内容: #{args[0]}")
public void showList(List<String> items) { }
```

### 4. Map 操作

```java
@Log("Map大小: #{args[0].size()}")
public void processMap(Map<String, Object> data) { }

@Log("包含key: #{args[0].containsKey('userId')}")
public void checkKey(Map<String, Object> data) { }

@Log("用户ID: #{args[0]['userId']}")
public void getUserId(Map<String, Object> data) { }
```

## ⚖️ 条件表达式

### 1. 简单条件

```java
// 基于参数值的条件
@Log(
    value = "大额转账",
    condition = "#{args[1] > 10000}"  // 金额大于1万才记录
)
public void transfer(String account, BigDecimal amount) { }

// 基于参数属性的条件
@Log(
    value = "VIP用户操作",
    condition = "#{args[0].vipLevel > 0}"
)
public void vipOperation(User user) { }

// 基于字符串的条件
@Log(
    value = "管理员操作", 
    condition = "#{args[0].equals('admin')}"
)
public void adminOperation(String role) { }
```

### 2. 复杂条件

```java
// 多条件组合
@Log(
    value = "重要业务操作",
    condition = "#{args[0] > 1000 && args[1].contains('important')}"
)
public void importantOperation(int priority, String tag) { }

// 使用逻辑运算符
@Log(
    value = "特殊情况处理",
    condition = "#{args[0] == null || args[0].isEmpty()}"
)
public void handleSpecialCase(String input) { }

// 三元运算符
@Log("用户类型: #{args[0].age >= 18 ? '成人' : '未成年'}")
public void checkUserType(User user) { }
```

### 3. 基于环境的条件

```java
@Log(
    value = "开发环境日志",
    condition = "#{environment.getProperty('spring.profiles.active') == 'dev'}"
)
public void devOnlyLog() { }

@Log(
    value = "生产环境审计",
    condition = "#{environment.getProperty('spring.profiles.active') == 'prod'}"
)
public void prodAuditLog() { }
```

## 🏗️ 调用Spring Bean

### 1. 基本Bean调用

```java
@Component
public class UserHelper {
    public boolean isVip(Long userId) {
        return userService.getUser(userId).isVip();
    }
    
    public String getUserType(Long userId) {
        return userService.getUser(userId).getType();
    }
}

// 在日志中使用
@Log(
    value = "用户操作",
    condition = "#{@userHelper.isVip(args[0])}"
)
public void userOperation(Long userId) { }

@Log("#{@userHelper.getUserType(args[0])}用户登录")
public void userLogin(Long userId) { }
```

### 2. 服务层Bean调用

```java
@Service
public class SecurityService {
    public boolean hasPermission(Long userId, String resource) {
        return permissionService.check(userId, resource);
    }
}

@Log(
    value = "资源访问",
    condition = "#{@securityService.hasPermission(args[0], 'sensitive-data')}"
)
public void accessSensitiveData(Long userId) { }
```

### 3. 配置Bean调用

```java
@Component
public class LogConfig {
    
    @Value("${app.log.detailed:false}")
    private boolean detailedLog;
    
    public boolean isDetailedLogEnabled() {
        return detailedLog;
    }
    
    public boolean shouldLogForUser(Long userId) {
        // 复杂的用户日志策略
        return userId % 10 == 0;  // 示例：只记录10%用户的日志
    }
}

@Log(
    value = "详细操作日志",
    condition = "#{@logConfig.isDetailedLogEnabled()}"
)
public void detailedOperation() { }

@Log(
    value = "采样日志",
    condition = "#{@logConfig.shouldLogForUser(args[0])}"
)
public void sampledOperation(Long userId) { }
```

## 🔧 类型和方法调用

### 1. 静态方法调用

```java
// 调用 Java 标准库静态方法
@Log("当前时间: #{T(java.time.LocalDateTime).now()}")
public void timestampOperation() { }

@Log("随机数: #{T(java.lang.Math).random()}")
public void randomOperation() { }

@Log("UUID: #{T(java.util.UUID).randomUUID().toString()}")
public void uuidOperation() { }

// 调用自定义工具类
public class LogUtils {
    public static String formatUserId(Long userId) {
        return "USER_" + String.format("%08d", userId);
    }
}

@Log("格式化用户ID: #{T(com.example.LogUtils).formatUserId(args[0])}")
public void formatOperation(Long userId) { }
```

### 2. 类型转换

```java
@Log("字符串转数字: #{T(Integer).parseInt(args[0])}")
public void parseNumber(String numberStr) { }

@Log("转为大写: #{args[0].toString().toUpperCase()}")
public void upperCase(Object obj) { }
```

## 🚀 高级用法

### 1. Elvis 运算符（空安全）

```java
// 空安全访问
@Log("用户名: #{args[0]?.name ?: '匿名用户'}")
public void processUser(User user) { }

@Log("邮箱: #{args[0]?.profile?.email ?: '未设置'}")
public void checkEmail(User user) { }
```

### 2. 集合投影和选择

```java
// 集合投影（提取属性）
@Log("用户ID列表: #{args[0].![id]}")
public void processUsers(List<User> users) { }

// 集合选择（过滤）
@Log("VIP用户: #{args[0].?[vip == true]}")
public void processVipUsers(List<User> users) { }

// 组合使用
@Log("VIP用户名列表: #{args[0].?[vip == true].![name]}")
public void getVipUserNames(List<User> users) { }
```

### 3. 正则表达式

```java
@Log("邮箱格式正确: #{args[0].matches('^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$')}")
public void validateEmail(String email) { }

@Log("手机号格式: #{args[0].matches('^1[3-9]\\d{9}$')}")
public void validatePhone(String phone) { }
```

### 4. 复杂对象处理

```java
@Log("订单摘要: 订单=#{args[0].orderNo}, 商品数=#{args[0].items.size()}, 总金额=#{args[0].items.![price * quantity].sum()}")
public void processOrder(Order order) { }
```

## ⚡ 性能和注意事项

### 1. 性能优化技巧

```java
// ✅ 推荐：简单表达式
@Log("用户ID: #{args[0]}")
public void simpleExpression(Long userId) { }

// ⚠️ 注意：复杂表达式可能影响性能
@Log("复杂计算: #{T(java.time.LocalDateTime).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyy-MM-dd HH:mm:ss'))}")
public void complexExpression() { }

// ✅ 推荐：将复杂逻辑移到Bean中
@Component
public class DateHelper {
    public String currentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}

@Log("时间戳: #{@dateHelper.currentTimestamp()}")
public void optimizedExpression() { }
```

### 2. 错误处理

```yaml
# 启用安全模式，表达式错误时不影响业务
atlas:
  log:
    exception:
      fail-safe: true  # 表达式异常时使用默认值
```

```java
// 防御性编程
@Log("安全访问: #{args[0]?.name ?: 'unknown'}")  // 使用Elvis运算符
public void safeAccess(User user) { }
```

### 3. 调试技巧

```yaml
# 启用SpEL调试日志
logging:
  level:
    io.github.nemoob.atlas.log.expression: DEBUG
```

```java
// 在开发环境可以使用复杂表达式进行调试
@Log("调试信息: args=#{args}, method=#{methodName}, time=#{T(System).currentTimeMillis()}")
@Profile("dev")
public void debugMethod(String input) { }
```

## 📋 SpEL 表达式速查表

### 基础语法
| 功能 | 语法 | 示例 |
|------|------|------|
| 访问属性 | `#{object.property}` | `#{args[0].name}` |
| 访问数组/列表 | `#{array[index]}` | `#{args[0]}` |
| 方法调用 | `#{object.method()}` | `#{args[0].toString()}` |
| 静态方法 | `#{T(Class).method()}` | `#{T(Math).max(1,2)}` |
| Bean引用 | `#{@beanName.method()}` | `#{@userService.isVip()}` |

### 运算符
| 类型 | 运算符 | 示例 |
|------|---------|------|
| 算术 | `+`, `-`, `*`, `/`, `%` | `#{args[0] + args[1]}` |
| 关系 | `>`, `<`, `>=`, `<=`, `==`, `!=` | `#{args[0] > 100}` |
| 逻辑 | `&&`, `\|\|`, `!` | `#{args[0] > 0 && args[1] != null}` |
| 三元 | `condition ? true : false` | `#{args[0] > 0 ? 'positive' : 'negative'}` |
| Elvis | `expr ?: default` | `#{args[0]?.name ?: 'unknown'}` |

### 集合操作
| 功能 | 语法 | 示例 |
|------|------|------|
| 投影 | `collection.![expression]` | `#{users.![name]}` |
| 选择 | `collection.?[condition]` | `#{users.?[age > 18]}` |
| 第一个匹配 | `collection.^[condition]` | `#{users.^[vip == true]}` |
| 最后一个匹配 | `collection.$[condition]` | `#{users.$[active == true]}` |

## 🎯 实战示例

### 1. API接口日志

```java
@RestController
public class UserController {
    
    @Log("API调用: #{request.requestURI}, 用户=#{args[0]}")
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id, HttpServletRequest request) { }
    
    @Log(
        value = "创建用户API", 
        condition = "#{@configService.isAuditEnabled()}",
        exitMessage = "用户创建成功: ID=#{result.id}, 用户名=#{result.username}"
    )
    @PostMapping("/users")
    public User createUser(@RequestBody User user) { }
}
```

### 2. 业务服务日志

```java
@Service
public class OrderService {
    
    @Log("订单处理: 订单=#{args[0].orderNo}, 金额=#{args[0].totalAmount}, 用户=#{args[0].userId}")
    public void processOrder(Order order) { }
    
    @Log(
        value = "大额订单处理",
        condition = "#{args[0].totalAmount > 10000}",
        level = LogLevel.WARN,
        tags = {"business", "large-order"}
    )
    public void processLargeOrder(Order order) { }
}
```

### 3. 安全审计日志

```java
@Service
public class SecurityService {
    
    @Log(
        value = "权限检查: 用户=#{args[0]}, 资源=#{args[1]}, 结果=#{result}",
        level = LogLevel.WARN,
        tags = {"security", "permission"}
    )
    public boolean checkPermission(Long userId, String resource) { }
    
    @Log(
        value = "敏感操作",
        condition = "#{@securityService.isHighRiskUser(args[0])}",
        level = LogLevel.ERROR,
        tags = {"security", "high-risk"}
    )
    public void sensitiveOperation(Long userId) { }
}
```

现在您已经掌握了 SpEL 表达式的强大功能，可以创建动态、智能的日志系统！

---

**恭喜！您已经成为 SpEL 表达式专家！🔍**